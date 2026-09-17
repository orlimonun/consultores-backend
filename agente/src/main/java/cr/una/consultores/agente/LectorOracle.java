package cr.una.consultores.agente;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Lee las metricas de la instancia Oracle local y arma el reporte.
 *
 * Cuatro componentes:
 *   IP Procesos      sesiones, bloqueos y cercania al limite de procesos
 *   IM Memoria       uso de SGA y eficiencia del buffer cache
 *   IA Archivos      ocupacion de tablespaces, datafiles y distribucion
 *   IR Recuperacion  modo de archivado, multiplexado de bitacoras y area
 *                    de recuperacion
 *
 * El cuarto es el que una Autonomous no puede reportar, porque alli Oracle
 * administra el almacenamiento y la recuperacion y no permite modificarlos.
 * En una instancia tradicional eso es responsabilidad del administrador, asi
 * que aqui si tiene sentido medirlo.
 */
@Service
public class LectorOracle {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final JdbcTemplate cdb;
    private final JdbcTemplate pdb;

    @Value("${agente.instancia-id}") private String instanciaId;
    @Value("${agente.nombre}")       private String nombre;
    @Value("${agente.empresa}")      private String empresa;
    @Value("${agente.esquema}")      private String esquema;

    public LectorOracle(@Qualifier("cdbTemplate") JdbcTemplate cdb,
                        @Qualifier("pdbTemplate") JdbcTemplate pdb) {
        this.cdb = cdb;
        this.pdb = pdb;
    }

    /** Reporte que se envia cuando no se pudo leer la base. La instancia
     *  aparece caida en el tablero, que es distinto de que el agente calle. */
    public ReporteSalud caida(String detalle) {
        ReporteSalud r = identidad(new ReporteSalud());
        r.ip = 0; r.im = 0; r.ia = 0; r.ir = 0.0; r.isbd = 0;
        r.estado = "critical";
        r.add("Procesos", "Base local inaccesible", 0, "critical");
        return r;
    }

    private ReporteSalud identidad(ReporteSalud r) {
        r.instanciaId = instanciaId;
        r.nombre = nombre;
        r.empresa = empresa;
        r.tipo = "tradicional";
        r.ultimaLectura = ISO.format(Instant.now());
        return r;
    }

    public ReporteSalud leer() {
        ReporteSalud r = identidad(new ReporteSalud());

        double ip = leerProcesos(r);
        double im = leerMemoria(r);
        double ia = leerArchivos(r);
        double ir = leerRecuperacion(r);

        r.ip = ip; r.im = im; r.ia = ia; r.ir = ir;
        // Formula de cuatro componentes. Memoria y archivos pesan mas porque
        // degradan el rendimiento de forma continua; procesos solo importa
        // cerca del limite y recuperacion se comporta casi como un interruptor.
        r.isbd = Math.round((0.20 * ip + 0.30 * im + 0.30 * ia + 0.20 * ir) * 10) / 10.0;

        boolean critico = ip <= 40 || im <= 40 || ia <= 40 || ir <= 40;
        r.estado = critico ? "critical" : escala(r.isbd);
        return r;
    }

    // ------------------------------------------------------------------
    // IP — Procesos (contenedor raiz: son de la instancia completa)
    // ------------------------------------------------------------------
    private double leerProcesos(ReporteSalud r) {
        Map<String, Object> s = cdb.queryForMap(
            "SELECT COUNT(*) AS total, " +
            " SUM(CASE WHEN status='ACTIVE' THEN 1 ELSE 0 END) AS activas, " +
            " SUM(CASE WHEN status='INACTIVE' THEN 1 ELSE 0 END) AS inactivas, " +
            " SUM(CASE WHEN blocking_session IS NOT NULL THEN 1 ELSE 0 END) AS bloqueadas " +
            "FROM v$session");

        int total = i(s.get("TOTAL")), activas = i(s.get("ACTIVAS"));
        int inactivas = i(s.get("INACTIVAS")), bloqueadas = i(s.get("BLOQUEADAS"));

        // V$RESOURCE_LIMIT da el uso actual Y el techo configurado. Contar
        // filas de V$PROCESS solo responde "cuantos hay"; esto responde
        // "que tan cerca estoy del limite", que es lo que importa: cuando el
        // limite se agota, la base rechaza toda conexion nueva (ORA-00020).
        Map<String, Object> lim = cdb.queryForMap(
            "SELECT current_utilization AS actual, max_utilization AS pico, " +
            "       CASE WHEN limit_value IN ('UNLIMITED','-1') THEN 0 " +
            "            ELSE TO_NUMBER(limit_value) END AS tope " +
            "FROM v$resource_limit WHERE resource_name='processes'");
        int actual = i(lim.get("ACTUAL")), pico = i(lim.get("PICO")), tope = i(lim.get("TOPE"));
        int pctProcesos = tope == 0 ? 0 : (int) Math.round(actual * 100.0 / tope);

        r.add("Procesos", "Sesiones totales", total, null);
        r.add("Procesos", "Sesiones activas", activas, null);
        r.add("Procesos", "Sesiones inactivas", inactivas, null);
        r.add("Procesos", "Sesiones bloqueadas", bloqueadas,
              bloqueadas >= 5 ? "critical" : bloqueadas >= 1 ? "warning" : "normal");
        r.add("Procesos", "Procesos", actual, null);
        r.add("Procesos", "Pico de procesos", pico, null);
        r.add("Procesos", "Uso del limite de procesos (%)", pctProcesos,
              pctProcesos >= 90 ? "critical" : pctProcesos >= 75 ? "warning" : "normal");

        double ip = 100;
        if (bloqueadas >= 5 || pctProcesos >= 90) ip = 40;
        else if (bloqueadas >= 1 || pctProcesos >= 75) ip = 80;
        return ip;
    }

    // ------------------------------------------------------------------
    // IM — Memoria (contenedor raiz: la SGA es de la instancia)
    // ------------------------------------------------------------------
    private double leerMemoria(ReporteSalud r) {
        Map<String, Object> sga = cdb.queryForMap(
            "SELECT ROUND(SUM(bytes)/1024/1024) AS total_mb, " +
            " ROUND(SUM(CASE WHEN name='free memory' THEN bytes ELSE 0 END)/1024/1024) AS free_mb " +
            "FROM v$sgastat");
        int total = i(sga.get("TOTAL_MB")), libre = i(sga.get("FREE_MB"));
        double usoPct = total == 0 ? 0 : Math.round((1.0 - (double) libre / total) * 1000) / 10.0;

        // Ratio de acierto del buffer cache: que proporcion de las lecturas se
        // resolvio en memoria sin ir al disco. Es la misma distincion entre
        // consistent gets y physical reads que aparece en AUTOTRACE.
        Double hit = cdb.queryForObject(
            "SELECT ROUND((1 - (phy.value / NULLIF(cur.value + con.value,0))) * 100, 2) " +
            "FROM v$sysstat cur, v$sysstat con, v$sysstat phy " +
            "WHERE cur.name='db block gets' AND con.name='consistent gets' " +
            "  AND phy.name='physical reads'", Double.class);
        if (hit == null) hit = 100.0;

        r.add("Memoria", "SGA total (MB)", total, "normal");
        r.add("Memoria", "SGA libre (MB)", libre, null);
        r.add("Memoria", "Uso de SGA (%)", (int) Math.round(usoPct),
              usoPct >= 95 ? "critical" : usoPct >= 85 ? "warning" : "normal");
        r.add("Memoria", "Buffer cache hit (%)", (int) Math.round(hit),
              hit < 80 ? "critical" : hit < 90 ? "warning" : "normal");

        double im = 100;
        if (usoPct >= 95 || hit < 80) im = 40;
        else if (usoPct >= 85 || hit < 90) im = 80;
        return im;
    }

    // ------------------------------------------------------------------
    // IA — Archivos (contenedor conectable: ahi viven los tablespaces)
    // ------------------------------------------------------------------
    private double leerArchivos(ReporteSalud r) {
        // DBA_TABLESPACE_USAGE_METRICS calcula el uso contra el tamano MAXIMO
        // al que el datafile puede crecer, no contra el que tiene hoy. Con
        // AUTOEXTEND el archivo siempre esta casi lleno por diseno, asi que
        // medir contra el tamano actual reporta cerca del 100 % siempre.
        Map<String, Object> ts = pdb.queryForMap(
            "SELECT COUNT(*) AS total, ROUND(NVL(MAX(used_percent),0)) AS max_uso " +
            "FROM dba_tablespace_usage_metrics");
        int tablespaces = i(ts.get("TOTAL")), maxUso = i(ts.get("MAX_USO"));

        Map<String, Object> df = pdb.queryForMap(
            "SELECT SUM(CASE WHEN status IN ('ONLINE','SYSTEM') THEN 1 ELSE 0 END) AS ok, " +
            "       SUM(CASE WHEN status NOT IN ('ONLINE','SYSTEM') THEN 1 ELSE 0 END) AS mal " +
            "FROM v$datafile");
        int dfOk = i(df.get("OK")), dfMal = i(df.get("MAL"));

        r.add("Archivos", "Tablespaces", tablespaces, "normal");
        r.add("Archivos", "Max uso tablespace (%)", maxUso,
              maxUso >= 90 ? "critical" : maxUso >= 80 ? "warning" : "normal");
        r.add("Archivos", "Datafiles online", dfOk, "normal");
        r.add("Archivos", "Datafiles con problema", dfMal, dfMal >= 1 ? "critical" : "normal");

        double ia = 100;
        if (maxUso >= 90 || dfMal >= 1) ia = 40;
        else if (maxUso >= 80) ia = 80;

        // --- Metricas del esquema: distribucion e indices de clave foranea ---
        // Si el esquema no existe, se omiten en vez de fallar.
        try {
            Integer existe = pdb.queryForObject(
                "SELECT COUNT(*) FROM dba_users WHERE username = ?", Integer.class, esquema);
            if (existe != null && existe > 0) {
                Integer tsUsados = pdb.queryForObject(
                    "SELECT COUNT(DISTINCT tablespace_name) FROM dba_segments WHERE owner = ?",
                    Integer.class, esquema);
                Integer enUsers = pdb.queryForObject(
                    "SELECT COUNT(*) FROM dba_segments WHERE owner = ? AND tablespace_name = 'USERS'",
                    Integer.class, esquema);

                // Claves foraneas sin indice fisico. Oracle crea indice
                // automatico solo para PRIMARY KEY y UNIQUE; para FOREIGN KEY
                // no crea nada, y sin el, cada borrado en la tabla padre
                // recorre la hija completa y la bloquea.
                Integer fkSinIdx = pdb.queryForObject(
                    "SELECT COUNT(*) FROM dba_constraints c " +
                    "JOIN dba_cons_columns cc ON cc.constraint_name = c.constraint_name " +
                    "                        AND cc.owner = c.owner " +
                    "WHERE c.owner = ? AND c.constraint_type = 'R' " +
                    "AND NOT EXISTS (SELECT 1 FROM dba_ind_columns ic " +
                    "                WHERE ic.table_owner = c.owner " +
                    "                AND ic.table_name = cc.table_name " +
                    "                AND ic.column_name = cc.column_name " +
                    "                AND ic.column_position = cc.position)",
                    Integer.class, esquema);

                int distintos = n(tsUsados), users = n(enUsers), sinIdx = n(fkSinIdx);
                r.add("Archivos", "Tablespaces usados por el esquema", distintos, "normal");
                r.add("Archivos", "Segmentos aun en USERS", users,
                      users > 0 ? "warning" : "normal");
                r.add("Archivos", "Claves foraneas sin indice", sinIdx,
                      sinIdx >= 3 ? "critical" : sinIdx >= 1 ? "warning" : "normal");

                if (sinIdx >= 3 || users > 0) ia = Math.min(ia, 60);
                else if (sinIdx >= 1) ia = Math.min(ia, 80);
            }
        } catch (Exception ignorado) {
            // Sin permisos o sin esquema: el indicador se calcula sin esta parte
        }
        return ia;
    }

    // ------------------------------------------------------------------
    // IR — Recuperacion (contenedor raiz: bitacoras y archivado)
    // ------------------------------------------------------------------
    private double leerRecuperacion(ReporteSalud r) {
        String modo = cdb.queryForObject("SELECT log_mode FROM v$database", String.class);
        boolean archivelog = "ARCHIVELOG".equalsIgnoreCase(modo);

        Map<String, Object> logs = cdb.queryForMap(
            "SELECT COUNT(*) AS grupos, NVL(MIN(members),0) AS min_miembros, " +
            "       SUM(CASE WHEN members < 2 THEN 1 ELSE 0 END) AS sin_espejo " +
            "FROM v$log");
        int grupos = i(logs.get("GRUPOS"));
        int minMiembros = i(logs.get("MIN_MIEMBROS"));
        int sinEspejo = i(logs.get("SIN_ESPEJO"));

        Integer malos = cdb.queryForObject(
            "SELECT COUNT(*) FROM v$logfile WHERE status IN ('INVALID','STALE')", Integer.class);

        int fraPct = 0;
        try {
            Double fra = cdb.queryForObject(
                "SELECT NVL(ROUND(SUM(percent_space_used)),0) FROM v$recovery_area_usage",
                Double.class);
            fraPct = fra == null ? 0 : (int) Math.round(fra);
        } catch (Exception ignorado) {
            // Sin area de recuperacion configurada
        }

        r.add("Recuperacion", "Modo ARCHIVELOG", archivelog ? 1 : 0,
              archivelog ? "normal" : "critical");
        r.add("Recuperacion", "Grupos de bitacora", grupos,
              grupos < 2 ? "critical" : grupos < 3 ? "warning" : "normal");
        r.add("Recuperacion", "Miembros por grupo (minimo)", minMiembros,
              minMiembros < 2 ? "critical" : "normal");
        r.add("Recuperacion", "Grupos sin espejo", sinEspejo,
              sinEspejo > 0 ? "critical" : "normal");
        r.add("Recuperacion", "Miembros danados", n(malos),
              n(malos) > 0 ? "warning" : "normal");
        r.add("Recuperacion", "Uso del area de recuperacion (%)", fraPct,
              fraPct >= 90 ? "critical" : fraPct >= 80 ? "warning" : "normal");

        double ir = 100;
        // Sin archivado no hay recuperacion posible ante un fallo de medios:
        // el redo se sobrescribe al completar el ciclo de bitacoras.
        if (!archivelog) ir = 40;
        if (grupos < 2) ir = 40;
        // Un grupo con un solo miembro es un punto unico de falla: si ese
        // archivo se pierde, el grupo queda inservible.
        if (sinEspejo > 0) ir = Math.min(ir, 60);
        if (n(malos) > 0) ir = Math.min(ir, 60);
        // Si el area de recuperacion se llena, la base se detiene (ORA-00257).
        if (fraPct >= 90) ir = Math.min(ir, 40);
        else if (fraPct >= 80) ir = Math.min(ir, 80);
        return ir;
    }

    private String escala(double v) {
        if (v >= 90) return "optimal";
        if (v >= 75) return "healthy";
        if (v >= 60) return "warning";
        if (v >= 40) return "degraded";
        return "critical";
    }

    private int i(Object o) { return o == null ? 0 : ((Number) o).intValue(); }
    private int n(Integer o) { return o == null ? 0 : o; }
}
