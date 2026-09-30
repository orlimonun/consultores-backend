package cr.una.consultores.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Programación de una estrategia: define el CUÁNDO.
 * Una estrategia puede tener una programación activa a la vez.
 */
@Entity
@Table(name = "programacion_respaldo")
public class ProgramacionRespaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estrategia_id", nullable = false)
    private EstrategiaRespaldo estrategia;

    /**
     * Expresión cron (5 campos, compatible con Spring Scheduler / cron de Unix).
     * Ej: "0 2 * * 0" = cada domingo a las 02:00
     *     "0 23 * * 1-5" = lunes a viernes a las 23:00
     */
    @Column(name = "cron_expresion", nullable = false, length = 100)
    private String cronExpresion;

    /** Descripción legible de la programación (ej. "Cada domingo a las 02:00"). */
    @Column(name = "descripcion_cron", length = 200)
    private String descripcionCron;

    /** Fecha desde la que está vigente esta programación. */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    /** Fecha de expiración (null = sin límite). */
    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    /**
     * Ventana de respaldo en minutos: tiempo máximo permitido para completar
     * la ejecución antes de que se considere que excedió la ventana.
     */
    @Column(name = "ventana_minutos")
    private Integer ventanaMinutos;

    /**
     * Mecanismo de automatización: AGENTE_RMAN | ORACLE_SCHEDULER | CRON_SO | MANUAL.
     * En esta implementación se usa AGENTE_RMAN por defecto.
     */
    @Column(nullable = false, length = 30)
    private String mecanismo = "AGENTE_RMAN";

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "proxima_ejecucion")
    private LocalDateTime proximaEjecucion;

    @Column(name = "ultima_ejecucion")
    private LocalDateTime ultimaEjecucion;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    // ── getters / setters ────────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public EstrategiaRespaldo getEstrategia() { return estrategia; }
    public void setEstrategia(EstrategiaRespaldo estrategia) { this.estrategia = estrategia; }

    public String getCronExpresion() { return cronExpresion; }
    public void setCronExpresion(String cronExpresion) { this.cronExpresion = cronExpresion; }

    public String getDescripcionCron() { return descripcionCron; }
    public void setDescripcionCron(String descripcionCron) { this.descripcionCron = descripcionCron; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public Integer getVentanaMinutos() { return ventanaMinutos; }
    public void setVentanaMinutos(Integer ventanaMinutos) { this.ventanaMinutos = ventanaMinutos; }

    public String getMecanismo() { return mecanismo; }
    public void setMecanismo(String mecanismo) { this.mecanismo = mecanismo; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }

    public LocalDateTime getProximaEjecucion() { return proximaEjecucion; }
    public void setProximaEjecucion(LocalDateTime proximaEjecucion) { this.proximaEjecucion = proximaEjecucion; }

    public LocalDateTime getUltimaEjecucion() { return ultimaEjecucion; }
    public void setUltimaEjecucion(LocalDateTime ultimaEjecucion) { this.ultimaEjecucion = ultimaEjecucion; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}
