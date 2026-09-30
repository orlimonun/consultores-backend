package cr.una.consultores.controller;

import cr.una.consultores.entity.Organizacion;
import cr.una.consultores.repository.OrganizacionRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizaciones")
public class OrganizacionController {

    private final OrganizacionRepository repo;

    public OrganizacionController(OrganizacionRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Organizacion> listar() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organizacion> obtener(@PathVariable Integer id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Organizacion crear(@Valid @RequestBody Organizacion org) {
        org.setId(null);
        return repo.save(org);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Organizacion> actualizar(@PathVariable Integer id,
                                                   @Valid @RequestBody Organizacion datos) {
        return repo.findById(id).map(org -> {
            org.setNombre(datos.getNombre());
            org.setAreaEvaluada(datos.getAreaEvaluada());
            org.setDbaNombre(datos.getDbaNombre());
            return ResponseEntity.ok(repo.save(org));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
