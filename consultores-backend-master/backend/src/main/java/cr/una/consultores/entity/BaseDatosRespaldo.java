package cr.una.consultores.entity;

import jakarta.persistence.*;

/**
 * Registro de una instancia Oracle sobre la que se gestionan estrategias
 * de respaldo. No almacena credenciales: la conexión la ejecuta el agente
 * RMAN que corre en la misma red que Oracle.
 */
@Entity
@Table(name = "base_datos_respaldo")
public class BaseDatosRespaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nombre descriptivo: "PRODDB", "Oracle XE Lab", etc. */
    @Column(nullable = false, length = 150)
    private String nombre;

    /** Host o IP donde corre Oracle (referencia informativa). */
    @Column(nullable = false, length = 255)
    private String host;

    /** Puerto del listener Oracle. */
    @Column(nullable = false)
    private Integer puerto = 1521;

    /** Nombre del servicio / SID (ej. XE, ORCL, XEPDB1). */
    @Column(nullable = false, length = 100)
    private String servicio;

    /**
     * Modo de archivado: ARCHIVELOG | NOARCHIVELOG | DESCONOCIDO.
     * Se actualiza cuando el agente reporta o el admin lo confirma.
     */
    @Column(name = "modo_archivado", nullable = false, length = 20)
    private String modoArchivado = "DESCONOCIDO";

    /** Descripción libre del entorno (dev, test, producción, lab académico). */
    @Column(length = 500)
    private String descripcion;

    /** Usuario responsable / DBA de esta instancia. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    /** ID del agente RMAN que tiene acceso a esta instancia. */
    @Column(name = "agente_id", length = 100)
    private String agenteId;

    /** Si está activa para crear estrategias. */
    @Column(nullable = false)
    private Boolean activa = true;

    // ── getters / setters ───────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPuerto() { return puerto; }
    public void setPuerto(Integer puerto) { this.puerto = puerto; }

    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }

    public String getModoArchivado() { return modoArchivado; }
    public void setModoArchivado(String modoArchivado) { this.modoArchivado = modoArchivado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Usuario getResponsable() { return responsable; }
    public void setResponsable(Usuario responsable) { this.responsable = responsable; }

    public String getAgenteId() { return agenteId; }
    public void setAgenteId(String agenteId) { this.agenteId = agenteId; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
