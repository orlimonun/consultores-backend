package cr.una.consultores.agente;

/** Resultado que el agente reporta al backend tras ejecutar RMAN. */
public class ResultadoRman {
    public Integer ejecucionId;
    public String resultado;          // EXITOSO | CON_ADVERTENCIAS | FALLIDO
    public String salidaRman;
    public String mensajeError;
    public String ubicacionRespaldo;
    public Long duracionSegundos;
}
