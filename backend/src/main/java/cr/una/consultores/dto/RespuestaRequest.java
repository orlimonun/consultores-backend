package cr.una.consultores.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RespuestaRequest {
    @NotNull
    public Integer preguntaId;

    @NotBlank
    public String respuesta; // SI, NO, NA

    // Nivel de madurez elegido por el auditor (0-5). Opcional al guardar
    // parcialmente, pero necesario para el calculo de resultados.
    @Min(0)
    @Max(5)
    public Short nivelMadurez;

    public String observacion;
}
