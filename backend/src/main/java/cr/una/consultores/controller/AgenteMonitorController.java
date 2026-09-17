package cr.una.consultores.controller;

import cr.una.consultores.dto.SaludOracleDTO;
import cr.una.consultores.service.AgenteMetricasService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Recibe las métricas que envían los agentes instalados junto a instancias
 * Oracle que no son alcanzables desde internet.
 *
 * POR QUE EL AGENTE EMPUJA EN VEZ DE QUE EL BACKEND CONSULTE
 * Una instancia local vive detrás de un router con NAT: el backend en Render
 * no puede iniciar una conexión hacia ella. El agente sí puede salir hacia
 * internet, porque esa dirección todos los firewalls la permiten. Es el mismo
 * modelo que usan Datadog, Zabbix y Prometheus.
 *
 * AUTENTICACION
 * El agente no tiene sesión ni JWT, así que este endpoint se autentica con una
 * clave compartida en la cabecera X-Agente-Clave. Hay que abrirlo en
 * SecurityConfig, de modo que esa clave es lo único que lo protege: debe ser
 * larga, aleatoria y vivir en una variable de entorno.
 */
@RestController
@RequestMapping("/api/monitoreo/agente")
public class AgenteMonitorController {

    private final AgenteMetricasService agentes;

    @Value("${agente.clave:}")
    private String claveEsperada;

    public AgenteMonitorController(AgenteMetricasService agentes) {
        this.agentes = agentes;
    }

    @PostMapping("/push")
    public ResponseEntity<?> push(
            @RequestHeader(value = "X-Agente-Clave", required = false) String clave,
            @RequestBody SaludOracleDTO reporte) {

        if (claveEsperada == null || claveEsperada.isBlank()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "El backend no tiene configurada la clave de agentes",
                    "detalle", "Falta la variable AGENTE_CLAVE en el servidor"));
        }
        if (clave == null || !constantes(clave, claveEsperada)) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Clave de agente invalida"));
        }
        try {
            agentes.registrar(reporte);
            return ResponseEntity.ok(Map.of(
                    "recibido", true,
                    "instanciaId", reporte.instanciaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Comparación en tiempo constante: no revela la clave por el tiempo de respuesta. */
    private boolean constantes(String a, String b) {
        if (a.length() != b.length()) return false;
        int dif = 0;
        for (int i = 0; i < a.length(); i++) dif |= a.charAt(i) ^ b.charAt(i);
        return dif == 0;
    }
}