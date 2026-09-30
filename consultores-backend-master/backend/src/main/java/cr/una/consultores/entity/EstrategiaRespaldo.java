package cr.una.consultores.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Estrategia de respaldo: define QUÉ respaldar, CÓMO respaldarlo y
 * delega el CUÁNDO a ProgramacionRespaldo.
 *
 * Cada estrategia pertenece a una BaseDatosRespaldo y produce, en el
 * momento de su aprobación, un script RMAN que queda almacenado aquí.
 */
@Entity
@Table(name = "estrategia_respaldo")
public class EstrategiaRespaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_datos_id", nullable = false)
    private BaseDatosRespaldo baseDatos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    /**
     * Prioridad: ALTA | MEDIA | BAJA.
     * Determina el orden de ejecución cuando hay conflictos de ventana.
     */
    @Column(nullable = false, length = 10)
    private String prioridad = "MEDIA";

    /**
     * Estado: BORRADOR | ACTIVA | INACTIVA | PENDIENTE_APROBACION.
     * Solo las ACTIVA tienen programación ejecutable.
     */
    @Column(nullable = false, length = 30)
    private String estado = "BORRADOR";

    // ── QUÉ respaldar ────────────────────────────────────────────────────────

    /** Respaldar la base de datos completa. */
    @Column(name = "incluir_database", nullable = false)
    private Boolean incluirDatabase = false;

    /** Lista de tablespaces separada por comas (ej. "USERS,INDX"). */
    @Column(name = "tablespaces", length = 500)
    private String tablespaces;

    /** Lista de datafiles separada por comas (ej. "/u01/oradata/users01.dbf"). */
    @Column(name = "datafiles", length = 1000)
    private String datafiles;

    @Column(name = "incluir_control_file", nullable = false)
    private Boolean incluirControlFile = true;

    @Column(name = "incluir_spfile", nullable = false)
    private Boolean incluirSpfile = true;

    @Column(name = "incluir_archived_logs", nullable = false)
    private Boolean incluirArchivedLogs = false;

    // ── CÓMO respaldar ───────────────────────────────────────────────────────

    /**
     * Tipo de respaldo: COMPLETO | INCREMENTAL_NIVEL_0 | INCREMENTAL_NIVEL_1
     *                   | INCREMENTAL_DIFERENCIAL | INCREMENTAL_ACUMULATIVO
     */
    @Column(name = "tipo_respaldo", nullable = false, length = 30)
    private String tipoRespaldo = "COMPLETO";

    /** Usar compresión RMAN (COMPRESSED BACKUPSET). */
    @Column(nullable = false)
    private Boolean compresion = false;

    /** Algoritmo de compresión: BASIC | LOW | MEDIUM | HIGH (requiere Advanced Compression). */
    @Column(name = "algoritmo_compresion", length = 10)
    private String algoritmoCompresion = "BASIC";

    /** Eliminar archived logs después de respaldarlos. */
    @Column(name = "delete_archived_logs", nullable = false)
    private Boolean deleteArchivedLogs = false;

    /** Número de canales RMAN paralelos. */
    @Column(name = "canales", nullable = false)
    private Integer canales = 1;

    // ── Destino ──────────────────────────────────────────────────────────────

    /** Ruta de destino del respaldo (ej. /backup/rman o USE_DB_RECOVERY_FILE_DEST). */
    @Column(name = "destino_ruta", length = 500)
    private String destinoRuta;

    /** Identificador del dispositivo (ej. DISK, SBT_TAPE). */
    @Column(name = "destino_dispositivo", length = 50)
    private String destinoDispositivo = "DISK";

    /** Formato de nombre de los backupsets. */
    @Column(name = "formato_backupset", length = 200)
    private String formatoBackupset = "%d_%T_%U";

    // ── Script generado ──────────────────────────────────────────────────────

    /** Script RMAN generado a partir de la estrategia. */
    @Column(name = "script_rman", columnDefinition = "TEXT")
    private String scriptRman;

    /** Cuándo fue generado el script actual. */
    @Column(name = "script_generado_en")
    private LocalDateTime scriptGeneradoEn;

    /** Notas adicionales del administrador. */
    @Column(length = 1000)
    private String notas;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Column(name = "actualizada_en")
    private LocalDateTime actualizadaEn;

    // ── getters / setters ────────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BaseDatosRespaldo getBaseDatos() { return baseDatos; }
    public void setBaseDatos(BaseDatosRespaldo baseDatos) { this.baseDatos = baseDatos; }

    public Usuario getResponsable() { return responsable; }
    public void setResponsable(Usuario responsable) { this.responsable = responsable; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Boolean getIncluirDatabase() { return incluirDatabase; }
    public void setIncluirDatabase(Boolean incluirDatabase) { this.incluirDatabase = incluirDatabase; }

    public String getTablespaces() { return tablespaces; }
    public void setTablespaces(String tablespaces) { this.tablespaces = tablespaces; }

    public String getDatafiles() { return datafiles; }
    public void setDatafiles(String datafiles) { this.datafiles = datafiles; }

    public Boolean getIncluirControlFile() { return incluirControlFile; }
    public void setIncluirControlFile(Boolean incluirControlFile) { this.incluirControlFile = incluirControlFile; }

    public Boolean getIncluirSpfile() { return incluirSpfile; }
    public void setIncluirSpfile(Boolean incluirSpfile) { this.incluirSpfile = incluirSpfile; }

    public Boolean getIncluirArchivedLogs() { return incluirArchivedLogs; }
    public void setIncluirArchivedLogs(Boolean incluirArchivedLogs) { this.incluirArchivedLogs = incluirArchivedLogs; }

    public String getTipoRespaldo() { return tipoRespaldo; }
    public void setTipoRespaldo(String tipoRespaldo) { this.tipoRespaldo = tipoRespaldo; }

    public Boolean getCompresion() { return compresion; }
    public void setCompresion(Boolean compresion) { this.compresion = compresion; }

    public String getAlgoritmoCompresion() { return algoritmoCompresion; }
    public void setAlgoritmoCompresion(String algoritmoCompresion) { this.algoritmoCompresion = algoritmoCompresion; }

    public Boolean getDeleteArchivedLogs() { return deleteArchivedLogs; }
    public void setDeleteArchivedLogs(Boolean deleteArchivedLogs) { this.deleteArchivedLogs = deleteArchivedLogs; }

    public Integer getCanales() { return canales; }
    public void setCanales(Integer canales) { this.canales = canales; }

    public String getDestinoRuta() { return destinoRuta; }
    public void setDestinoRuta(String destinoRuta) { this.destinoRuta = destinoRuta; }

    public String getDestinoDispositivo() { return destinoDispositivo; }
    public void setDestinoDispositivo(String destinoDispositivo) { this.destinoDispositivo = destinoDispositivo; }

    public String getFormatoBackupset() { return formatoBackupset; }
    public void setFormatoBackupset(String formatoBackupset) { this.formatoBackupset = formatoBackupset; }

    public String getScriptRman() { return scriptRman; }
    public void setScriptRman(String scriptRman) { this.scriptRman = scriptRman; }

    public LocalDateTime getScriptGeneradoEn() { return scriptGeneradoEn; }
    public void setScriptGeneradoEn(LocalDateTime scriptGeneradoEn) { this.scriptGeneradoEn = scriptGeneradoEn; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }

    public LocalDateTime getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(LocalDateTime actualizadaEn) { this.actualizadaEn = actualizadaEn; }
}
