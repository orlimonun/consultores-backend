package cr.una.consultores.service;

import cr.una.consultores.dto.ReporteAgenteDTO;
import cr.una.consultores.dto.SaludInstanciaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda el ULTIMO REPORTE CRUDO de cada agente y lo evalua al leerlo.
 *
 * Se guarda crudo a proposito: si se ajusta un umbral, el proximo
 * /instancias reevalua lo que ya hay sin esperar al siguiente reporte.
 * Con el veredicto ya congelado eso no seria posible.
 */
@Service
public class AgenteMetricasService {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final EvaluadorSaludService evaluador;
    private final Map<String, ReporteAgenteDTO> ultimos = new ConcurrentHashMap<>();
    private final Map<String, Instant> recibido = new ConcurrentHashMap<>();

    @Value("${agente.timeout-segundos:180}")
    private int timeoutSegundos;

    public AgenteMetricasService(EvaluadorSaludService evaluador) {
        this.evaluador = evaluador;
    }

    public void registrar(ReporteAgenteDTO r) {
        if (r == null || r.instanciaId == null || r.instanciaId.isBlank()) {
            throw new IllegalArgumentException("El reporte debe traer instanciaId");
        }
        if (r.ultimaLectura == null || r.ultimaLectura.isBlank()) {
            r.ultimaLectura = ISO.format(Instant.now());
        }
        if (r.tipo == null || r.tipo.isBlank()) r.tipo = "tradicional";
        ultimos.put(r.instanciaId, r);
        recibido.put(r.instanciaId, Instant.now());
    }

    /**
     * Evalua cada reporte con los umbrales vigentes y aplica el latido.
     *
     * Una instancia sin contacto NO desaparece: conserva sus ultimos
     * valores y se marca el silencio. Ocultar lo que no responde es lo
     * peor que puede hacer un monitor, porque el silencio suele ser la
     * senal mas importante.
     */
    public List<SaludInstanciaDTO> listar() {
        Instant ahora = Instant.now();
        List<SaludInstanciaDTO> salida = new ArrayList<>();

        for (Map.Entry<String, ReporteAgenteDTO> e : ultimos.entrySet()) {
            SaludInstanciaDTO d = evaluador.evaluar(e.getValue(), "agente");

            Instant visto = recibido.get(e.getKey());
            long seg = visto == null ? Long.MAX_VALUE
                    : Duration.between(visto, ahora).getSeconds();
            d.segundosSinContacto = (int) Math.min(seg, Integer.MAX_VALUE);

            if (seg > timeoutSegundos) {
                d.conectado = false;
                d.estado = "unknown";   // se conservan los valores, no el veredicto
                d.causasCriticas.add(0, "Sin contacto con el agente desde hace " +
                        (seg / 60) + " minutos. Los valores mostrados son los últimos conocidos.");
            }
            salida.add(d);
        }
        salida.sort(Comparator.comparing(x -> x.nombre == null ? "" : x.nombre));
        return salida;
    }

    public int cantidad() { return ultimos.size(); }

    public void olvidar(String instanciaId) {
        ultimos.remove(instanciaId);
        recibido.remove(instanciaId);
    }
}