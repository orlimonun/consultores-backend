package cr.una.consultores.dto;

import java.time.LocalDateTime;

/** DTO de respuesta para ProgramacionRespaldo. */
public class ProgramacionRespaldoDTO {
    public Integer id;
    public Integer estrategiaId;
    public String estrategiaNombre;
    public String cronExpresion;
    public String descripcionCron;
    public LocalDateTime fechaInicio;
    public LocalDateTime fechaFin;
    public Integer ventanaMinutos;
    public String mecanismo;
    public Boolean activa;
    public LocalDateTime proximaEjecucion;
    public LocalDateTime ultimaEjecucion;
    public LocalDateTime creadaEn;
}
