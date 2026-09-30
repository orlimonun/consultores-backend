package cr.una.consultores.dto;

import java.time.LocalDateTime;

/** DTO de respuesta para EjecucionRespaldo (historial + evidencia). */
public class EjecucionRespaldoDTO {
    public Integer id;
    public Integer estrategiaId;
    public String estrategiaNombre;
    public Integer programacionId;
    public String resultado;
    public String tipoRespaldo;
    public String baseDatosNombre;
    public LocalDateTime inicio;
    public LocalDateTime fin;
    public Long duracionSegundos;
    public String salidaRman;
    public String mensajeError;
    public String ubicacionRespaldo;
    public String agenteId;
    public String origen;
    // El script no se incluye en el listado por peso; se expone en /detalle
    public Boolean tieneScript;
}
