package cr.una.consultores.dto;

/**
 * Payload que el agente RMAN envía al backend cuando termina una ejecución.
 * El campo ejecucionId debe coincidir con el registro creado al despachar
 * el script hacia el agente.
 */
public class ResultadoRmanDTO {
    public Integer ejecucionId;
    public String resultado;          // EXITOSO | CON_ADVERTENCIAS | FALLIDO
    public String salidaRman;         // stdout + stderr del proceso rman
    public String mensajeError;
    public String ubicacionRespaldo;
    public Long duracionSegundos;
}
