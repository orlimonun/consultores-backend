package cr.una.consultores.controller;

import cr.una.consultores.dto.ReporteAgenteDTO;
import cr.una.consultores.service.AgenteMetricasService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Recibe los reportes de los agentes instalados junto a instancias que no
 * son alcanzables desde internet.
 *
 * El agente envia numeros crudos, sin veredicto: los umbrales viven en el
 * catalogo del backend. El endpoint se autentica con una clave compartida
 * porque un agente no tiene sesion de usuario.
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
            @RequestBody ReporteAgenteDTO reporte) {

        if (claveEsperada == null || claveEsperada.isBlank()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "El backend no tiene configurada la clave de agentes",
                    "detalle", "Falta la variable AGENTE_CLAVE en el servidor"));
        }
        if (clave == null || !constantes(clave, claveEsperada)) {
            return ResponseEntity.status(401).body(Map.of("error", "Clave de agente inválida"));
        }
        try {
            agentes.registrar(reporte);
            return ResponseEntity.ok(Map.of(
                    "recibido", true,
                    "instanciaId", reporte.instanciaId,
                    "variables", reporte.lecturas == null ? 0 : reporte.lecturas.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Comparación en tiempo constante: no filtra la clave por el tiempo de respuesta. */
    private boolean constantes(String a, String b) {
        if (a.length() != b.length()) return false;
        int dif = 0;
        for (int i = 0; i < a.length(); i++) dif |= a.charAt(i) ^ b.charAt(i);
        return dif == 0;
    }
}