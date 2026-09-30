package cr.una.consultores.dto;

import java.time.LocalDateTime;

/** Alerta de control preventivo generada por AlertaRespaldoService. */
public class AlertaRespaldoDTO {
    public String tipo;           // SIN_PROGRAMACION | INACTIVA | RESPALDO_VENCIDO |
                                  // EJECUCION_FALLIDA | NOARCHIVELOG | SIN_RESPALDO_RECIENTE
    public String severidad;      // INFO | ADVERTENCIA | CRITICA
    public Integer estrategiaId;
    public String estrategiaNombre;
    public String baseDatosNombre;
    public String mensaje;
    public LocalDateTime detectedaEn;
}
