package cr.una.consultores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RegisterRequest {
    @NotBlank
    public String nombre;

    @NotBlank @Email
    public String email;

    @NotBlank
    public String password;

    public String rol; // ADMIN o AUDITOR (opcional, default AUDITOR)
}
