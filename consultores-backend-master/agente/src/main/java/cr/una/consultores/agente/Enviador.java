package cr.una.consultores.agente;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Lee la base local y envia el reporte al backend, una vez por intervalo.
 *
 * DOS FALLOS DISTINTOS, DOS SENALES DISTINTAS
 *   - Si no se puede leer Oracle pero si alcanzar el backend, se envia un
 *     reporte marcando la instancia como caida. El tablero muestra "Oracle
 *     local inaccesible".
 *   - Si no se puede alcanzar el backend, no se envia nada. El backend deja
 *     de recibir latidos y a los tres minutos marca la instancia sin contacto.
 *
 * La diferencia importa: en el primer caso el agente vive y la base murio;
 * en el segundo el que fallo fue el agente o la red.
 */
@Component
public class Enviador {

    private static final Logger log = LoggerFactory.getLogger(Enviador.class);

    private final LectorOracle lector;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${agente.backend-url}") private String url;
    @Value("${agente.clave}")       private String clave;

    public Enviador(LectorOracle lector) {
        this.lector = lector;
    }

    @Scheduled(fixedDelayString = "${agente.intervalo-ms:60000}", initialDelay = 3000)
    public void ciclo() {
        if (clave == null || clave.isBlank()) {
            log.error("Falta la clave del agente. Definir AGENTE_CLAVE antes de arrancar.");
            return;
        }

        ReporteSalud reporte;
        try {
            reporte = lector.leer();
            log.info("Lectura OK  ISBD={}  IP={} IM={} IA={} IR={}",
                     reporte.isbd, reporte.ip, reporte.im, reporte.ia, reporte.ir);
        } catch (Exception e) {
            log.warn("No se pudo leer la base local: {}", e.getMessage());
            reporte = lector.caida(e.getMessage());
        }

        try {
            String cuerpo = json.writeValueAsString(reporte);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-Agente-Clave", clave)
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                log.info("Reporte enviado ({})", reporte.instanciaId);
            } else if (res.statusCode() == 401) {
                log.error("Clave de agente rechazada por el backend. Revisar AGENTE_CLAVE.");
            } else if (res.statusCode() == 503) {
                log.error("El backend no tiene configurada la clave de agentes.");
            } else {
                log.error("El backend respondio {}: {}", res.statusCode(), res.body());
            }
        } catch (Exception e) {
            // Sin conexion al backend: no hay nada que hacer salvo reintentar
            // en el proximo ciclo. El backend lo notara por la falta de latido.
            log.warn("No se pudo enviar el reporte: {}", e.getMessage());
        }
    }
}
