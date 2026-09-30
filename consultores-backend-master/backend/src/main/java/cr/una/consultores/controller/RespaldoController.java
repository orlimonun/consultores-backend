package cr.una.consultores.controller;

import cr.una.consultores.dto.*;
import cr.una.consultores.entity.*;
import cr.una.consultores.repository.*;
import cr.una.consultores.service.AlertaRespaldoService;
import cr.una.consultores.service.RmanScriptBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador principal del módulo de respaldo.
 * Expone los endpoints bajo /api/respaldo/*.
 *
 * Rutas:
 *   GET    /api/respaldo/bases                     → listar instancias Oracle
 *   POST   /api/respaldo/bases                     → registrar instancia
 *   PUT    /api/respaldo/bases/{id}                → actualizar instancia
 *   DELETE /api/respaldo/bases/{id}                → eliminar instancia
 *
 *   GET    /api/respaldo/estrategias               → listar estrategias
 *   GET    /api/respaldo/estrategias/{id}          → detalle estrategia
 *   POST   /api/respaldo/estrategias               → crear estrategia
 *   PUT    /api/respaldo/estrategias/{id}          → actualizar estrategia
 *   DELETE /api/respaldo/estrategias/{id}          → eliminar estrategia
 *   POST   /api/respaldo/estrategias/{id}/generar-script → generar/regenerar script
 *   POST   /api/respaldo/estrategias/{id}/ejecutar → lanzar manualmente
 *
 *   GET    /api/respaldo/programaciones            → listar programaciones
 *   POST   /api/respaldo/programaciones            → crear programación
 *   PUT    /api/respaldo/programaciones/{id}       → actualizar programación
 *   DELETE /api/respaldo/programaciones/{id}       → eliminar programación
 *
 *   GET    /api/respaldo/ejecuciones               → historial global
 *   GET    /api/respaldo/ejecuciones/{id}          → detalle con script
 *   GET    /api/respaldo/ejecuciones/estrategia/{id} → historial por estrategia
 *
 *   GET    /api/respaldo/alertas                   → panel de control preventivo
 *
 *   POST   /api/respaldo/webhook/resultado         → recibe resultado del agente RMAN
 */
@RestController
@RequestMapping("/api/respaldo")
public class RespaldoController {

    private final BaseDatosRespaldoRepository basesRepo;
    private final EstrategiaRespaldoRepository estrategiasRepo;
    private final ProgramacionRespaldoRepository programacionesRepo;
    private final EjecucionRespaldoRepository ejecucionesRepo;
    private final UsuarioRepository usuariosRepo;
    private final RmanScriptBuilder scriptBuilder;
    private final AlertaRespaldoService alertaService;

    public RespaldoController(BaseDatosRespaldoRepository basesRepo,
                               EstrategiaRespaldoRepository estrategiasRepo,
                               ProgramacionRespaldoRepository programacionesRepo,
                               EjecucionRespaldoRepository ejecucionesRepo,
                               UsuarioRepository usuariosRepo,
                               RmanScriptBuilder scriptBuilder,
                               AlertaRespaldoService alertaService) {
        this.basesRepo = basesRepo;
        this.estrategiasRepo = estrategiasRepo;
        this.programacionesRepo = programacionesRepo;
        this.ejecucionesRepo = ejecucionesRepo;
        this.usuariosRepo = usuariosRepo;
        this.scriptBuilder = scriptBuilder;
        this.alertaService = alertaService;
    }

    // ════════════════════════════════════════════════════════════════════════
    // BASES DE DATOS
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/bases")
    public List<BaseDatosRespaldoDTO> listarBases() {
        return basesRepo.findAll().stream().map(this::toBasesDTO).collect(Collectors.toList());
    }

    @PostMapping("/bases")
    public BaseDatosRespaldoDTO crearBase(@RequestBody BaseDatosRespaldoDTO req) {
        BaseDatosRespaldo b = new BaseDatosRespaldo();
        aplicarBase(b, req);
        return toBasesDTO(basesRepo.save(b));
    }

    @PutMapping("/bases/{id}")
    public ResponseEntity<BaseDatosRespaldoDTO> actualizarBase(@PathVariable Integer id,
                                                                @RequestBody BaseDatosRespaldoDTO req) {
        return basesRepo.findById(id).map(b -> {
            aplicarBase(b, req);
            return ResponseEntity.ok(toBasesDTO(basesRepo.save(b)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/bases/{id}")
    public ResponseEntity<Void> eliminarBase(@PathVariable Integer id) {
        if (!basesRepo.existsById(id)) return ResponseEntity.notFound().build();
        basesRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // ESTRATEGIAS
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/estrategias")
    public List<EstrategiaRespaldoDTO> listarEstrategias(
            @RequestParam(required = false) Integer baseDatosId) {
        List<EstrategiaRespaldo> lista = baseDatosId != null
                ? estrategiasRepo.findByBaseDatosId(baseDatosId)
                : estrategiasRepo.findAll();
        return lista.stream().map(this::toEstrategiaDTO).collect(Collectors.toList());
    }

    @GetMapping("/estrategias/{id}")
    public ResponseEntity<EstrategiaRespaldoDTO> obtenerEstrategia(@PathVariable Integer id) {
        return estrategiasRepo.findById(id)
                .map(e -> ResponseEntity.ok(toEstrategiaDTO(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/estrategias")
    public ResponseEntity<EstrategiaRespaldoDTO> crearEstrategia(
            @RequestBody EstrategiaRespaldoRequest req) {
        BaseDatosRespaldo bd = basesRepo.findById(req.baseDatosId).orElse(null);
        if (bd == null) return ResponseEntity.badRequest().build();

        EstrategiaRespaldo e = new EstrategiaRespaldo();
        aplicarEstrategia(e, req, bd);
        return ResponseEntity.ok(toEstrategiaDTO(estrategiasRepo.save(e)));
    }

    @PutMapping("/estrategias/{id}")
    public ResponseEntity<EstrategiaRespaldoDTO> actualizarEstrategia(
            @PathVariable Integer id, @RequestBody EstrategiaRespaldoRequest req) {
        return estrategiasRepo.findById(id).map(e -> {
            BaseDatosRespaldo bd = basesRepo.findById(req.baseDatosId).orElse(e.getBaseDatos());
            aplicarEstrategia(e, req, bd);
            e.setActualizadaEn(LocalDateTime.now());
            // Si la estrategia cambia, el script anterior ya no es válido
            e.setScriptRman(null);
            e.setScriptGeneradoEn(null);
            return ResponseEntity.ok(toEstrategiaDTO(estrategiasRepo.save(e)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/estrategias/{id}")
    public ResponseEntity<Void> eliminarEstrategia(@PathVariable Integer id) {
        if (!estrategiasRepo.existsById(id)) return ResponseEntity.notFound().build();
        estrategiasRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Genera (o regenera) el script RMAN para la estrategia y lo persiste.
     * Cambia el estado a PENDIENTE_APROBACION para que el admin lo revise.
     */
    @PostMapping("/estrategias/{id}/generar-script")
    public ResponseEntity<EstrategiaRespaldoDTO> generarScript(@PathVariable Integer id) {
        return estrategiasRepo.findById(id).map(e -> {
            String script = scriptBuilder.construir(e);
            e.setScriptRman(script);
            e.setScriptGeneradoEn(LocalDateTime.now());
            if ("BORRADOR".equals(e.getEstado())) {
                e.setEstado("PENDIENTE_APROBACION");
            }
            return ResponseEntity.ok(toEstrategiaDTO(estrategiasRepo.save(e)));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lanza una ejecución manual de la estrategia.
     * Crea el registro de ejecución en estado EN_PROGRESO y devuelve su ID
     * para que el frontend pueda hacer polling o el agente lo use en el webhook.
     */
    @PostMapping("/estrategias/{id}/ejecutar")
    public ResponseEntity<?> ejecutarManual(@PathVariable Integer id) {
        return estrategiasRepo.findById(id).map(e -> {
            if (e.getScriptRman() == null || e.getScriptRman().isBlank()) {
                return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "La estrategia no tiene script generado. " +
                        "Genere y apruebe el script antes de ejecutar."));
            }
            EjecucionRespaldo ej = new EjecucionRespaldo();
            ej.setEstrategia(e);
            ej.setScriptEjecutado(e.getScriptRman());
            ej.setTipoRespaldo(e.getTipoRespaldo());
            ej.setBaseDatosNombre(e.getBaseDatos().getNombre());
            ej.setAgenteId(e.getBaseDatos().getAgenteId());
            ej.setOrigen("MANUAL");
            ej.setResultado("EN_PROGRESO");
            EjecucionRespaldo guardada = ejecucionesRepo.save(ej);
            return ResponseEntity.ok(java.util.Map.of(
                "ejecucionId", guardada.getId(),
                "mensaje", "Ejecución registrada. Envíe el script al agente RMAN.",
                "scriptRman", e.getScriptRman(),
                "agenteId", e.getBaseDatos().getAgenteId() != null
                    ? e.getBaseDatos().getAgenteId() : "no configurado"
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // PROGRAMACIONES
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/programaciones")
    public List<ProgramacionRespaldoDTO> listarProgramaciones(
            @RequestParam(required = false) Integer estrategiaId) {
        List<ProgramacionRespaldo> lista = estrategiaId != null
                ? programacionesRepo.findByEstrategiaId(estrategiaId)
                : programacionesRepo.findAll();
        return lista.stream().map(this::toProgramacionDTO).collect(Collectors.toList());
    }

    @PostMapping("/programaciones")
    public ResponseEntity<ProgramacionRespaldoDTO> crearProgramacion(
            @RequestBody ProgramacionRespaldoRequest req) {
        EstrategiaRespaldo e = estrategiasRepo.findById(req.estrategiaId).orElse(null);
        if (e == null) return ResponseEntity.badRequest().build();

        ProgramacionRespaldo p = new ProgramacionRespaldo();
        p.setEstrategia(e);
        p.setCronExpresion(req.cronExpresion);
        p.setDescripcionCron(req.descripcionCron);
        p.setFechaInicio(req.fechaInicio != null ? req.fechaInicio : LocalDateTime.now());
        p.setFechaFin(req.fechaFin);
        p.setVentanaMinutos(req.ventanaMinutos);
        p.setMecanismo(req.mecanismo != null ? req.mecanismo : "AGENTE_RMAN");
        p.setActiva(true);

        // Activar la estrategia automáticamente al programarla
        if ("PENDIENTE_APROBACION".equals(e.getEstado()) || "BORRADOR".equals(e.getEstado())) {
            e.setEstado("ACTIVA");
            estrategiasRepo.save(e);
        }

        return ResponseEntity.ok(toProgramacionDTO(programacionesRepo.save(p)));
    }

    @PutMapping("/programaciones/{id}")
    public ResponseEntity<ProgramacionRespaldoDTO> actualizarProgramacion(
            @PathVariable Integer id, @RequestBody ProgramacionRespaldoRequest req) {
        return programacionesRepo.findById(id).map(p -> {
            if (req.cronExpresion != null) p.setCronExpresion(req.cronExpresion);
            if (req.descripcionCron != null) p.setDescripcionCron(req.descripcionCron);
            if (req.fechaInicio != null) p.setFechaInicio(req.fechaInicio);
            p.setFechaFin(req.fechaFin);
            if (req.ventanaMinutos != null) p.setVentanaMinutos(req.ventanaMinutos);
            if (req.mecanismo != null) p.setMecanismo(req.mecanismo);
            return ResponseEntity.ok(toProgramacionDTO(programacionesRepo.save(p)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/programaciones/{id}")
    public ResponseEntity<Void> eliminarProgramacion(@PathVariable Integer id) {
        if (!programacionesRepo.existsById(id)) return ResponseEntity.notFound().build();
        programacionesRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // EJECUCIONES / HISTORIAL
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/ejecuciones")
    public List<EjecucionRespaldoDTO> historialGlobal() {
        return ejecucionesRepo.findTop50ByOrderByInicioDesc()
                .stream().map(this::toEjecucionDTO).collect(Collectors.toList());
    }

    @GetMapping("/ejecuciones/{id}")
    public ResponseEntity<EjecucionRespaldoDTO> detalleEjecucion(@PathVariable Integer id) {
        return ejecucionesRepo.findById(id).map(ej -> {
            EjecucionRespaldoDTO dto = toEjecucionDTO(ej);
            // En el endpoint de detalle sí se incluye el script completo
            // (se reutiliza el mismo DTO, el campo tieneScript = true indica que hay contenido)
            return ResponseEntity.ok(dto);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ejecuciones/estrategia/{estrategiaId}")
    public List<EjecucionRespaldoDTO> historialEstrategia(@PathVariable Integer estrategiaId) {
        return ejecucionesRepo.findByEstrategiaIdOrderByInicioDesc(estrategiaId)
                .stream().map(this::toEjecucionDTO).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════════════
    // ALERTAS
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/alertas")
    public List<AlertaRespaldoDTO> alertas() {
        return alertaService.evaluar();
    }

    // ════════════════════════════════════════════════════════════════════════
    // WEBHOOK DEL AGENTE RMAN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * El agente RMAN llama a este endpoint cuando termina la ejecución.
     * Autenticado con la misma cabecera X-Agente-Clave del agente de monitoreo.
     */
    @PostMapping("/webhook/resultado")
    public ResponseEntity<?> recibirResultado(
            @RequestHeader(value = "X-Agente-Clave", required = false) String clave,
            @RequestBody ResultadoRmanDTO resultado) {

        return ejecucionesRepo.findById(resultado.ejecucionId).map(ej -> {
            ej.setResultado(resultado.resultado != null ? resultado.resultado : "FALLIDO");
            ej.setSalidaRman(resultado.salidaRman);
            ej.setMensajeError(resultado.mensajeError);
            ej.setUbicacionRespaldo(resultado.ubicacionRespaldo);
            ej.setFin(LocalDateTime.now());
            if (resultado.duracionSegundos != null) {
                ej.setDuracionSegundos(resultado.duracionSegundos);
            } else {
                long seg = java.time.Duration.between(ej.getInicio(), ej.getFin()).getSeconds();
                ej.setDuracionSegundos(seg);
            }
            // Guardar datos de disco reportados por el agente
            if (resultado.discoUsoPct != null)    ej.setDiscoUsoPct(resultado.discoUsoPct);
            if (resultado.discoLibreMb != null)   ej.setDiscoLibreMb(resultado.discoLibreMb);
            if (resultado.discoMountPoint != null) ej.setDiscoMountPoint(resultado.discoMountPoint);
            ejecucionesRepo.save(ej);
            return ResponseEntity.ok(java.util.Map.of(
                "recibido", true, "ejecucionId", ej.getId(), "resultado", ej.getResultado()));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ════════════════════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════════════════════

    private BaseDatosRespaldoDTO toBasesDTO(BaseDatosRespaldo b) {
        BaseDatosRespaldoDTO d = new BaseDatosRespaldoDTO();
        d.id = b.getId();
        d.nombre = b.getNombre();
        d.host = b.getHost();
        d.puerto = b.getPuerto();
        d.servicio = b.getServicio();
        d.modoArchivado = b.getModoArchivado();
        d.descripcion = b.getDescripcion();
        d.agenteId = b.getAgenteId();
        d.activa = b.getActiva();
        if (b.getResponsable() != null) {
            d.responsableId = b.getResponsable().getId();
            d.responsableNombre = b.getResponsable().getNombre();
        }
        return d;
    }

    private EstrategiaRespaldoDTO toEstrategiaDTO(EstrategiaRespaldo e) {
        EstrategiaRespaldoDTO d = new EstrategiaRespaldoDTO();
        d.id = e.getId();
        d.nombre = e.getNombre();
        d.descripcion = e.getDescripcion();
        d.baseDatosId = e.getBaseDatos().getId();
        d.baseDatosNombre = e.getBaseDatos().getNombre();
        d.modoArchivado = e.getBaseDatos().getModoArchivado();
        d.prioridad = e.getPrioridad();
        d.estado = e.getEstado();
        d.incluirDatabase = e.getIncluirDatabase();
        d.tablespaces = e.getTablespaces();
        d.datafiles = e.getDatafiles();
        d.incluirControlFile = e.getIncluirControlFile();
        d.incluirSpfile = e.getIncluirSpfile();
        d.incluirArchivedLogs = e.getIncluirArchivedLogs();
        d.tipoRespaldo = e.getTipoRespaldo();
        d.compresion = e.getCompresion();
        d.algoritmoCompresion = e.getAlgoritmoCompresion();
        d.deleteArchivedLogs = e.getDeleteArchivedLogs();
        d.canales = e.getCanales();
        d.destinoRuta = e.getDestinoRuta();
        d.destinoDispositivo = e.getDestinoDispositivo();
        d.formatoBackupset = e.getFormatoBackupset();
        d.scriptRman = e.getScriptRman();
        d.scriptGeneradoEn = e.getScriptGeneradoEn();
        d.notas = e.getNotas();
        d.creadaEn = e.getCreadaEn();
        d.actualizadaEn = e.getActualizadaEn();
        if (e.getResponsable() != null) {
            d.responsableId = e.getResponsable().getId();
            d.responsableNombre = e.getResponsable().getNombre();
        }
        // Advertencias actuales calculadas en caliente
        d.advertencias = scriptBuilder.advertencias(e);
        // Última ejecución
        List<EjecucionRespaldo> hist = ejecucionesRepo.findByEstrategiaIdOrderByInicioDesc(e.getId());
        if (!hist.isEmpty()) {
            d.ultimoResultado = hist.get(0).getResultado();
            d.ultimaEjecucion = hist.get(0).getInicio();
        }
        return d;
    }

    private ProgramacionRespaldoDTO toProgramacionDTO(ProgramacionRespaldo p) {
        ProgramacionRespaldoDTO d = new ProgramacionRespaldoDTO();
        d.id = p.getId();
        d.estrategiaId = p.getEstrategia().getId();
        d.estrategiaNombre = p.getEstrategia().getNombre();
        d.cronExpresion = p.getCronExpresion();
        d.descripcionCron = p.getDescripcionCron();
        d.fechaInicio = p.getFechaInicio();
        d.fechaFin = p.getFechaFin();
        d.ventanaMinutos = p.getVentanaMinutos();
        d.mecanismo = p.getMecanismo();
        d.activa = p.getActiva();
        d.proximaEjecucion = p.getProximaEjecucion();
        d.ultimaEjecucion = p.getUltimaEjecucion();
        d.creadaEn = p.getCreadaEn();
        return d;
    }

    private EjecucionRespaldoDTO toEjecucionDTO(EjecucionRespaldo ej) {
        EjecucionRespaldoDTO d = new EjecucionRespaldoDTO();
        d.id = ej.getId();
        d.estrategiaId = ej.getEstrategia().getId();
        d.estrategiaNombre = ej.getEstrategia().getNombre();
        d.programacionId = ej.getProgramacion() != null ? ej.getProgramacion().getId() : null;
        d.resultado = ej.getResultado();
        d.tipoRespaldo = ej.getTipoRespaldo();
        d.baseDatosNombre = ej.getBaseDatosNombre();
        d.inicio = ej.getInicio();
        d.fin = ej.getFin();
        d.duracionSegundos = ej.getDuracionSegundos();
        d.salidaRman = ej.getSalidaRman();
        d.mensajeError = ej.getMensajeError();
        d.ubicacionRespaldo = ej.getUbicacionRespaldo();
        d.agenteId = ej.getAgenteId();
        d.origen = ej.getOrigen();
        d.tieneScript = ej.getScriptEjecutado() != null && !ej.getScriptEjecutado().isBlank();
        return d;
    }

    private void aplicarBase(BaseDatosRespaldo b, BaseDatosRespaldoDTO req) {
        if (req.nombre != null) b.setNombre(req.nombre);
        if (req.host != null) b.setHost(req.host);
        if (req.puerto != null) b.setPuerto(req.puerto);
        if (req.servicio != null) b.setServicio(req.servicio);
        if (req.modoArchivado != null) b.setModoArchivado(req.modoArchivado);
        if (req.descripcion != null) b.setDescripcion(req.descripcion);
        if (req.agenteId != null) b.setAgenteId(req.agenteId);
        if (req.activa != null) b.setActiva(req.activa);
        if (req.responsableId != null) {
            usuariosRepo.findById(req.responsableId).ifPresent(b::setResponsable);
        }
    }

    private void aplicarEstrategia(EstrategiaRespaldo e,
                                    EstrategiaRespaldoRequest req,
                                    BaseDatosRespaldo bd) {
        e.setBaseDatos(bd);
        if (req.nombre != null) e.setNombre(req.nombre);
        if (req.descripcion != null) e.setDescripcion(req.descripcion);
        if (req.prioridad != null) e.setPrioridad(req.prioridad);
        if (req.estado != null) e.setEstado(req.estado);
        if (req.incluirDatabase != null) e.setIncluirDatabase(req.incluirDatabase);
        e.setTablespaces(req.tablespaces);
        e.setDatafiles(req.datafiles);
        if (req.incluirControlFile != null) e.setIncluirControlFile(req.incluirControlFile);
        if (req.incluirSpfile != null) e.setIncluirSpfile(req.incluirSpfile);
        if (req.incluirArchivedLogs != null) e.setIncluirArchivedLogs(req.incluirArchivedLogs);
        if (req.tipoRespaldo != null) e.setTipoRespaldo(req.tipoRespaldo);
        if (req.compresion != null) e.setCompresion(req.compresion);
        if (req.algoritmoCompresion != null) e.setAlgoritmoCompresion(req.algoritmoCompresion);
        if (req.deleteArchivedLogs != null) e.setDeleteArchivedLogs(req.deleteArchivedLogs);
        if (req.canales != null) e.setCanales(req.canales);
        e.setDestinoRuta(req.destinoRuta);
        if (req.destinoDispositivo != null) e.setDestinoDispositivo(req.destinoDispositivo);
        if (req.formatoBackupset != null) e.setFormatoBackupset(req.formatoBackupset);
        e.setNotas(req.notas);
        if (req.responsableId != null) {
            usuariosRepo.findById(req.responsableId).ifPresent(e::setResponsable);
        }
    }
}
