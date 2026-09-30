package cr.una.consultores.controller;

import cr.una.consultores.dto.SaludInstanciaDTO;
import cr.una.consultores.monitor.CatalogoVariables;
import cr.una.consultores.monitor.VariableDef;
import cr.una.consultores.service.AgenteMetricasService;
import cr.una.consultores.service.EvaluadorSaludService;
import cr.una.consultores.service.OracleMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/monitoreo/oracle")
public class OracleMonitorController {

    private final OracleMonitorService service;
    private final AgenteMetricasService agentes;
    private final CatalogoVariables catalogo;

    public OracleMonitorController(OracleMonitorService service,
                                   AgenteMetricasService agentes,
                                   CatalogoVariables catalogo) {
        this.service = service;
        this.agentes = agentes;
        this.catalogo = catalogo;
    }

    /** Todas las instancias vigiladas, con su arbol de tres niveles evaluado. */
    @GetMapping("/instancias")
    public List<SaludInstanciaDTO> instancias() {
        List<SaludInstanciaDTO> todas = new ArrayList<>();
        if (service.disponible()) {
            try {
                todas.add(service.leerSalud());
            } catch (Exception e) {
                // Una fuente caida no interrumpe la respuesta: esa instancia
                // se reporta caida y las demas siguen visibles.
                todas.add(service.instanciaCaida(e.getMessage()));
            }
        }
        todas.addAll(agentes.listar());
        return todas;
    }

    /** Compatibilidad con la pantalla anterior: solo la Autonomous. */
    @GetMapping("/salud")
    public ResponseEntity<?> salud() {
        if (!service.disponible()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "La conexión a Oracle no está configurada",
                    "detalle", "Falta la variable ORACLE_WALLET_B64 en el servidor"));
        }
        try {
            return ResponseEntity.ok(service.leerSalud());
        } catch (Exception e) {
            return ResponseEntity.ok(service.instanciaCaida(e.getMessage()));
        }
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of(
                "conectado", service.disponible(),
                "instancias", (service.disponible() ? 1 : 0) + agentes.cantidad(),
                "formula", EvaluadorSaludService.FORMULA);
    }

    /**
     * El catalogo completo de variables con sus umbrales y su origen.
     *
     * Existe porque la revision pidio que los rangos sean DEMOSTRABLES:
     * no basta con que el monitor pinte de verde, hay que poder mostrar
     * contra que se comparo y de donde salio ese limite.
     */
    @GetMapping("/catalogo")
    public Map<String, Object> catalogo() {
        List<Map<String, Object>> vars = new ArrayList<>();
        for (VariableDef d : catalogo.todas()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", d.codigo);
            m.put("indicador", d.indicador);
            m.put("grupo", d.grupo);
            m.put("nombre", d.nombre);
            m.put("unidad", d.unidad);
            m.put("consulta", d.consulta);
            m.put("direccion", d.direccion.name());
            m.put("umbralAdvertencia", d.umbralAdvertencia);
            m.put("umbralCritico", d.umbralCritico);
            m.put("limiteInferior", d.limiteInferior());
            m.put("limiteSuperior", d.limiteSuperior());
            m.put("fuenteUmbral", d.fuente.name());
            m.put("velocidad", d.velocidad);
            m.put("alcance", d.alcance);
            m.put("pesoBruto", d.pesoBruto());
            m.put("peso", catalogo.peso(d.codigo));
            m.put("ayuda", d.ayuda);
            m.put("remediacion", d.remediacion);
            vars.add(m);
        }
        return Map.of(
                "formula", EvaluadorSaludService.FORMULA,
                "pesosIndicador", CatalogoVariables.PESO_INDICADOR,
                "nombresGrupo", CatalogoVariables.NOMBRE_GRUPO,
                "metodoPeso", "El peso de cada variable sale de velocidad de degradación × alcance, " +
                        "normalizado dentro de su indicador. No se asigna a mano.",
                "variables", vars);
    }

    /** Ajuste de umbrales en caliente, para la pantalla de configuración. */
    @PatchMapping("/catalogo/{codigo}")
    public ResponseEntity<?> ajustar(@PathVariable String codigo,
                                     @RequestBody Map<String, Double> cuerpo) {
        boolean ok = catalogo.ajustar(codigo,
                cuerpo.get("umbralAdvertencia"), cuerpo.get("umbralCritico"));
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("codigo", codigo, "actualizado", true,
                "nota", "El cambio aplica de inmediato sobre los reportes ya recibidos."));
    }
}