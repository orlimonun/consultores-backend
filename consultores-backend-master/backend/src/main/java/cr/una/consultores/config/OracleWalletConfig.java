package cr.una.consultores.config;

import oracle.jdbc.pool.OracleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Configura una conexion SECUNDARIA a Oracle Autonomous (para el monitor),
 * sin tocar la conexion principal de Postgres/Neon (auditoria).
 *
 * El wallet llega como texto base64 en la variable de entorno ORACLE_WALLET_B64.
 * Al arrancar, se decodifica y se reconstruyen los archivos del wallet en una
 * carpeta temporal, y se apunta la conexion a esa carpeta.
 *
 * Si ORACLE_WALLET_B64 no esta definida, este datasource no se crea y el
 * monitor simplemente no tendra Oracle (el resto de la app sigue funcionando).
 */
@Configuration
public class OracleWalletConfig {

    @Value("${oracle.wallet.b64:}")
    private String walletB64;

    @Value("${oracle.username:ADMIN}")
    private String oracleUser;

    @Value("${oracle.password:}")
    private String oraclePassword;

    // TNS name a usar (ej. monitordb_tp). Debe existir en tnsnames.ora del wallet.
    @Value("${oracle.tns:monitordb_tp}")
    private String tnsName;

    @Bean(name = "oracleJdbcTemplate")
    public JdbcTemplate oracleJdbcTemplate() throws Exception {
        DataSource ds = oracleDataSource();
        if (ds == null) return null;
        return new JdbcTemplate(ds);
    }

    private DataSource oracleDataSource() throws Exception {
        if (walletB64 == null || walletB64.isBlank()) {
            System.out.println(">> Oracle: ORACLE_WALLET_B64 no definida; monitor sin Oracle.");
            return null;
        }

        // 1) Reconstruir el wallet en una carpeta temporal
        Path walletDir = Files.createTempDirectory("oracle_wallet");
        byte[] zipBytes = Base64.getDecoder().decode(walletB64.replaceAll("\\s", ""));
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                // aplanar: usar solo el nombre del archivo (sin subcarpetas)
                String fileName = Paths.get(entry.getName()).getFileName().toString();
                Path out = walletDir.resolve(fileName);
                try (OutputStream os = Files.newOutputStream(out)) {
                    zis.transferTo(os);
                }
            }
        }

        String walletPath = walletDir.toAbsolutePath().toString();
        // Oracle necesita saber donde esta el wallet
        System.setProperty("oracle.net.tns_admin", walletPath);

        // 2) Crear el datasource apuntando al TNS del wallet
        OracleDataSource ods = new OracleDataSource();
        ods.setURL("jdbc:oracle:thin:@" + tnsName + "?TNS_ADMIN=" + walletPath);
        ods.setUser(oracleUser);
        ods.setPassword(oraclePassword);

        System.out.println(">> Oracle: wallet reconstruido en " + walletPath + ", TNS=" + tnsName);
        return ods;
    }
}
