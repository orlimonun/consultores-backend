package cr.una.consultores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RespuestaRequest {
    @NotNull
    public Integer preguntaId;

    @NotBlank
    public String respuesta; // SI, NO, NA

    public String observacion;
}