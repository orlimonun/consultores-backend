package cr.una.consultores.service;

import cr.una.consultores.dto.LecturaRequest;
import cr.una.consultores.entity.InstanciaMonitoreada;
import cr.una.consultores.entity.LecturaMetricas;
import cr.una.consultores.repository.InstanciaMonitoreadaRepository;
import cr.una.consultores.repository.LecturaMetricasRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MonitoreoService {

    private final InstanciaMonitoreadaRepository instanciaRepo;
    private final LecturaMetricasRepository lecturaRepo;

    public MonitoreoService(InstanciaMonitoreadaRepository instanciaRepo,
                            LecturaMetricasRepository lecturaRepo) {
        this.instanciaRepo = instanciaRepo;
        this.lecturaRepo = lecturaRepo;
    }

    public InstanciaMonitoreada crearInstancia(InstanciaMonitoreada i) {
        i.setId(null);
        i.setActivo(true);
        return instanciaRepo.save(i);
    }

    public LecturaMetricas registrarLectura(Integer instanciaId, LecturaRequest req) {
        InstanciaMonitoreada instancia = instanciaRepo.findById(instanciaId)
                .orElseThrow(() -> new IllegalArgumentException("Instancia no encontrada"));
        LecturaMetricas l = new LecturaMetricas();
        l.setInstancia(instancia);
        l.setTomadaEn(LocalDateTime.now());
        l.setProcesosActuales(req.procesosActuales);
        l.setLimiteProcesos(req.limiteProcesos);
        l.setSesionesActuales(req.sesionesActuales);
        l.setSesionesActivas(req.sesionesActivas);
        l.setSesionesInactivas(req.sesionesInactivas);
        l.setSesionesBloqueadas(req.sesionesBloqueadas);
        l.setOperacionesProlongadas(req.operacionesProlongadas);
        l.setUsoRecursos(req.usoRecursos);
        l.setTamanoSga(req.tamanoSga);
        l.setSgaLibre(req.sgaLibre);
        l.setSharedPoolUso(req.sharedPoolUso);
        l.setBufferCacheHit(req.bufferCacheHit);
        l.setPgaAsignada(req.pgaAsignada);
        l.setPgaUtilizada(req.pgaUtilizada);
        l.setPgaMaxima(req.pgaMaxima);
        l.setOverAllocation(req.overAllocation);
        l.setPgaCacheHit(req.pgaCacheHit);
        l.setDatafilesOnline(req.datafilesOnline);
        l.setDatafilesOffline(req.datafilesOffline);
        l.setCapacidadDatafiles(req.capacidadDatafiles);
        l.setTablespaces(req.tablespaces);
        l.setTempfilesProblema(req.tempfilesProblema);
        l.setRedoProblema(req.redoProblema);
        l.setArchivosInvalidos(req.archivosInvalidos);
        l.setArchivosInaccesibles(req.archivosInaccesibles);
        return lecturaRepo.save(l);
    }
}
