package cr.una.consultores.controller;

import cr.una.consultores.entity.Dominio;
import cr.una.consultores.repository.DominioRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dominios")
public class DominioController {

    private final DominioRepository repo;

    public DominioController(DominioRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Dominio> listar() {
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<Dominio> crear(@Valid @RequestBody Dominio dominio) {
        // Evitar duplicados por nombre
        String nombre = dominio.getNombre() != null ? dominio.getNombre().trim() : "";
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre del dominio es obligatorio");
        }
        if (repo.findAll().stream().anyMatch(d -> d.getNombre().equalsIgnoreCase(nombre))) {
            throw new IllegalArgumentException("Ya existe un dominio con ese nombre");
        }
        dominio.setId(null);
        dominio.setNombre(nombre);
        return ResponseEntity.ok(repo.save(dominio));
    }
}
