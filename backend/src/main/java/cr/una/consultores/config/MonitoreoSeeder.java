package cr.una.consultores.config;

import cr.una.consultores.entity.InstanciaMonitoreada;
import cr.una.consultores.entity.LecturaMetricas;
import cr.una.consultores.repository.InstanciaMonitoreadaRepository;
import cr.una.consultores.repository.LecturaMetricasRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga las 6 instancias de ejemplo del monitor (los mismos datos que el mock
 * del frontend) para tener contenido al probar. Solo corre si no hay instancias.
 */
@Component
@Order(2)
public class MonitoreoSeeder implements CommandLineRunner {

    private final InstanciaMonitoreadaRepository instanciaRepo;
    private final LecturaMetricasRepository lecturaRepo;

    public MonitoreoSeeder(InstanciaMonitoreadaRepository instanciaRepo,
                           LecturaMetricasRepository lecturaRepo) {
        this.instanciaRepo = instanciaRepo;
        this.lecturaRepo = lecturaRepo;
    }

    @Override
    public void run(String... args) {
        if (instanciaRepo.count() > 0) return; // ya sembrado

        // nombre, instancia, ubicacion, y las 25 metricas en orden
        crear("Cliente 1", "ORCL-PROD-01", "San Jose",
                new int[]{126,300,210,42,36,0,1,48, 8,26,68,96,59,54,67,0,95, 18,0,64,61,0,0,0,0});
        crear("Cliente 2", "ORCL-FIN-02", "Heredia",
                new int[]{224,300,361,72,61,2,2,74, 8,18,78,92,67,63,79,0,91, 16,0,74,78,0,0,0,0});
        crear("Cliente 3", "ORCL-ERP-03", "Alajuela",
                new int[]{245,300,398,77,72,3,3,82, 12,12,86,87,79,76,87,2,86, 21,0,81,82,1,0,0,0});
        crear("Cliente 4", "ORCL-CRM-04", "Cartago",
                new int[]{289,300,481,96,88,7,7,97, 8,4,96,76,96,94,99,6,74, 14,3,91,94,3,2,3,1});
        crear("Cliente 5", "ORCL-RRHH-05", "Guanacaste",
                new int[]{153,300,256,51,47,0,1,58, 8,21,79,91,71,68,78,0,90, 17,0,72,76,0,0,0,0});
        crear("Cliente 6", "ORCL-BI-06", "Puntarenas",
                new int[]{238,300,382,76,68,2,3,79, 12,10,88,84,82,79,90,3,82, 19,1,88,87,1,1,0,0});

        System.out.println(">> MonitoreoSeeder: 6 instancias de ejemplo creadas.");
    }

    private void crear(String nombre, String instancia, String ubicacion, int[] v) {
        InstanciaMonitoreada inst = new InstanciaMonitoreada();
        inst.setNombre(nombre);
        inst.setInstancia(instancia);
        inst.setUbicacion(ubicacion);
        inst = instanciaRepo.save(inst);

        LecturaMetricas l = new LecturaMetricas();
        l.setInstancia(inst);
        int i = 0;
        l.setProcesosActuales(v[i++]); l.setLimiteProcesos(v[i++]); l.setSesionesActuales(v[i++]);
        l.setSesionesActivas(v[i++]); l.setSesionesInactivas(v[i++]); l.setSesionesBloqueadas(v[i++]);
        l.setOperacionesProlongadas(v[i++]); l.setUsoRecursos(v[i++]);
        l.setTamanoSga(v[i++]); l.setSgaLibre(v[i++]); l.setSharedPoolUso(v[i++]); l.setBufferCacheHit(v[i++]);
        l.setPgaAsignada(v[i++]); l.setPgaUtilizada(v[i++]); l.setPgaMaxima(v[i++]); l.setOverAllocation(v[i++]);
        l.setPgaCacheHit(v[i++]);
        l.setDatafilesOnline(v[i++]); l.setDatafilesOffline(v[i++]); l.setCapacidadDatafiles(v[i++]);
        l.setTablespaces(v[i++]); l.setTempfilesProblema(v[i++]); l.setRedoProblema(v[i++]);
        l.setArchivosInvalidos(v[i++]); l.setArchivosInaccesibles(v[i++]);
        lecturaRepo.save(l);
    }
}
