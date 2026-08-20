package cr.una.consultores.dto;

public class LecturaRequest {
    // Procesos
    public Integer procesosActuales, limiteProcesos, sesionesActuales, sesionesActivas,
            sesionesInactivas, sesionesBloqueadas, operacionesProlongadas, usoRecursos;
    // Memoria
    public Integer tamanoSga, sgaLibre, sharedPoolUso, bufferCacheHit, pgaAsignada,
            pgaUtilizada, pgaMaxima, overAllocation, pgaCacheHit;
    // Archivos
    public Integer datafilesOnline, datafilesOffline, capacidadDatafiles, tablespaces,
            tempfilesProblema, redoProblema, archivosInvalidos, archivosInaccesibles;
}
