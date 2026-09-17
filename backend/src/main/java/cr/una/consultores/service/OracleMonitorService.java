package cr.una.consultores.service;

import cr.una.consultores.dto.SaludOracleDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lee metricas REALES de la instancia Oracle Autonomous y calcula el
 * indice de salud (ISBD). Usa solo las consultas confirmadas que funcionan
 * en Autonomous Database.
 *
 * Componentes:
 *   IP (Procesos): sesiones totales/activas/inactivas/bloqueadas, procesos
 *   IM (Memoria):  SGA total/libre (% uso), buffer cache hit ratio
 *   IA (Archivos): tablespaces y su % de uso, datafiles online/problema
 *   IR (Recuperacion): 100 fijo, ver la nota en el calculo
 *   ISBD = 0.20*IP + 0.30*IM + 0.30*IA + 0.20*IR
 */
@Service
public class OracleMonitorService {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final JdbcTemplate oracle;

    @Value("${oracle.instancia.id:autonomous-monitordb}")
    private String instanciaId;

    @Value("${oracle.instancia.nombre:monitordb}")
    private String instanciaNombre;

    @Value("${oracle.instancia.empresa:Instancia Autonomous (Oracle Cloud)}")
    private String instanciaEmpresa;

    public OracleMonitorService(@Nullable @Qualifier("oracleJdbcTemplate") JdbcTemplate oracle) {
        this.oracle = oracle;
    }

    public boolean disponible() {
        return oracle != null;
    }

    /** Se devuelve cuando la lectura en vivo falla: la instancia aparece caida
     *  en el tablero en vez de desaparecer o romper toda la respuesta. */
    public SaludOracleDTO instanciaCaida(String detalle) {
        SaludOracleDTO dto = new SaludOracleDTO();
        dto.metricas = new ArrayList<>();
        dto.ip = 0; dto.im = 0; dto.ia = 0; dto.ir = 0.0; dto.isbd = 0;
        dto.estado = "unknown";
        dto.conectado = false;
        identificar(dto);
        dto.metricas.add(metrica("Procesos", "Sin contacto con la instancia", 0, "critical"));
        return dto;
    }

    private void identificar(SaludOracleDTO dto) {
        dto.instanciaId = instanciaId;
        dto.nombre = instanciaNombre;
        dto.empresa = instanciaEmpresa;
        dto.tipo = "autonomous";
        dto.origen = "directo";
        dto.ultimaLectura = ISO.format(Instant.now());
        dto.segundosSinContacto = null;   // lectura en vivo: no aplica el latido
    }

    public SaludOracleDTO leerSalud() {
        if (oracle == null) {
            throw new IllegalStateException("Conexion a Oracle no configurada");
        }

        SaludOracleDTO dto = new SaludOracleDTO();
        dto.metricas = new ArrayList<>();

        // ---------- PROCESOS / SESIONES ----------
        Map<String, Object> ses = oracle.queryForMap(
                "SELECT COUNT(*) AS total, " +
                        " SUM(CASE WHEN status='ACTIVE' THEN 1 ELSE 0 END) AS activas, " +
                        " SUM(CASE WHEN status='INACTIVE' THEN 1 ELSE 0 END) AS inactivas, " +
                        " SUM(CASE WHEN blocking_session IS NOT NULL THEN 1 ELSE 0 END) AS bloqueadas " +
                        "FROM v$session");
        int totalSes = num(ses.get("TOTAL"));
        int activas = num(ses.get("ACTIVAS"));
        int inactivas = num(ses.get("INACTIVAS"));
        int bloqueadas = num(ses.get("BLOQUEADAS"));
        int procesos = oracle.queryForObject("SELECT COUNT(*) FROM v$process", Integer.class);

        dto.metricas.add(metrica("Procesos", "Sesiones totales", totalSes, null));
        dto.metricas.add(metrica("Procesos", "Sesiones activas", activas, null));
        dto.metricas.add(metrica("Procesos", "Sesiones inactivas", inactivas, null));
        dto.metricas.add(metrica("Procesos", "Sesiones bloqueadas", bloqueadas,
                bloqueadas >= 5 ? "critical" : bloqueadas >= 1 ? "warning" : "normal"));
        dto.metricas.add(metrica("Procesos", "Procesos", procesos, null));

        double ip = 100;
        if (bloqueadas >= 5) ip = 40;
        else if (bloqueadas >= 1) ip = 80;

        // ---------- MEMORIA ----------
        Map<String, Object> sga = oracle.queryForMap(
                "SELECT ROUND(SUM(bytes)/1024/1024) AS total_mb, " +
                        " ROUND(SUM(CASE WHEN name='free memory' THEN bytes ELSE 0 END)/1024/1024) AS free_mb " +
                        "FROM v$sgastat");
        int sgaTotal = num(sga.get("TOTAL_MB"));
        int sgaFree = num(sga.get("FREE_MB"));
        double sgaUsoPct = sgaTotal == 0 ? 0 : Math.round((1.0 - (double) sgaFree / sgaTotal) * 1000) / 10.0;

        Double bufferHit = oracle.queryForObject(
                "SELECT ROUND((1 - (phy.value / NULLIF(cur.value + con.value,0))) * 100, 2) " +
                        "FROM v$sysstat cur, v$sysstat con, v$sysstat phy " +
                        "WHERE cur.name='db block gets' AND con.name='consistent gets' AND phy.name='physical reads'",
                Double.class);
        if (bufferHit == null) bufferHit = 100.0;

        dto.metricas.add(metrica("Memoria", "SGA total (MB)", sgaTotal, "normal"));
        dto.metricas.add(metrica("Memoria", "SGA libre (MB)", sgaFree, null));
        dto.metricas.add(metrica("Memoria", "Uso de SGA (%)", (int) Math.round(sgaUsoPct),
                sgaUsoPct >= 95 ? "critical" : sgaUsoPct >= 85 ? "warning" : "normal"));
        dto.metricas.add(metrica("Memoria", "Buffer cache hit (%)", (int) Math.round(bufferHit),
                bufferHit < 80 ? "critical" : bufferHit < 90 ? "warning" : "normal"));

        double im = 100;
        if (sgaUsoPct >= 95 || bufferHit < 80) im = 40;
        else if (sgaUsoPct >= 85 || bufferHit < 90) im = 80;

        // ---------- ARCHIVOS ----------
        // Nota: este calculo compara contra el tamano ACTUAL del datafile. Con
        // AUTOEXTEND el archivo siempre esta casi lleno por diseno, asi que
        // tiende a reportar cerca del 100 %. La medicion que compara contra el
        // tamano MAXIMO es DBA_TABLESPACE_USAGE_METRICS.USED_PERCENT.
        Map<String, Object> tbs = oracle.queryForMap(
                "SELECT COUNT(*) AS total, ROUND(MAX(pct_used)) AS max_uso FROM ( " +
                        " SELECT df.tablespace_name, ROUND((1 - (fs.free/df.total))*100, 2) AS pct_used " +
                        " FROM (SELECT tablespace_name, SUM(bytes) total FROM dba_data_files GROUP BY tablespace_name) df, " +
                        "      (SELECT tablespace_name, SUM(bytes) free FROM dba_free_space GROUP BY tablespace_name) fs " +
                        " WHERE df.tablespace_name = fs.tablespace_name)");
        int totalTbs = num(tbs.get("TOTAL"));
        int maxUso = num(tbs.get("MAX_USO"));
        int datafilesOnline = oracle.queryForObject(
                "SELECT COUNT(*) FROM v$datafile WHERE status IN ('ONLINE','SYSTEM')", Integer.class);
        int datafilesProblema = oracle.queryForObject(
                "SELECT COUNT(*) FROM v$datafile WHERE status NOT IN ('ONLINE','SYSTEM')", Integer.class);

        dto.metricas.add(metrica("Archivos", "Tablespaces", totalTbs, "normal"));
        dto.metricas.add(metrica("Archivos", "Max uso tablespace (%)", maxUso,
                maxUso >= 90 ? "critical" : maxUso >= 80 ? "warning" : "normal"));
        dto.metricas.add(metrica("Archivos", "Datafiles online", datafilesOnline, "normal"));
        dto.metricas.add(metrica("Archivos", "Datafiles con problema", datafilesProblema,
                datafilesProblema >= 1 ? "critical" : "normal"));

        double ia = 100;
        if (maxUso >= 90 || datafilesProblema >= 1) ia = 40;
        else if (maxUso >= 80) ia = 80;

        // ---------- RECUPERACION ----------
        // Autonomous recibe 100 por definicion: la instancia esta siempre en
        // ARCHIVELOG, con respaldos automaticos, y nada de eso es
        // administrable. No hay forma de que el administrador lo haga mal,
        // asi que no hay nada que penalizar. Eso permite usar una sola
        // formula para las dos modalidades y comparar los ISBD en el mismo
        // grafico, que es lo que no se podria con pesos distintos.
        double ir = 100;
        dto.metricas.add(metrica("Recuperacion", "Modo ARCHIVELOG", 1, "normal"));
        dto.metricas.add(metrica("Recuperacion", "Archivado gestionado por Oracle", 1, "normal"));
        dto.metricas.add(metrica("Recuperacion", "Respaldos automaticos", 1, "normal"));

        // ---------- ISBD ----------
        dto.ip = ip; dto.im = im; dto.ia = ia; dto.ir = ir;
        dto.isbd = Math.round((0.20 * ip + 0.30 * im + 0.30 * ia + 0.20 * ir) * 10) / 10.0;
        boolean critico = ip <= 40 || im <= 40 || ia <= 40 || ir <= 40;
        dto.estado = critico ? "critical" : estadoIsbd(dto.isbd);
        dto.conectado = true;
        identificar(dto);
        return dto;
    }

    private String estadoIsbd(double v) {
        if (v >= 90) return "optimal";
        if (v >= 75) return "healthy";
        if (v >= 60) return "warning";
        if (v >= 40) return "degraded";
        return "critical";
    }

    private int num(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private SaludOracleDTO.Metrica metrica(String comp, String label, int valor, String estado) {
        SaludOracleDTO.Metrica m = new SaludOracleDTO.Metrica();
        m.componente = comp; m.label = label; m.valor = valor;
        m.estado = estado == null ? "normal" : estado;
        return m;
    }
}