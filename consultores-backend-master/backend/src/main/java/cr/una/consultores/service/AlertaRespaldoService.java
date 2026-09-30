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
 * Condiciones mapeadas al documento de requerimientos (sección 11):
 *   SIN_PROGRAMACION    → Estrategia activa sin programación definida
 *   INACTIVA            → Estrategia marcada como inactiva
 *   RESPALDO_VENCIDO    → Próxima ejecución ya pasó y no hay registro de ella
 *   EJECUCION_FALLIDA   → Fallo registrado en las últimas 24 horas
 *   NOARCHIVELOG        → Base de datos en modo NOARCHIVELOG
 *   SIN_RESPALDO_RECIENTE → Sin respaldo exitoso en 7 días
 *   SCRIPT_INCOMPLETO   → Script RMAN aún no generado
 *   SIN_ESPACIO         → Disco con >85% de uso reportado por el agente
 */
@Service
public class AlertaRespaldoService {

    /** Umbral de uso de disco a partir del cual se genera alerta. */
    private static final double DISCO_ALERTA_PCT   = 85.0;
    private static final double DISCO_CRITICO_PCT  = 95.0;

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
        LocalDateTime hace7dias  = ahora.minusDays(7);
        LocalDateTime hace24h    = ahora.minusHours(24);

        List<EstrategiaRespaldo> todas = estrategias.findAll();

        // Cargamos las ejecuciones fallidas recientes una sola vez (evita N+1)
        List<EjecucionRespaldo> fallidasRecientes = ejecuciones.findFallidasDesde(hace24h);

        for (EstrategiaRespaldo e : todas) {
            String bdNombre = e.getBaseDatos().getNombre();

            // ── 1. Estrategia inactiva ───────────────────────────────────────
            if ("INACTIVA".equals(e.getEstado())) {
                alertas.add(alerta("INACTIVA", "ADVERTENCIA", e,
                    "La estrategia '" + e.getNombre() + "' está inactiva y no será ejecutada."));
                continue; // no evaluar más condiciones para inactivas
            }

            // ── 2. Script no generado ────────────────────────────────────────
            if (e.getScriptRman() == null || e.getScriptRman().isBlank()) {
                alertas.add(alerta("SCRIPT_INCOMPLETO", "ADVERTENCIA", e,
                    "La estrategia '" + e.getNombre() + "' no tiene script RMAN generado. " +
                    "Genera el script antes de activar la estrategia."));
            }

            // ── 3. Estrategia activa sin programación ────────────────────────
            Optional<ProgramacionRespaldo> prog =
                programaciones.findFirstByEstrategiaIdAndActivaTrue(e.getId());

            if ("ACTIVA".equals(e.getEstado()) && prog.isEmpty()) {
                alertas.add(alerta("SIN_PROGRAMACION", "CRITICA", e,
                    "La estrategia '" + e.getNombre() + "' está activa pero no tiene " +
                    "programación definida. Los respaldos no se ejecutarán automáticamente."));
            }

            // ── 4. Respaldo programado que no se ejecutó (Fix 2) ─────────────
            // Condición: tiene programación activa, proxima_ejecucion ya pasó
            // y no hay ninguna ejecución registrada después de ese momento.
            if (prog.isPresent()) {
                ProgramacionRespaldo p = prog.get();
                LocalDateTime proxima = p.getProximaEjecucion();
                if (proxima != null && proxima.isBefore(ahora)) {
                    // La ventana tolerada es 30 minutos después de la hora programada
                    LocalDateTime limite = proxima.plusMinutes(
                        p.getVentanaMinutos() != null ? p.getVentanaMinutos() : 30);
                    if (ahora.isAfter(limite)) {
                        // Buscar si hay alguna ejecución que arrancó después de la hora programada
                        boolean ejecutado = ejecuciones
                            .tieneEjecucionExitosaDesde(e.getId(), proxima);
                        if (!ejecutado) {
                            alertas.add(alerta("RESPALDO_VENCIDO", "CRITICA", e,
                                "El respaldo estaba programado para " + proxima.toString().replace("T", " ").substring(0, 16) +
                                " y no se ejecutó. Verifique el estado del agente RMAN."));
                        }
                    }
                }
            }

            // ── 5. Base de datos en NOARCHIVELOG ─────────────────────────────
            String modo = e.getBaseDatos().getModoArchivado();
            if ("NOARCHIVELOG".equalsIgnoreCase(modo)) {
                alertas.add(alerta("NOARCHIVELOG", "ADVERTENCIA", e,
                    "La base '" + bdNombre + "' está en modo NOARCHIVELOG. " +
                    "No es posible la recuperación hasta un punto en el tiempo. " +
                    "Considere activar ARCHIVELOG en Oracle."));
            }

            // ── 6. Ejecuciones fallidas en las últimas 24 horas ──────────────
            fallidasRecientes.stream()
                .filter(ej -> ej.getEstrategia().getId().equals(e.getId()))
                .forEach(ej -> alertas.add(alerta("EJECUCION_FALLIDA", "CRITICA", e,
                    "Ejecución fallida el " +
                    ej.getInicio().toString().replace("T", " ").substring(0, 16) +
                    (ej.getMensajeError() != null ? ": " + ej.getMensajeError() : "."))));

            // ── 7. Sin respaldo exitoso en los últimos 7 días ─────────────────
            if ("ACTIVA".equals(e.getEstado())) {
                boolean tieneReciente = ejecuciones.tieneEjecucionExitosaDesde(e.getId(), hace7dias);
                if (!tieneReciente) {
                    alertas.add(alerta("SIN_RESPALDO_RECIENTE", "ADVERTENCIA", e,
                        "La estrategia '" + e.getNombre() + "' no tiene ningún respaldo exitoso " +
                        "en los últimos 7 días. Revisa la programación y el estado del agente."));
                }
            }

            // ── 8. Falta de espacio en disco (Fix 1) ─────────────────────────
            // Se basa en la última ejecución que reportó datos de disco.
            ejecuciones.findByEstrategiaIdOrderByInicioDesc(e.getId())
                .stream()
                .filter(ej -> ej.getDiscoUsoPct() != null)
                .findFirst()
                .ifPresent(ej -> {
                    double pct = ej.getDiscoUsoPct();
                    if (pct >= DISCO_CRITICO_PCT) {
                        alertas.add(alerta("SIN_ESPACIO", "CRITICA", e,
                            "Disco al " + pct + "% de uso en " + ej.getDiscoMountPoint() +
                            " (solo " + ej.getDiscoLibreMb() + " MB libres). " +
                            "El respaldo puede fallar por falta de espacio."));
                    } else if (pct >= DISCO_ALERTA_PCT) {
                        alertas.add(alerta("SIN_ESPACIO", "ADVERTENCIA", e,
                            "Disco al " + pct + "% de uso en " + ej.getDiscoMountPoint() +
                            " (" + ej.getDiscoLibreMb() + " MB libres). " +
                            "Considera liberar espacio antes del próximo respaldo."));
                    }
                });
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
