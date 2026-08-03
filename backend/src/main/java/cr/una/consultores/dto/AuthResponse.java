package cr.una.consultores.dto;

public class AuthResponse {
    public String token;
    public String nombre;
    public String email;
    public String rol;

    public AuthResponse(String token, String nombre, String email, String rol) {
        this.token = token;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }
}
