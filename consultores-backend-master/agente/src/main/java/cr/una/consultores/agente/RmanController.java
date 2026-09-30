package cr.una.consultores.agente;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint HTTP que el backend llama para despachar un script RMAN.
 *
 * El agente solo escucha en 127.0.0.1 (configurado en application.properties).
 * Si el backend está en la misma red, puede llamarlo directamente.
 * Si está en la nube, se usa un SSH tunnel o una VPN.
 *
 * Autenticación: misma cabecera X-Agente-Clave que el endpoint de push,
 * para consistencia con el resto del sistema.
 */
@RestController
@RequestMapping("/api/rman")
public class RmanController {

    private static final Logger log = LoggerFactory.getLogger(RmanController.class);

    private final EjecutorRman ejecutor;

    @Value("${agente.clave:}")
    private String claveEsperada;

    public RmanController(EjecutorRman ejecutor) {
        this.ejecutor = ejecutor;
    }

    /**
     * Recibe un script RMAN del backend y lo ejecuta de forma asíncrona.
     * Responde inmediatamente con 202 Accepted: el resultado llegará al backend
     * via el webhook /api/respaldo/webhook/resultado cuando RMAN termine.
     */
    @PostMapping("/ejecutar")
    public ResponseEntity<?> ejecutar(
            @RequestHeader(value = "X-Agente-Clave", required = false) String clave,
            @RequestBody ComandoRman cmd) {

        if (claveEsperada == null || claveEsperada.isBlank()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Agente no configurado: falta AGENTE_CLAVE"));
        }
        if (clave == null || !tiempoConstante(clave, claveEsperada)) {
            return ResponseEntity.status(401).body(Map.of("error", "Clave de agente inválida"));
        }
        if (cmd.ejecucionId == null || cmd.scriptRman == null || cmd.scriptRman.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Se requieren ejecucionId y scriptRman"));
        }

        log.info("Recibido comando RMAN para ejecucionId={} estrategia='{}'",
                 cmd.ejecucionId, cmd.descripcion);

        ejecutor.ejecutarAsync(cmd);

        return ResponseEntity.accepted().body(Map.of(
            "recibido", true,
            "ejecucionId", cmd.ejecucionId,
            "mensaje", "Script RMAN aceptado. El resultado se reportará al backend al terminar."
        ));
    }

    /** Estado del agente: confirma que está vivo y listo para recibir comandos. */
    @GetMapping("/estado")
    public ResponseEntity<?> estado() {
        return ResponseEntity.ok(Map.of(
            "activo", true,
            "mensaje", "Agente RMAN listo"
        ));
    }

    /** Comparación en tiempo constante para no filtrar la clave por timing. */
    private boolean tiempoConstante(String a, String b) {
        if (a.length() != b.length()) return false;
        int dif = 0;
        for (int i = 0; i < a.length(); i++) dif |= a.charAt(i) ^ b.charAt(i);
        return dif == 0;
    }
}
