package cr.una.consultores.dto;

import java.time.LocalDateTime;
import java.util.List;

/** DTO de respuesta completo para EstrategiaRespaldo. */
public class EstrategiaRespaldoDTO {
    public Integer id;
    public String nombre;
    public String descripcion;

    // Base de datos
    public Integer baseDatosId;
    public String baseDatosNombre;
    public String modoArchivado;

    // Responsable
    public Integer responsableId;
    public String responsableNombre;

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

    // Script generado
    public String scriptRman;
    public LocalDateTime scriptGeneradoEn;

    // Advertencias actuales de la estrategia
    public List<String> advertencias;

    public String notas;
    public LocalDateTime creadaEn;
    public LocalDateTime actualizadaEn;

    // Resumen de última ejecución (puede ser null)
    public String ultimoResultado;
    public LocalDateTime ultimaEjecucion;
}
