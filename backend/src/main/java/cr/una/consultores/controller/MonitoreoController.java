package cr.una.consultores.controller;

import cr.una.consultores.dto.*;
import cr.una.consultores.entity.InstanciaMonitoreada;
import cr.una.consultores.entity.LecturaMetricas;
import cr.una.consultores.repository.InstanciaMonitoreadaRepository;
import cr.una.consultores.repository.LecturaMetricasRepository;
import cr.una.consultores.service.MonitoreoService;
import cr.una.consultores.service.SaludBDService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/monitoreo")
public class MonitoreoController {

    private final InstanciaMonitoreadaRepository instanciaRepo;
    private final LecturaMetricasRepository lecturaRepo;
    private final SaludBDService saludService;
    private final MonitoreoService monitoreoService;

    public MonitoreoController(InstanciaMonitoreadaRepository instanciaRepo,
                               LecturaMetricasRepository lecturaRepo,
                               SaludBDService saludService,
                               MonitoreoService monitoreoService) {
        this.instanciaRepo = instanciaRepo;
        this.lecturaRepo = lecturaRepo;
        this.saludService = saludService;
        this.monitoreoService = monitoreoService;
    }

    // Tablero: todas las instancias con su salud actual
    @GetMapping("/instancias")
    public List<InstanciaSaludDTO> listar() {
        List<InstanciaSaludDTO> salida = new ArrayList<>();
        for (InstanciaMonitoreada inst : instanciaRepo.findByActivoTrue()) {
            InstanciaSaludDTO dto = lecturaRepo
                    .findTopByInstanciaIdOrderByTomadaEnDesc(inst.getId())
                    .map(saludService::calcularSalud)
                    .orElseGet(InstanciaSaludDTO::new);
            dto.id = inst.getId();
            dto.nombre = inst.getNombre();
            dto.instancia = inst.getInstancia();
            dto.ubicacion = inst.getUbicacion();
            salida.add(dto);
        }
        return salida;
    }

    // Detalle de una instancia: indicadores, metricas y alertas
    @GetMapping("/instancias/{id}")
    public ResponseEntity<InstanciaDetalleDTO> detalle(@PathVariable Integer id) {
        InstanciaMonitoreada inst = instanciaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instancia no encontrada"));
        return lecturaRepo.findTopByInstanciaIdOrderByTomadaEnDesc(id)
                .map(lectura -> {
                    InstanciaDetalleDTO d = saludService.calcularDetalle(lectura);
                    d.id = inst.getId();
                    d.nombre = inst.getNombre();
                    d.instancia = inst.getInstancia();
                    d.ubicacion = inst.getUbicacion();
                    return ResponseEntity.ok(d);
                })
                .orElse(ResponseEntity.noContent().build());
    }

    // Historico de ISBD de una instancia
    @GetMapping("/instancias/{id}/historico")
    public List<PuntoHistoricoDTO> historico(@PathVariable Integer id) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        List<PuntoHistoricoDTO> puntos = new ArrayList<>();
        List<LecturaMetricas> lecturas = lecturaRepo.findByInstanciaIdOrderByTomadaEnDesc(id);
        // ultimas 12, en orden cronologico
        int desde = Math.max(0, lecturas.size() - 12);
        List<LecturaMetricas> recientes = lecturas.subList(0, Math.min(lecturas.size(), 12));
        for (int i = recientes.size() - 1; i >= 0; i--) {
            LecturaMetricas l = recientes.get(i);
            PuntoHistoricoDTO p = new PuntoHistoricoDTO();
            p.hora = l.getTomadaEn().format(fmt);
            p.isbd = saludService.calcularSalud(l).isbd;
            puntos.add(p);
        }
        return puntos;
    }

    // Crear una instancia a monitorear
    @PostMapping("/instancias")
    public InstanciaMonitoreada crear(@RequestBody InstanciaMonitoreada instancia) {
        return monitoreoService.crearInstancia(instancia);
    }

    // Registrar una lectura de metricas para una instancia
    @PostMapping("/instancias/{id}/lecturas")
    public ResponseEntity<InstanciaSaludDTO> registrarLectura(@PathVariable Integer id,
                                                              @RequestBody LecturaRequest req) {
        LecturaMetricas lectura = monitoreoService.registrarLectura(id, req);
        return ResponseEntity.ok(saludService.calcularSalud(lectura));
    }
}
