package cr.una.consultores.controller;

import cr.una.consultores.dto.SaludOracleDTO;
import cr.una.consultores.service.AgenteMetricasService;
import cr.una.consultores.service.OracleMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Endpoints del monitor Oracle. Requieren login, como el resto de la app.
 *
 * /salud      una sola instancia: la Autonomous leida en vivo. Se conserva
 *             porque el frontend actual lo consume.
 * /instancias todas las instancias vigiladas: la Autonomous mas las que
 *             reporten agentes. Es el endpoint nuevo.
 */
@RestController
@RequestMapping("/api/monitoreo/oracle")
public class OracleMonitorController {

    private final OracleMonitorService service;
    private final AgenteMetricasService agentes;

    public OracleMonitorController(OracleMonitorService service,
                                   AgenteMetricasService agentes) {
        this.service = service;
        this.agentes = agentes;
    }

    // ---- Compatibilidad: la Autonomous, tal como antes ----
    @GetMapping("/salud")
    public ResponseEntity<?> salud() {
        if (!service.disponible()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "La conexion a Oracle no esta configurada",
                    "detalle", "Falta la variable ORACLE_WALLET_B64 en el servidor"));
        }
        try {
            return ResponseEntity.ok(service.leerSalud());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "No se pudo leer la salud de Oracle",
                    "detalle", e.getMessage()));
        }
    }

    // ---- Todas las instancias vigiladas ----
    @GetMapping("/instancias")
    public List<SaludOracleDTO> instancias() {
        List<SaludOracleDTO> todas = new ArrayList<>();

        // 1. La Autonomous, leida en vivo. Si falla, se reporta caida en vez
        //    de romper la respuesta completa: el resto de instancias sigue
        //    siendo visible.
        if (service.disponible()) {
            try {
                todas.add(service.leerSalud());
            } catch (Exception e) {
                todas.add(service.instanciaCaida(e.getMessage()));
            }
        }

        // 2. Las que reportan agentes
        todas.addAll(agentes.listar());
        return todas;
    }

    // ---- Ping de la conexion directa ----
    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of(
                "conectado", service.disponible(),
                "instancias", (service.disponible() ? 1 : 0) + agentes.cantidad());
    }
}