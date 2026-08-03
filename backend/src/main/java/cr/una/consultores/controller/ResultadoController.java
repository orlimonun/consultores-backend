package cr.una.consultores.controller;

import cr.una.consultores.dto.ResultadoAuditoriaDTO;
import cr.una.consultores.service.CalculoRiesgoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resultados")
public class ResultadoController {

    private final CalculoRiesgoService calculoRiesgoService;

    public ResultadoController(CalculoRiesgoService calculoRiesgoService) {
        this.calculoRiesgoService = calculoRiesgoService;
    }

    @GetMapping("/auditoria/{auditoriaId}")
    public ResultadoAuditoriaDTO calcular(@PathVariable Integer auditoriaId) {
        return calculoRiesgoService.calcular(auditoriaId);
    }
}
