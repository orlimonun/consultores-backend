package cr.una.consultores.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Evidencia de una ejecución de estrategia de respaldo.
 * Se crea por cada intento de ejecución (exitoso, con advertencias o fallido).
 * Este registro es inmutable una vez completado: constituye la evidencia
 * de control preventivo del proyecto.
 */
@Entity
@Table(name = "ejecucion_respaldo")
public class EjecucionRespaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estrategia_id", nullable = false)
    private EstrategiaRespaldo estrategia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programacion_id")
    private ProgramacionRespaldo programacion;

    /**
     * Resultado: EXITOSO | CON_ADVERTENCIAS | FALLIDO | EN_PROGRESO | CANCELADO
     */
    @Column(nullable = false, length = 20)
    private String resultado = "EN_PROGRESO";

    /** Script RMAN exacto que fue enviado al agente para ejecución. */
    @Column(name = "script_ejecutado", columnDefinition = "TEXT")
    private String scriptEjecutado;

    /** Tipo de respaldo ejecutado (copiado de la estrategia al momento de ejecutar). */
    @Column(name = "tipo_respaldo", length = 30)
    private String tipoRespaldo;

    /** Base de datos sobre la que se ejecutó. */
    @Column(name = "base_datos_nombre", length = 150)
    private String baseDatosNombre;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio = LocalDateTime.now();

    @Column(name = "fin")
    private LocalDateTime fin;

    /** Duración en segundos (calculada al completar). */
    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    /**
     * Salida completa de RMAN (stdout + stderr combinados).
     * Es la evidencia técnica de la ejecución.
     */
    @Column(name = "salida_rman", columnDefinition = "TEXT")
    private String salidaRman;

    /** Mensaje de error principal si resultado = FALLIDO. */
    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    /** Ruta donde quedó almacenado el respaldo. */
    @Column(name = "ubicacion_respaldo", length = 500)
    private String ubicacionRespaldo;

    /** ID del agente RMAN que ejecutó el respaldo. */
    @Column(name = "agente_id", length = 100)
    private String agenteId;

    /**
     * Origen de la ejecución: PROGRAMADO | MANUAL.
     * Permite distinguir ejecuciones manuales de las automáticas.
     */
    @Column(nullable = false, length = 15)
    private String origen = "PROGRAMADO";

    // ── getters / setters ────────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public EstrategiaRespaldo getEstrategia() { return estrategia; }
    public void setEstrategia(EstrategiaRespaldo estrategia) { this.estrategia = estrategia; }

    public ProgramacionRespaldo getProgramacion() { return programacion; }
    public void setProgramacion(ProgramacionRespaldo programacion) { this.programacion = programacion; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getScriptEjecutado() { return scriptEjecutado; }
    public void setScriptEjecutado(String scriptEjecutado) { this.scriptEjecutado = scriptEjecutado; }

    public String getTipoRespaldo() { return tipoRespaldo; }
    public void setTipoRespaldo(String tipoRespaldo) { this.tipoRespaldo = tipoRespaldo; }

    public String getBaseDatosNombre() { return baseDatosNombre; }
    public void setBaseDatosNombre(String baseDatosNombre) { this.baseDatosNombre = baseDatosNombre; }

    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }

    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }

    public Long getDuracionSegundos() { return duracionSegundos; }
    public void setDuracionSegundos(Long duracionSegundos) { this.duracionSegundos = duracionSegundos; }

    public String getSalidaRman() { return salidaRman; }
    public void setSalidaRman(String salidaRman) { this.salidaRman = salidaRman; }

    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }

    public String getUbicacionRespaldo() { return ubicacionRespaldo; }
    public void setUbicacionRespaldo(String ubicacionRespaldo) { this.ubicacionRespaldo = ubicacionRespaldo; }

    public String getAgenteId() { return agenteId; }
    public void setAgenteId(String agenteId) { this.agenteId = agenteId; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
}
