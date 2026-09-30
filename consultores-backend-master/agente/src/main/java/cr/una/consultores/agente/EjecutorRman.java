package cr.una.consultores.agente;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Ejecuta un script RMAN en el servidor Oracle local.
 *
 * Flujo:
 *   1. Escribe el script en un archivo temporal (.rman)
 *   2. Invoca: rman target / cmdfile <archivo>
 *   3. Captura stdout + stderr
 *   4. Determina el resultado según los patrones de salida de RMAN
 *   5. Reporta el resultado al backend via HTTP POST
 *   6. Elimina el archivo temporal
 *
 * Detección de resultado:
 *   RMAN imprime "RMAN-" para errores y "ORA-" para errores de Oracle.
 *   Si hay alguno de esos patrones, se marca FALLIDO.
 *   Si hay "WARNING" se marca CON_ADVERTENCIAS.
 *   Si el proceso termina con código 0 y sin errores: EXITOSO.
 */
@Service
public class EjecutorRman {

    private static final Logger log = LoggerFactory.getLogger(EjecutorRman.class);

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final LectorEspacioDisco lectorDisco;

    @Value("${rman.ejecutable:/u01/app/oracle/product/19.0.0/dbhome_1/bin/rman}")
    private String rmanEjecutable;

    @Value("${rman.tmp-dir:/tmp/rman_scripts}")
    private String tmpDir;

    @Value("${agente.backend-url:https://consultores-backend.onrender.com/api/monitoreo/agente/push}")
    private String backendBaseUrl;

    @Value("${agente.clave:}")
    private String agenteClave;

    public EjecutorRman(LectorEspacioDisco lectorDisco) {
        this.lectorDisco = lectorDisco;
    }

    /**
     * Ejecuta el comando de forma asíncrona (en un thread separado) para no
     * bloquear el endpoint HTTP que lo recibió.
     */
    public void ejecutarAsync(ComandoRman cmd) {
        Thread.ofVirtual().name("rman-" + cmd.ejecucionId).start(() -> ejecutar(cmd));
    }

    private void ejecutar(ComandoRman cmd) {
        log.info("Iniciando ejecución RMAN para ejecucionId={} estrategia='{}'",
                 cmd.ejecucionId, cmd.descripcion);
        Instant inicio = Instant.now();
        ResultadoRman resultado = new ResultadoRman();
        resultado.ejecucionId = cmd.ejecucionId;

        Path scriptPath = null;
        try {
            // 1. Crear directorio temporal si no existe
            Files.createDirectories(Path.of(tmpDir));

            // 2. Escribir el script en un archivo temporal
            scriptPath = Path.of(tmpDir, "rman_" + cmd.ejecucionId + "_" + System.currentTimeMillis() + ".rman");
            Files.writeString(scriptPath, cmd.scriptRman);
            log.info("Script escrito en {}", scriptPath);

            // 3. Ejecutar RMAN
            ProcessBuilder pb = new ProcessBuilder(
                    rmanEjecutable, "target", "/",
                    "cmdfile", scriptPath.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true); // combinar stdout y stderr
            pb.environment().put("ORACLE_SID", System.getenv().getOrDefault("ORACLE_SID", "XE"));
            pb.environment().put("ORACLE_HOME", System.getenv().getOrDefault("ORACLE_HOME",
                    rmanEjecutable.replace("/bin/rman", "")));

            Process proceso = pb.start();

            // 4. Leer la salida completa
            StringBuilder salida = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    salida.append(linea).append("\n");
                }
            }

            // Esperar máximo 2 horas
            boolean termino = proceso.waitFor(120, java.util.concurrent.TimeUnit.MINUTES);
            if (!termino) {
                proceso.destroyForcibly();
                resultado.resultado = "FALLIDO";
                resultado.mensajeError = "La ejecución RMAN excedió el tiempo máximo de 2 horas.";
                resultado.salidaRman = salida.toString();
            } else {
                int exitCode = proceso.exitValue();
                String salidaStr = salida.toString();
                resultado.salidaRman = salidaStr;
                resultado.resultado = determinarResultado(exitCode, salidaStr);
                log.info("RMAN terminó con código {} → {}", exitCode, resultado.resultado);
            }

        } catch (Exception e) {
            log.error("Error ejecutando RMAN: {}", e.getMessage(), e);
            resultado.resultado = "FALLIDO";
            resultado.mensajeError = e.getMessage();
        } finally {
            // 5. Eliminar el archivo temporal
            if (scriptPath != null) {
                try { Files.deleteIfExists(scriptPath); } catch (Exception ignored) {}
            }
        }

        resultado.duracionSegundos = Duration.between(inicio, Instant.now()).getSeconds();

        // 6. Capturar espacio en disco al terminar (para la alerta SIN_ESPACIO)
        try {
            LectorEspacioDisco.InfoDisco disco = lectorDisco.peorParticion();
            if (disco != null) {
                resultado.discoUsoPct    = disco.usoPct;
                resultado.discoLibreMb   = disco.libreMb;
                resultado.discoMountPoint = disco.mountPoint;
            }
        } catch (Exception ignored) {}

        // 7. Reportar al backend
        reportarAlBackend(resultado);
    }

    /**
     * Determina el resultado basándose en el código de salida y la salida de RMAN.
     * RMAN devuelve 0 en éxito, 1 en advertencias y >1 en errores.
     * Además busca patrones en la salida como segunda verificación.
     */
    private String determinarResultado(int exitCode, String salida) {
        boolean tieneError = salida.contains("RMAN-") || salida.contains("ORA-")
                || salida.contains("error") || exitCode > 1;
        boolean tieneWarning = salida.contains("WARNING") || salida.contains("WARN")
                || exitCode == 1;

        if (tieneError) return "FALLIDO";
        if (tieneWarning) return "CON_ADVERTENCIAS";
        return "EXITOSO";
    }

    private void reportarAlBackend(ResultadoRman resultado) {
        // La URL del webhook de respaldo: reemplaza la ruta del agente de monitoreo
        String webhookUrl = backendBaseUrl
                .replace("/api/monitoreo/agente/push", "/api/respaldo/webhook/resultado");

        try {
            String cuerpo = json.writeValueAsString(resultado);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-Agente-Clave", agenteClave)
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                log.info("Resultado de ejecución {} reportado al backend: {}",
                         resultado.ejecucionId, resultado.resultado);
            } else {
                log.error("El backend respondió {} al recibir el resultado: {}",
                          res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.error("No se pudo reportar el resultado al backend: {}", e.getMessage());
        }
    }
}
