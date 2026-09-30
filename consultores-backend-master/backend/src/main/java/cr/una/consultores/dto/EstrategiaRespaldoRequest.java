package cr.una.consultores.dto;

/** Payload para crear o actualizar una EstrategiaRespaldo. */
public class EstrategiaRespaldoRequest {
    public String nombre;
    public String descripcion;
    public Integer baseDatosId;
    public Integer responsableId;
    public String prioridad;
    public String estado;

    // QUÉ
    public Boolean incluirDatabase;
    public String tablespaces;
    public String datafiles;
    public Boolean incluirControlFile;
    public Boolean incluirSpfile;
    public Boolean incluirArchivedLogs;

    // CÓMO
    public String tipoRespaldo;
    public Boolean compresion;
    public String algoritmoCompresion;
    public Boolean deleteArchivedLogs;
    public Integer canales;

    // Destino
    public String destinoRuta;
    public String destinoDispositivo;
    public String formatoBackupset;

    public String notas;
}
