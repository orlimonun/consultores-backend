package cr.una.consultores.agente;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Dos conexiones a la misma instancia Oracle, por servicios distintos.
 *
 * POR QUE DOS Y NO UNA
 * En Oracle multitenant los objetos viven en contenedores distintos:
 *   CDB (servicio XE)      -> bitacoras, modo de archivado, memoria, procesos
 *   PDB (servicio XEPDB1)  -> tablespaces, tablas, indices del esquema
 * Se podria usar una sola conexion y cambiar de contenedor con ALTER SESSION,
 * pero con un pool eso es fragil: la sesion se devuelve al pool con el
 * contenedor cambiado. Dos pools separados es mas simple y mas predecible.
 */
@Configuration
public class ConexionesConfig {

    @Value("${oracle.cdb.url}") private String cdbUrl;
    @Value("${oracle.pdb.url}") private String pdbUrl;
    @Value("${oracle.user}")    private String usuario;
    @Value("${oracle.password}") private String clave;

    private DataSource crear(String url) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("oracle.jdbc.OracleDriver");
        ds.setJdbcUrl(url);
        ds.setUsername(usuario);
        ds.setPassword(clave);
        ds.setMaximumPoolSize(2);          // el agente no necesita mas
        ds.setMinimumIdle(0);
        ds.setConnectionTimeout(10000);    // si la base no esta, fallar rapido
        ds.setInitializationFailTimeout(-1); // arrancar aunque la base este caida
        ds.setPoolName(url.contains("XEPDB1") ? "pdb" : "cdb");
        return ds;
    }

    @Bean(name = "cdbTemplate")
    public JdbcTemplate cdbTemplate() {
        JdbcTemplate t = new JdbcTemplate(crear(cdbUrl));
        t.setQueryTimeout(10);
        return t;
    }

    @Bean(name = "pdbTemplate")
    public JdbcTemplate pdbTemplate() {
        JdbcTemplate t = new JdbcTemplate(crear(pdbUrl));
        t.setQueryTimeout(10);
        return t;
    }
}
