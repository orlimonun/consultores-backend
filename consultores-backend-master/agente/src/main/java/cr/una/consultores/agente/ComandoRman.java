package cr.una.consultores.agente;

/** Payload que el backend envía cuando quiere ejecutar un respaldo. */
public class ComandoRman {
    public Integer ejecucionId;   // ID del registro EjecucionRespaldo en el backend
    public String scriptRman;     // Contenido completo del script .rman
    public String descripcion;    // Nombre de la estrategia (para logs)
}
