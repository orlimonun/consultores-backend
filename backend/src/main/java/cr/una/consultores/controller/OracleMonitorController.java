package cr.una.consultores.controller;

import cr.una.consultores.dto.SaludOracleDTO;
import cr.una.consultores.service.OracleMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint del monitor Oracle real. Lee metricas en vivo en cada request.
 * Requiere login (queda protegido por SecurityConfig como el resto).
 */
@RestController
@RequestMapping("/api/monitoreo/oracle")
public class OracleMonitorController {

    private final OracleMonitorService service;

    public OracleMonitorController(OracleMonitorService service) {
        this.service = service;
    }

    // Salud actual de la instancia Oracle (lectura en vivo)
    @GetMapping("/salud")
    public ResponseEntity<?> salud() {
        if (!service.disponible()) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "La conexion a Oracle no esta configurada",
                "detalle", "Falta la variable ORACLE_WALLET_B64 en el servidor"));
        }
        try {
            SaludOracleDTO dto = service.leerSalud();
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "No se pudo leer la salud de Oracle",
                "detalle", e.getMessage()));
        }
    }

    // Ping para verificar si Oracle esta conectado
    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of("conectado", service.disponible());
    }
}
