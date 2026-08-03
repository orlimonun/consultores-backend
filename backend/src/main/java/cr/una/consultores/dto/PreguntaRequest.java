package cr.una.consultores.dto;

import jakarta.validation.constraints.NotBlank;

public class PreguntaRequest {
    @NotBlank
    public String texto;
}