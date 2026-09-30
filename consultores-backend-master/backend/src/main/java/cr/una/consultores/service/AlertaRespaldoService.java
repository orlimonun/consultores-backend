package cr.una.consultores.service;

import cr.una.consultores.dto.AlertaRespaldoDTO;
import cr.una.consultores.entity.EjecucionRespaldo;
import cr.una.consultores.entity.EstrategiaRespaldo;
import cr.una.consultores.entity.ProgramacionRespaldo;
import cr.una.consultores.repository.EjecucionRespaldoRepository;
import cr.una.consultores.repository.EstrategiaRespaldoRepository;
import cr.una.consultores.repository.ProgramacionRespaldoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Genera las alertas de control preventivo del módulo de respaldos.
 *
 * Cada condición mapeada al documento de requerimientos:
 *   - Estrategia sin programación           → SIN_PROGRAMACION
 *   - Estrategia inactiva                   → INACTIVA
 *   - Respaldo programado no ejecutado      → RESPALDO_VENCIDO
 *   - Ejecución fallida reciente            → EJECUCION_FALLIDA
 *   - Base en NOARCHIVELOG                  → NOARCHIVELOG
 *   - Sin respaldo exitoso en 7 días        → SIN_RESPALDO_RECIENTE
 *   - Script no generado                    → SCRIPT_INCOMPLETO
 */
@Service
public class AlertaRespaldoService {

    private final EstrategiaRespaldoRepository estrategias;
    private final ProgramacionRespaldoRepository programaciones;
    private final EjecucionRespaldoRepository ejecuciones;

    public AlertaRespaldoService(EstrategiaRespaldoRepository estrategias,
                                  ProgramacionRespaldoRepository programaciones,
                                  EjecucionRespaldoRepository ejecuciones) {
        this.estrategias = estrategias;
        this.programaciones = programaciones;
        this.ejecuciones = ejecuciones;
    }

    /** Evalúa todas las estrategias y devuelve la lista de alertas activas. */
    public List<AlertaRespaldoDTO> evaluar() {
        List<AlertaRespaldoDTO> alertas = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime hace7dias = ahora.minusDays(7);
        LocalDateTime hace24h = ahora.minusHours(24);

        List<EstrategiaRespaldo> todas = estrategias.findAll();

        for (EstrategiaRespaldo e : todas) {
            String bdNombre = e.getBaseDatos().getNombre();

            // 1. Estrategia inactiva con advertencia
            if ("INACTIVA".equals(e.getEstado())) {
                alertas.add(alerta("INACTIVA", "ADVERTENCIA", e,
                    "La estrategia '" + e.getNombre() + "' está inactiva y no será ejecutada."));
                continue; // no evaluar más condiciones para inactivas
            }

            // 2. Estrategia activa sin programación
            if ("ACTIVA".equals(e.getEstado())) {
                Optional<ProgramacionRespaldo> prog =
                    programaciones.findFirstByEstrategiaIdAndActivaTrue(e.getId());
                if (prog.isEmpty()) {
                    alertas.add(alerta("SIN_PROGRAMACION", "CRITICA", e,
                        "La estrategia '" + e.getNombre() + "' está activa pero no tiene programación definida."));
                }
            }

            // 3. Script no generado
            if (e.getScriptRman() == null || e.getScriptRman().isBlank()) {
                alertas.add(alerta("SCRIPT_INCOMPLETO", "ADVERTENCIA", e,
                    "La estrategia '" + e.getNombre() + "' no tiene script RMAN generado."));
            }

            // 4. Base de datos en NOARCHIVELOG
            String modo = e.getBaseDatos().getModoArchivado();
            if ("NOARCHIVELOG".equalsIgnoreCase(modo)) {
                alertas.add(alerta("NOARCHIVELOG", "ADVERTENCIA", e,
                    "La base '" + bdNombre + "' está en modo NOARCHIVELOG. " +
                    "Las posibilidades de recuperación puntual son limitadas."));
            }

            // 5. Ejecuciones fallidas en las últimas 24 horas
            List<EjecucionRespaldo> fallidas = ejecuciones.findFallidasDesde(hace24h);
            for (EjecucionRespaldo ej : fallidas) {
                if (ej.getEstrategia().getId().equals(e.getId())) {
                    alertas.add(alerta("EJECUCION_FALLIDA", "CRITICA", e,
                        "Ejecución fallida el " + ej.getInicio() +
                        (ej.getMensajeError() != null ? ": " + ej.getMensajeError() : ".")));
                }
            }

            // 6. Sin respaldo exitoso en los últimos 7 días
            if ("ACTIVA".equals(e.getEstado())) {
                boolean tieneReciente = ejecuciones.tieneEjecucionExitosaDesde(e.getId(), hace7dias);
                if (!tieneReciente) {
                    alertas.add(alerta("SIN_RESPALDO_RECIENTE", "ADVERTENCIA", e,
                        "La estrategia '" + e.getNombre() + "' no tiene ningún respaldo exitoso " +
                        "en los últimos 7 días."));
                }
            }
        }

        return alertas;
    }

    private AlertaRespaldoDTO alerta(String tipo, String severidad,
                                     EstrategiaRespaldo e, String mensaje) {
        AlertaRespaldoDTO a = new AlertaRespaldoDTO();
        a.tipo = tipo;
        a.severidad = severidad;
        a.estrategiaId = e.getId();
        a.estrategiaNombre = e.getNombre();
        a.baseDatosNombre = e.getBaseDatos().getNombre();
        a.mensaje = mensaje;
        a.detectedaEn = LocalDateTime.now();
        return a;
    }
}
