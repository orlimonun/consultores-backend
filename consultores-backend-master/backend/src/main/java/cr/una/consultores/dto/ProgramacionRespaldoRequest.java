package cr.una.consultores.dto;

import java.time.LocalDateTime;

/** Payload para crear o actualizar una ProgramacionRespaldo. */
public class ProgramacionRespaldoRequest {
    public Integer estrategiaId;
    public String cronExpresion;
    public String descripcionCron;
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public Integer ventanaMinutos;
    public String mecanismo;
}
