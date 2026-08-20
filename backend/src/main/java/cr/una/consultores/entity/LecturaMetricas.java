package cr.una.consultores.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Una lectura (toma) de las 25 metricas de una instancia en un momento dado.
 * Las metricas coinciden 1 a 1 con las que espera el frontend del monitor.
 */
@Entity
@Table(name = "lectura_metricas")
public class LecturaMetricas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instancia_id", nullable = false)
    private InstanciaMonitoreada instancia;

    @Column(nullable = false)
    private LocalDateTime tomadaEn = LocalDateTime.now();

    // --- PROCESOS (8) ---
    private Integer procesosActuales;
    private Integer limiteProcesos;
    private Integer sesionesActuales;
    private Integer sesionesActivas;
    private Integer sesionesInactivas;
    private Integer sesionesBloqueadas;
    private Integer operacionesProlongadas;
    private Integer usoRecursos;

    // --- MEMORIA (9) ---
    private Integer tamanoSga;
    private Integer sgaLibre;
    private Integer sharedPoolUso;
    private Integer bufferCacheHit;
    private Integer pgaAsignada;
    private Integer pgaUtilizada;
    private Integer pgaMaxima;
    private Integer overAllocation;
    private Integer pgaCacheHit;

    // --- ARCHIVOS (8) ---
    private Integer datafilesOnline;
    private Integer datafilesOffline;
    private Integer capacidadDatafiles;
    private Integer tablespaces;
    private Integer tempfilesProblema;
    private Integer redoProblema;
    private Integer archivosInvalidos;
    private Integer archivosInaccesibles;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public InstanciaMonitoreada getInstancia() { return instancia; }
    public void setInstancia(InstanciaMonitoreada instancia) { this.instancia = instancia; }
    public LocalDateTime getTomadaEn() { return tomadaEn; }
    public void setTomadaEn(LocalDateTime tomadaEn) { this.tomadaEn = tomadaEn; }

    public Integer getProcesosActuales() { return procesosActuales; }
    public void setProcesosActuales(Integer v) { this.procesosActuales = v; }
    public Integer getLimiteProcesos() { return limiteProcesos; }
    public void setLimiteProcesos(Integer v) { this.limiteProcesos = v; }
    public Integer getSesionesActuales() { return sesionesActuales; }
    public void setSesionesActuales(Integer v) { this.sesionesActuales = v; }
    public Integer getSesionesActivas() { return sesionesActivas; }
    public void setSesionesActivas(Integer v) { this.sesionesActivas = v; }
    public Integer getSesionesInactivas() { return sesionesInactivas; }
    public void setSesionesInactivas(Integer v) { this.sesionesInactivas = v; }
    public Integer getSesionesBloqueadas() { return sesionesBloqueadas; }
    public void setSesionesBloqueadas(Integer v) { this.sesionesBloqueadas = v; }
    public Integer getOperacionesProlongadas() { return operacionesProlongadas; }
    public void setOperacionesProlongadas(Integer v) { this.operacionesProlongadas = v; }
    public Integer getUsoRecursos() { return usoRecursos; }
    public void setUsoRecursos(Integer v) { this.usoRecursos = v; }

    public Integer getTamanoSga() { return tamanoSga; }
    public void setTamanoSga(Integer v) { this.tamanoSga = v; }
    public Integer getSgaLibre() { return sgaLibre; }
    public void setSgaLibre(Integer v) { this.sgaLibre = v; }
    public Integer getSharedPoolUso() { return sharedPoolUso; }
    public void setSharedPoolUso(Integer v) { this.sharedPoolUso = v; }
    public Integer getBufferCacheHit() { return bufferCacheHit; }
    public void setBufferCacheHit(Integer v) { this.bufferCacheHit = v; }
    public Integer getPgaAsignada() { return pgaAsignada; }
    public void setPgaAsignada(Integer v) { this.pgaAsignada = v; }
    public Integer getPgaUtilizada() { return pgaUtilizada; }
    public void setPgaUtilizada(Integer v) { this.pgaUtilizada = v; }
    public Integer getPgaMaxima() { return pgaMaxima; }
    public void setPgaMaxima(Integer v) { this.pgaMaxima = v; }
    public Integer getOverAllocation() { return overAllocation; }
    public void setOverAllocation(Integer v) { this.overAllocation = v; }
    public Integer getPgaCacheHit() { return pgaCacheHit; }
    public void setPgaCacheHit(Integer v) { this.pgaCacheHit = v; }

    public Integer getDatafilesOnline() { return datafilesOnline; }
    public void setDatafilesOnline(Integer v) { this.datafilesOnline = v; }
    public Integer getDatafilesOffline() { return datafilesOffline; }
    public void setDatafilesOffline(Integer v) { this.datafilesOffline = v; }
    public Integer getCapacidadDatafiles() { return capacidadDatafiles; }
    public void setCapacidadDatafiles(Integer v) { this.capacidadDatafiles = v; }
    public Integer getTablespaces() { return tablespaces; }
    public void setTablespaces(Integer v) { this.tablespaces = v; }
    public Integer getTempfilesProblema() { return tempfilesProblema; }
    public void setTempfilesProblema(Integer v) { this.tempfilesProblema = v; }
    public Integer getRedoProblema() { return redoProblema; }
    public void setRedoProblema(Integer v) { this.redoProblema = v; }
    public Integer getArchivosInvalidos() { return archivosInvalidos; }
    public void setArchivosInvalidos(Integer v) { this.archivosInvalidos = v; }
    public Integer getArchivosInaccesibles() { return archivosInaccesibles; }
    public void setArchivosInaccesibles(Integer v) { this.archivosInaccesibles = v; }
}
