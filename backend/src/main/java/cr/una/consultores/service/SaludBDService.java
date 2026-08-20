package cr.una.consultores.service;

import cr.una.consultores.dto.*;
import cr.una.consultores.entity.LecturaMetricas;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula el Indice de Salud de la Base de Datos (ISBD) de Oracle.
 *
 * Modelo (segun el enunciado y el frontend):
 *   - Tres componentes: Procesos (IP), Memoria (IM), Archivos (IA), cada uno 0-100.
 *   - ISBD = 0.30*IP + 0.35*IM + 0.35*IA
 *   - Cada metrica tiene umbrales que definen su estado: normal/warning/degraded/critical.
 *   - El indicador de un componente se calcula a partir del estado de sus metricas.
 *   - REGLA CLAVE: si algun componente esta en critico, el estado global es critico
 *     aunque el promedio ponderado sea alto (un buen promedio no oculta un problema critico).
 */
@Service
public class SaludBDService {

    // Pesos del ISBD
    private static final double WP = 0.30, WM = 0.35, WA = 0.35;

    // Estados de metrica -> penalizacion sobre 100 (normal no penaliza)
    private static final double PEN_WARNING = 20, PEN_DEGRADED = 45, PEN_CRITICAL = 75;

    // ---- Definicion de umbrales por metrica (copiado del frontend) ----
    // higherIsWorse: warningAt, degradedAt, criticalAt
    // lowerIsWorse: warningBelow, degradedBelow, criticalBelow
    private enum Dir { HIGHER, LOWER, REFERENCE }

    private record Umbral(String key, String label, String componente, Dir dir,
                          double t1, double t2, double t3, String unidad, String help) {}

    private static final List<Umbral> UMBRALES = List.of(
        // ---- PROCESOS ----
        new Umbral("procesosActuales", "Procesos actuales", "Procesos", Dir.HIGHER, 208, 255, 285, "", "Procesos Oracle activos frente al limite configurado."),
        new Umbral("limiteProcesos", "Limite de procesos", "Procesos", Dir.REFERENCE, 0,0,0, "", "Maximo de procesos permitido (referencia)."),
        new Umbral("sesionesActuales", "Sesiones actuales", "Procesos", Dir.HIGHER, 345, 425, 475, "", "Total de sesiones conectadas."),
        new Umbral("sesionesActivas", "Sesiones activas", "Procesos", Dir.HIGHER, 70, 85, 95, "%", "Porcentaje de sesiones ejecutando actividad."),
        new Umbral("sesionesInactivas", "Sesiones inactivas", "Procesos", Dir.HIGHER, 70, 85, 95, "%", "Sesiones conectadas sin actividad."),
        new Umbral("sesionesBloqueadas", "Sesiones bloqueadas", "Procesos", Dir.HIGHER, 1, 3, 5, "", "Sesiones que esperan por un bloqueo."),
        new Umbral("operacionesProlongadas", "Operaciones prolongadas", "Procesos", Dir.HIGHER, 2, 4, 6, "", "Operaciones que corren mucho tiempo."),
        new Umbral("usoRecursos", "Uso de limites de recursos", "Procesos", Dir.HIGHER, 70, 85, 95, "%", "Mayor utilizacion entre los limites configurados."),
        // ---- MEMORIA ----
        new Umbral("tamanoSga", "Tamano de SGA", "Memoria", Dir.REFERENCE, 0,0,0, " GB", "Tamano total de la SGA (referencia)."),
        new Umbral("sgaLibre", "Memoria libre de SGA", "Memoria", Dir.LOWER, 20, 10, 5, "%", "Porcentaje disponible dentro de la SGA."),
        new Umbral("sharedPoolUso", "Uso de Shared Pool", "Memoria", Dir.HIGHER, 81, 89, 95, "%", "Memoria para SQL compartido y estructuras."),
        new Umbral("bufferCacheHit", "Aciertos de Buffer Cache", "Memoria", Dir.LOWER, 90, 85, 80, "%", "Lecturas atendidas desde memoria."),
        new Umbral("pgaAsignada", "PGA asignada", "Memoria", Dir.HIGHER, 76, 86, 95, "%", "Porcentaje asignado respecto al objetivo de PGA."),
        new Umbral("pgaUtilizada", "PGA utilizada", "Memoria", Dir.HIGHER, 76, 86, 95, "%", "Memoria PGA usada por los procesos."),
        new Umbral("pgaMaxima", "PGA maxima observada", "Memoria", Dir.HIGHER, 81, 89, 95, "%", "Pico de asignacion de PGA."),
        new Umbral("overAllocation", "Over-allocation", "Memoria", Dir.HIGHER, 1, 3, 5, "", "Veces que se asigno PGA sobre el objetivo."),
        new Umbral("pgaCacheHit", "Cache hit de PGA", "Memoria", Dir.LOWER, 90, 85, 80, "%", "Eficiencia del administrador de PGA."),
        // ---- ARCHIVOS ----
        new Umbral("datafilesOnline", "Datafiles online", "Archivos", Dir.REFERENCE, 0,0,0, "", "Datafiles disponibles (referencia)."),
        new Umbral("datafilesOffline", "Datafiles offline", "Archivos", Dir.HIGHER, 1, 2, 3, "", "Datafiles fuera de linea."),
        new Umbral("capacidadDatafiles", "Capacidad de datafiles", "Archivos", Dir.HIGHER, 80, 85, 90, "%", "Ocupacion del datafile con menos espacio."),
        new Umbral("tablespaces", "Espacio de tablespaces", "Archivos", Dir.HIGHER, 80, 85, 90, "%", "Tablespace con mayor ocupacion."),
        new Umbral("tempfilesProblema", "Tempfiles con problemas", "Archivos", Dir.HIGHER, 1, 2, 3, "", "Tempfiles que requieren atencion."),
        new Umbral("redoProblema", "Redo logs con problemas", "Archivos", Dir.HIGHER, 1, 2, 3, "", "Grupos/miembros de redo con estado anormal."),
        new Umbral("archivosInvalidos", "Archivos invalidos", "Archivos", Dir.HIGHER, 1, 2, 3, "", "Archivos con estado invalido."),
        new Umbral("archivosInaccesibles", "Archivos inaccesibles", "Archivos", Dir.HIGHER, 1, 2, 3, "", "Archivos que Oracle no puede abrir.")
    );

    // Devuelve el valor de una metrica desde la lectura, por su key
    private Integer valor(LecturaMetricas l, String key) {
        return switch (key) {
            case "procesosActuales" -> l.getProcesosActuales();
            case "limiteProcesos" -> l.getLimiteProcesos();
            case "sesionesActuales" -> l.getSesionesActuales();
            case "sesionesActivas" -> l.getSesionesActivas();
            case "sesionesInactivas" -> l.getSesionesInactivas();
            case "sesionesBloqueadas" -> l.getSesionesBloqueadas();
            case "operacionesProlongadas" -> l.getOperacionesProlongadas();
            case "usoRecursos" -> l.getUsoRecursos();
            case "tamanoSga" -> l.getTamanoSga();
            case "sgaLibre" -> l.getSgaLibre();
            case "sharedPoolUso" -> l.getSharedPoolUso();
            case "bufferCacheHit" -> l.getBufferCacheHit();
            case "pgaAsignada" -> l.getPgaAsignada();
            case "pgaUtilizada" -> l.getPgaUtilizada();
            case "pgaMaxima" -> l.getPgaMaxima();
            case "overAllocation" -> l.getOverAllocation();
            case "pgaCacheHit" -> l.getPgaCacheHit();
            case "datafilesOnline" -> l.getDatafilesOnline();
            case "datafilesOffline" -> l.getDatafilesOffline();
            case "capacidadDatafiles" -> l.getCapacidadDatafiles();
            case "tablespaces" -> l.getTablespaces();
            case "tempfilesProblema" -> l.getTempfilesProblema();
            case "redoProblema" -> l.getRedoProblema();
            case "archivosInvalidos" -> l.getArchivosInvalidos();
            case "archivosInaccesibles" -> l.getArchivosInaccesibles();
            default -> null;
        };
    }

    // Estado de una metrica segun su umbral
    private String estadoMetrica(Umbral u, Integer v) {
        if (u.dir == Dir.REFERENCE || v == null) return "normal";
        if (u.dir == Dir.HIGHER) {
            if (v >= u.t3) return "critical";
            if (v >= u.t2) return "degraded";
            if (v >= u.t1) return "warning";
            return "normal";
        } else { // LOWER: peor cuanto mas bajo
            if (v < u.t3) return "critical";
            if (v < u.t2) return "degraded";
            if (v < u.t1) return "warning";
            return "normal";
        }
    }

    private double penalizacion(String estado) {
        return switch (estado) {
            case "warning" -> PEN_WARNING;
            case "degraded" -> PEN_DEGRADED;
            case "critical" -> PEN_CRITICAL;
            default -> 0;
        };
    }

    // Indicador de un componente: 100 menos el promedio de penalizaciones de sus metricas no-referencia
    private double indicadorComponente(LecturaMetricas l, String componente) {
        double sumaPen = 0; int n = 0;
        for (Umbral u : UMBRALES) {
            if (!u.componente.equals(componente) || u.dir == Dir.REFERENCE) continue;
            String est = estadoMetrica(u, valor(l, u.key));
            sumaPen += penalizacion(est);
            n++;
        }
        if (n == 0) return 100;
        double valor = 100 - (sumaPen / n);
        return Math.max(0, Math.round(valor * 10) / 10.0);
    }

    private String estadoIsbd(double isbd) {
        if (isbd >= 90) return "optimal";
        if (isbd >= 75) return "healthy";
        if (isbd >= 60) return "warning";
        if (isbd >= 40) return "degraded";
        return "critical";
    }

    // ¿algun componente esta en critico? (regla: critico manda)
    private boolean hayComponenteCritico(double ip, double im, double ia) {
        return ip < 40 || im < 40 || ia < 40;
    }

    /** Calcula IP, IM, IA e ISBD para una lectura. */
    public InstanciaSaludDTO calcularSalud(LecturaMetricas l) {
        double ip = indicadorComponente(l, "Procesos");
        double im = indicadorComponente(l, "Memoria");
        double ia = indicadorComponente(l, "Archivos");
        double isbd = Math.round((WP * ip + WM * im + WA * ia) * 10) / 10.0;

        InstanciaSaludDTO dto = new InstanciaSaludDTO();
        dto.ip = ip; dto.im = im; dto.ia = ia; dto.isbd = isbd;
        // regla: si un componente es critico, el estado global es critico
        dto.estado = hayComponenteCritico(ip, im, ia) ? "critical" : estadoIsbd(isbd);
        dto.totalAlertas = generarAlertas(l).size();
        return dto;
    }

    /** Lista de alertas: toda metrica cuyo estado no sea normal. */
    public List<AlertaDTO> generarAlertas(LecturaMetricas l) {
        List<AlertaDTO> alertas = new ArrayList<>();
        for (Umbral u : UMBRALES) {
            if (u.dir == Dir.REFERENCE) continue;
            Integer v = valor(l, u.key);
            String est = estadoMetrica(u, v);
            if (est.equals("normal")) continue;
            AlertaDTO a = new AlertaDTO();
            a.componente = u.componente;
            a.variable = u.label;
            a.valor = (v == null ? "-" : v) + u.unidad;
            a.nivel = est;
            a.descripcion = u.help;
            alertas.add(a);
        }
        return alertas;
    }

    /** Detalle completo: indicadores con sus metricas + alertas. */
    public InstanciaDetalleDTO calcularDetalle(LecturaMetricas l) {
        InstanciaDetalleDTO d = new InstanciaDetalleDTO();
        double ip = indicadorComponente(l, "Procesos");
        double im = indicadorComponente(l, "Memoria");
        double ia = indicadorComponente(l, "Archivos");
        d.isbd = Math.round((WP * ip + WM * im + WA * ia) * 10) / 10.0;
        d.estado = hayComponenteCritico(ip, im, ia) ? "critical" : estadoIsbd(d.isbd);

        d.indicadores = List.of(
            construirIndicador(l, "IP", "Procesos", ip),
            construirIndicador(l, "IM", "Memoria", im),
            construirIndicador(l, "IA", "Archivos", ia)
        );
        d.alertas = generarAlertas(l);
        return d;
    }

    private IndicadorDTO construirIndicador(LecturaMetricas l, String codigo, String componente, double valor) {
        IndicadorDTO ind = new IndicadorDTO();
        ind.codigo = codigo;
        ind.label = componente;
        ind.valor = valor;
        ind.estado = valor < 40 ? "critical" : (valor < 60 ? "degraded" : (valor < 75 ? "warning" : "normal"));
        ind.metricas = new ArrayList<>();
        for (Umbral u : UMBRALES) {
            if (!u.componente.equals(componente)) continue;
            IndicadorDTO.MetricaDTO m = new IndicadorDTO.MetricaDTO();
            m.key = u.key;
            m.label = u.label;
            m.valor = valor(l, u.key);
            m.estado = estadoMetrica(u, m.valor);
            ind.metricas.add(m);
        }
        return ind;
    }
}
