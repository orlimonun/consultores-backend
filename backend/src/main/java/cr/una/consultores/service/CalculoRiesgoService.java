package cr.una.consultores.service;

import cr.una.consultores.dto.*;
import cr.una.consultores.entity.*;
import cr.una.consultores.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Corazon del proyecto: traduce las respuestas de una auditoria en
 * nivel de madurez, cumplimiento y exposicion al riesgo por C/I/D.
 *
 * MODELO DE MADUREZ (calculado desde respuestas Si/No/NA):
 *   Se toma el % de respuestas "Si" sobre las respondidas (excluye NA)
 *   y se mapea a la escala 0-5 de la norma:
 *     0%        -> 0    (no existe)
 *     1-20%     -> 1    (informal)
 *     21-40%    -> 2    (parcial)
 *     41-60%    -> 3    (documentado)
 *     61-80%    -> 4    (supervisado)
 *     81-100%   -> 5    (mejora continua)
 *
 * MODELO DE RIESGO (usa la madurez, NO el conteo de "No"):
 *   riesgoControl = ((5 - madurez) / 5) * peso
 *   Un control con madurez 5 -> riesgo 0; con madurez 0 -> riesgo = peso.
 *   Por cada eje del CID se promedia ponderado por peso, solo entre los
 *   controles que afectan ese eje, y se normaliza a escala 0-100.
 */
@Service
public class CalculoRiesgoService {

    private final ControlRepository controlRepository;
    private final RespuestaRepository respuestaRepository;
    private final PreguntaRepository preguntaRepository;

    public CalculoRiesgoService(ControlRepository controlRepository,
                                RespuestaRepository respuestaRepository,
                                PreguntaRepository preguntaRepository) {
        this.controlRepository = controlRepository;
        this.respuestaRepository = respuestaRepository;
        this.preguntaRepository = preguntaRepository;
    }

    public ResultadoAuditoriaDTO calcular(Integer auditoriaId) {
        List<Control> controles = controlRepository.findAll();
        List<Respuesta> respuestas = respuestaRepository.findByAuditoriaId(auditoriaId);

        // Indexar respuestas por preguntaId para acceso rapido
        Map<Integer, Respuesta> respPorPregunta = respuestas.stream()
                .collect(Collectors.toMap(r -> r.getPregunta().getId(), r -> r, (a, b) -> a));

        List<ResultadoControlDTO> resultados = new ArrayList<>();

        for (Control c : controles) {
            List<Pregunta> preguntas = preguntaRepository.findByControlId(c.getId());

            int totalRespondidas = 0;
            int siCount = 0;
            for (Pregunta p : preguntas) {
                Respuesta r = respPorPregunta.get(p.getId());
                if (r == null) continue;
                String val = r.getRespuesta();
                if ("NA".equalsIgnoreCase(val)) continue; // NA no cuenta
                totalRespondidas++;
                if ("SI".equalsIgnoreCase(val)) siCount++;
            }

            if (totalRespondidas == 0) continue; // control sin datos -> se omite

            double cumplimiento = (double) siCount / totalRespondidas * 100.0;
            int madurez = mapearMadurez(cumplimiento);
            double riesgoControl = ((5.0 - madurez) / 5.0) * c.getPeso();

            ResultadoControlDTO dto = new ResultadoControlDTO();
            dto.controlId = c.getId();
            dto.codigo = c.getCodigo();
            dto.nombre = c.getNombre();
            dto.dominio = c.getDominio().getNombre();
            dto.nivelMadurez = madurez;
            dto.cumplimiento = redondear(cumplimiento);
            dto.riesgoControl = redondear(riesgoControl);
            dto.afectaC = c.getAfectaC();
            dto.afectaI = c.getAfectaI();
            dto.afectaD = c.getAfectaD();
            resultados.add(dto);
        }

        ResultadoAuditoriaDTO out = new ResultadoAuditoriaDTO();
        out.auditoriaId = auditoriaId;
        out.controles = resultados;
        out.riesgoC = riesgoDimension(resultados, controles, 'C');
        out.riesgoI = riesgoDimension(resultados, controles, 'I');
        out.riesgoD = riesgoDimension(resultados, controles, 'D');
        out.indiceGeneralRiesgo = indiceGeneral(resultados, controles);
        out.madurezPromedioGeneral = resultados.isEmpty() ? 0.0 : redondear(
                resultados.stream().mapToInt(r -> r.nivelMadurez).average().orElse(0.0));
        out.dominios = cumplimientoPorDominio(resultados);
        out.menorMadurez = resultados.stream()
                .sorted(Comparator.comparingInt(r -> r.nivelMadurez))
                .limit(5).collect(Collectors.toList());
        out.mayorRiesgo = resultados.stream()
                .sorted((a, b) -> Double.compare(b.riesgoControl, a.riesgoControl))
                .limit(5).collect(Collectors.toList());
        return out;
    }

    private int mapearMadurez(double cumplimiento) {
        if (cumplimiento <= 0)   return 0;
        if (cumplimiento <= 20)  return 1;
        if (cumplimiento <= 40)  return 2;
        if (cumplimiento <= 60)  return 3;
        if (cumplimiento <= 80)  return 4;
        return 5;
    }

    private double riesgoDimension(List<ResultadoControlDTO> resultados,
                                   List<Control> controles, char eje) {
        Map<Integer, Control> ctrlPorId = controles.stream()
                .collect(Collectors.toMap(Control::getId, c -> c));
        double sumaPesos = 0, sumaPonderada = 0;
        for (ResultadoControlDTO r : resultados) {
            boolean afecta = switch (eje) {
                case 'C' -> r.afectaC;
                case 'I' -> r.afectaI;
                case 'D' -> r.afectaD;
                default -> false;
            };
            if (!afecta) continue;
            Control c = ctrlPorId.get(r.controlId);
            double peso = c.getPeso();
            // riesgo normalizado 0-100 = (1 - madurez/5) * 100, ponderado por peso
            double riesgoNorm = (1.0 - r.nivelMadurez / 5.0) * 100.0;
            sumaPonderada += riesgoNorm * peso;
            sumaPesos += peso;
        }
        return sumaPesos == 0 ? 0.0 : redondear(sumaPonderada / sumaPesos);
    }

    private double indiceGeneral(List<ResultadoControlDTO> resultados, List<Control> controles) {
        Map<Integer, Control> ctrlPorId = controles.stream()
                .collect(Collectors.toMap(Control::getId, c -> c));
        double sumaPesos = 0, sumaPonderada = 0;
        for (ResultadoControlDTO r : resultados) {
            double peso = ctrlPorId.get(r.controlId).getPeso();
            double riesgoNorm = (1.0 - r.nivelMadurez / 5.0) * 100.0;
            sumaPonderada += riesgoNorm * peso;
            sumaPesos += peso;
        }
        return sumaPesos == 0 ? 0.0 : redondear(sumaPonderada / sumaPesos);
    }

    private List<ResultadoDominioDTO> cumplimientoPorDominio(List<ResultadoControlDTO> resultados) {
        Map<String, List<ResultadoControlDTO>> porDominio = resultados.stream()
                .collect(Collectors.groupingBy(r -> r.dominio));
        List<ResultadoDominioDTO> out = new ArrayList<>();
        for (Map.Entry<String, List<ResultadoControlDTO>> e : porDominio.entrySet()) {
            ResultadoDominioDTO d = new ResultadoDominioDTO();
            d.dominio = e.getKey();
            d.cumplimiento = redondear(e.getValue().stream()
                    .mapToDouble(r -> r.cumplimiento).average().orElse(0.0));
            d.madurezPromedio = redondear(e.getValue().stream()
                    .mapToInt(r -> r.nivelMadurez).average().orElse(0.0));
            out.add(d);
        }
        return out;
    }

    private double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
