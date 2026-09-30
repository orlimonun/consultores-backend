package cr.una.consultores.service;

import cr.una.consultores.dto.*;
import cr.una.consultores.entity.Usuario;
import cr.una.consultores.repository.UsuarioRepository;
import cr.una.consultores.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest req) {
        if (usuarioRepository.existsByEmail(req.email)) {
            throw new IllegalArgumentException("El email ya esta registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre);
        u.setEmail(req.email);
        u.setPasswordHash(passwordEncoder.encode(req.password));
        u.setRol(req.rol != null ? req.rol : "AUDITOR");
        u.setActivo(true);
        usuarioRepository.save(u);
        String token = jwtService.generateToken(u.getEmail());
        return new AuthResponse(token, u.getNombre(), u.getEmail(), u.getRol());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email, req.password));
        Usuario u = usuarioRepository.findByEmail(req.email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales invalidas"));
        String token = jwtService.generateToken(u.getEmail());
        return new AuthResponse(token, u.getNombre(), u.getEmail(), u.getRol());
    }
}
