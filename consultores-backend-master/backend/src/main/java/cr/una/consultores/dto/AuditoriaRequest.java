package cr.una.consultores.dto;

import jakarta.validation.constraints.NotNull;

public class AuditoriaRequest {
    @NotNull
    public Integer organizacionId;
}