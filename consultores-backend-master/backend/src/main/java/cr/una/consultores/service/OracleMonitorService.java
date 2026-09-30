package cr.una.consultores.service;

import cr.una.consultores.dto.LecturaCrudaDTO;
import cr.una.consultores.dto.ReporteAgenteDTO;
import cr.una.consultores.dto.SaludInstanciaDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Lee la instancia Oracle Autonomous y produce LECTURAS CRUDAS, con los
 * mismos codigos de variable que usa el agente.
 *
 * El cambio respecto a la version anterior: este servicio ya no calcula
 * estados ni indices. Solo mide. La evaluacion la hace el evaluador, que
 * es el mismo para las dos instancias. Eso es lo que hace comparables sus
 * indices y lo que permite que un ajuste de umbral valga para ambas sin
 * tocar dos codigos.
 *
 * Cada lectura va envuelta en su propio try: Autonomous restringe algunas
 * vistas V$ segun la version, y una restriccion no debe tumbar el resto
 * de la medicion. Lo que no se pueda leer simplemente no se reporta, y el
 * evaluador reescala con lo que si llego.
 */
@Service
public class OracleMonitorService {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final JdbcTemplate oracle;
    private final EvaluadorSaludService evaluador;

    @Value("${oracle.instancia.id:autonomous-monitordb}")      private String insId;
    @Value("${oracle.instancia.nombre:monitordb}")             private String insNombre;
    @Value("${oracle.instancia.empresa:Oracle Cloud · Autonomous}") private String insEmpresa;

    public OracleMonitorService(@Nullable @Qualifier("oracleJdbcTemplate") JdbcTemplate oracle,
                                EvaluadorSaludService evaluador) {
        this.oracle = oracle;
        this.evaluador = evaluador;
    }

    public boolean disponible() { return oracle != null; }

    public SaludInstanciaDTO leerSalud() {
        return evaluador.evaluar(medir(), "directo");
    }

    public SaludInstanciaDTO instanciaCaida(String detalle) {
        ReporteAgenteDTO r = cabecera();
        r.baseAccesible = false;
        r.errorLectura = detalle;
        return evaluador.evaluar(r, "directo");
    }

    private ReporteAgenteDTO cabecera() {
        ReporteAgenteDTO r = new ReporteAgenteDTO();
        r.instanciaId = insId;
        r.nombre = insNombre;
        r.empresa = insEmpresa;
        r.tipo = "autonomous";
        r.ultimaLectura = ISO.format(Instant.now());
        return r;
    }

    /** Ejecuta una medicion aislada: si la vista esta restringida, se omite. */
    private void leer(ReporteAgenteDTO r, String codigo, Supplier<Double> f) {
        try {
            Double v = f.get();
            if (v != null) r.lecturas.add(new LecturaCrudaDTO(codigo, v));
        } catch (Exception ignorado) {
            // Vista no disponible en esta instancia: la variable no se reporta
        }
    }

    private ReporteAgenteDTO medir() {
        if (oracle == null) throw new IllegalStateException("Conexión a Oracle no configurada");
        ReporteAgenteDTO r = cabecera();

        // ---------------- PROCESOS ----------------
        leer(r, "p1", () -> pctRecurso("processes"));
        leer(r, "p8", () -> pctRecurso("sessions"));
        leer(r, "p2", () -> {
            Map<String, Object> m = oracle.queryForMap(
                    "SELECT max_utilization mx, CASE WHEN limit_value IN ('UNLIMITED','-1') THEN 0 " +
                            "ELSE TO_NUMBER(limit_value) END tp FROM v$resource_limit WHERE resource_name='processes'");
            double tp = num(m.get("TP"));
            return tp == 0 ? null : num(m.get("MX")) * 100 / tp;
        });
        leer(r, "p3", () -> escalar("SELECT COUNT(*) FROM v$session"));
        leer(r, "p4", () -> escalar("SELECT COUNT(*) FROM v$session WHERE status='ACTIVE'"));
        leer(r, "p5", () -> escalar("SELECT COUNT(*) FROM v$session WHERE status='INACTIVE'"));
        leer(r, "p6", () -> escalar("SELECT COUNT(*) FROM v$session WHERE blocking_session IS NOT NULL"));
        leer(r, "p7", () -> escalar("SELECT COUNT(*) FROM v$session_longops WHERE time_remaining>0"));
        leer(r, "p9", () -> escalar("SELECT COUNT(*) FROM v$session WHERE status IN ('KILLED','SNIPED')"));
        leer(r, "p12", () -> NVL(escalar(
                "SELECT NVL(SUM(total_waits),0) FROM v$system_event WHERE event='free buffer waits'")));
        leer(r, "p13", () -> esperaMs("db file parallel write"));
        leer(r, "p15", () -> esperaMs("log file sync"));
        leer(r, "p16", () -> NVL(escalar(
                "SELECT NVL(SUM(value),0) FROM v$sysstat WHERE name='redo log space requests'")));
        leer(r, "p14", () -> {
            Map<String, Object> m = oracle.queryForMap(
                    "SELECT MAX(CASE WHEN name='free buffer inspected' THEN value END) i, " +
                            "       MAX(CASE WHEN name='free buffer requested' THEN value END) s " +
                            "FROM v$sysstat WHERE name IN ('free buffer inspected','free buffer requested')");
            double s = num(m.get("S"));
            return s == 0 ? 0.0 : num(m.get("I")) / s;
        });

        // ---------------- MEMORIA ----------------
        leer(r, "m1", () -> escalar("SELECT ROUND(SUM(bytes)/1024/1024,2) FROM v$sgastat"));
        leer(r, "m2", () -> NVL(escalar(
                "SELECT NVL(ROUND(SUM(bytes)/1024/1024,2),0) FROM v$sgainfo WHERE name='Free SGA Memory Available'")));
        leer(r, "m3", () -> escalar(
                "SELECT ROUND(SUM(CASE WHEN name='free memory' THEN bytes ELSE 0 END)*100/NULLIF(SUM(bytes),0),2) " +
                        "FROM v$sgastat WHERE pool='shared pool'"));
        leer(r, "m4", () -> escalar(
                "SELECT ROUND((1-(p.value/NULLIF(d.value+c.value,0)))*100,2) " +
                        "FROM v$sysstat d, v$sysstat c, v$sysstat p " +
                        "WHERE d.name='db block gets' AND c.name='consistent gets' AND p.name='physical reads'"));
        leer(r, "m10", () -> esperaMs("db file sequential read"));
        leer(r, "m5", () -> pgaPct("total PGA allocated"));
        leer(r, "m6", () -> pgaMb("total PGA inuse"));
        leer(r, "m7", () -> pgaMb("maximum PGA allocated"));
        leer(r, "m8", () -> pga("over allocation count"));
        leer(r, "m9", () -> pga("cache hit percentage"));
        leer(r, "m11", () -> {
            Map<String, Object> m = oracle.queryForMap(
                    "SELECT MAX(CASE WHEN name='sorts (disk)' THEN value END) d, " +
                            "       MAX(CASE WHEN name='sorts (memory)' THEN value END) m " +
                            "FROM v$sysstat WHERE name IN ('sorts (disk)','sorts (memory)')");
            double tot = num(m.get("D")) + num(m.get("M"));
            return tot == 0 ? 0.0 : num(m.get("D")) * 100 / tot;
        });

        // ---------------- ARCHIVOS ----------------
        leer(r, "a1", () -> escalar("SELECT COUNT(*) FROM v$datafile WHERE status IN ('ONLINE','SYSTEM')"));
        leer(r, "a2", () -> escalar("SELECT COUNT(*) FROM v$datafile WHERE status NOT IN ('ONLINE','SYSTEM')"));
        leer(r, "a3", () -> escalar("SELECT COUNT(*) FROM dba_data_files WHERE autoextensible='NO'"));
        leer(r, "a7", () -> NVL(escalar("SELECT COUNT(*) FROM v$logfile WHERE status IN ('INVALID','STALE')")));
        leer(r, "a8", () -> NVL(escalar("SELECT COUNT(*) FROM v$recover_file")));
        leer(r, "a9", () -> escalar(
                "SELECT CASE WHEN log_mode='ARCHIVELOG' THEN 1 ELSE 0 END FROM v$database"));
        leer(r, "a11", () -> escalar("SELECT COUNT(*) FROM v$log"));
        leer(r, "a6", () -> escalar("SELECT NVL(SUM(CASE WHEN members<2 THEN 1 ELSE 0 END),0) FROM v$log"));
        leer(r, "a10", () -> NVL(escalar(
                "SELECT NVL(ROUND(SUM(percent_space_used)),0) FROM v$recovery_area_usage")));

        // a4 · el PEOR tablespace, con el desglose por tablespace (nivel 3)
        try {
            LecturaCrudaDTO a4 = new LecturaCrudaDTO("a4", null);
            List<Map<String, Object>> filas = oracle.queryForList(
                    "SELECT m.tablespace_name t, ROUND(m.used_percent,2) p, " +
                            "       ROUND(m.used_space*t2.block_size/1024/1024,2) usado_mb " +
                            "FROM dba_tablespace_usage_metrics m " +
                            "JOIN dba_tablespaces t2 ON t2.tablespace_name=m.tablespace_name " +
                            "ORDER BY m.used_percent DESC");
            double peor = 0;
            for (Map<String, Object> f : filas) {
                double p = num(f.get("P"));
                peor = Math.max(peor, p);
                LecturaCrudaDTO.Detalle d = new LecturaCrudaDTO.Detalle(
                        String.valueOf(f.get("T")), null, p);
                d.nota = f.get("USADO_MB") + " MB usados";
                a4.detalle.add(d);
            }
            a4.valor = peor;
            r.lecturas.add(a4);
        } catch (Exception ignorado) { }

        // a5 · tablespace temporal
        leer(r, "a5", () -> escalar(
                "SELECT ROUND(MAX((allocated_space-free_space)*100/NULLIF(tablespace_size,0)),2) " +
                        "FROM dba_temp_free_space"));

        return r;
    }

    // ---------------- utilitarios ----------------
    private Double escalar(String sql) { return oracle.queryForObject(sql, Double.class); }
    private Double NVL(Double d) { return d == null ? 0.0 : d; }
    private double num(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }

    private Double pctRecurso(String recurso) {
        Map<String, Object> m = oracle.queryForMap(
                "SELECT current_utilization cu, CASE WHEN limit_value IN ('UNLIMITED','-1') THEN 0 " +
                        "ELSE TO_NUMBER(limit_value) END tp FROM v$resource_limit WHERE resource_name='" + recurso + "'");
        double tp = num(m.get("TP"));
        return tp == 0 ? null : Math.round(num(m.get("CU")) * 10000 / tp) / 100.0;
    }

    private Double esperaMs(String evento) {
        Double v = oracle.queryForObject(
                "SELECT ROUND(NVL(SUM(time_waited_micro)/NULLIF(SUM(total_waits),0)/1000,0),2) " +
                        "FROM v$system_event WHERE event=?", Double.class, evento);
        return v == null ? 0.0 : v;
    }

    private Double pga(String nombre) {
        return oracle.queryForObject(
                "SELECT value FROM v$pgastat WHERE name=?", Double.class, nombre);
    }

    private Double pgaMb(String nombre) {
        Double v = pga(nombre);
        return v == null ? null : Math.round(v / 1024 / 1024 * 100) / 100.0;
    }

    private Double pgaPct(String nombre) {
        Double v = pga(nombre), t = pga("aggregate PGA target parameter");
        return (v == null || t == null || t == 0) ? null : Math.round(v * 10000 / t) / 100.0;
    }
}