package cr.una.consultores.controller;

import cr.una.consultores.dto.AuditoriaDTO;
import cr.una.consultores.dto.AuditoriaRequest;
import cr.una.consultores.entity.Auditoria;
import cr.una.consultores.entity.Organizacion;
import cr.una.consultores.entity.Usuario;
import cr.una.consultores.repository.AuditoriaRepository;
import cr.una.consultores.repository.OrganizacionRepository;
import cr.una.consultores.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auditorias")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;
    private final OrganizacionRepository organizacionRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaController(AuditoriaRepository auditoriaRepository,
                               OrganizacionRepository organizacionRepository,
                               UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.organizacionRepository = organizacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<AuditoriaDTO> listar() {
        return auditoriaRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaDTO> obtener(@PathVariable Integer id) {
        return auditoriaRepository.findById(id)
                .map(a -> ResponseEntity.ok(toDTO(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AuditoriaDTO> crear(@Valid @RequestBody AuditoriaRequest req,
                                              Authentication authentication) {
        Organizacion org = organizacionRepository.findById(req.organizacionId)
                .orElseThrow(() -> new IllegalArgumentException("Organizacion no encontrada"));
        Usuario auditor = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));

        Auditoria a = new Auditoria();
        a.setOrganizacion(org);
        a.setAuditor(auditor);
        a.setEstado("BORRADOR");
        Auditoria guardada = auditoriaRepository.save(a);
        return ResponseEntity.ok(toDTO(guardada));
    }

    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<AuditoriaDTO> finalizar(@PathVariable Integer id) {
        return auditoriaRepository.findById(id).map(a -> {
            a.setEstado("FINALIZADA");
            return ResponseEntity.ok(toDTO(auditoriaRepository.save(a)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!auditoriaRepository.existsById(id)) return ResponseEntity.notFound().build();
        auditoriaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private AuditoriaDTO toDTO(Auditoria a) {
        AuditoriaDTO dto = new AuditoriaDTO();
        dto.id = a.getId();
        dto.organizacionId = a.getOrganizacion().getId();
        dto.organizacionNombre = a.getOrganizacion().getNombre();
        dto.auditorId = a.getAuditor().getId();
        dto.auditorNombre = a.getAuditor().getNombre();
        dto.fecha = a.getFecha();
        dto.estado = a.getEstado();
        return dto;
    }
}