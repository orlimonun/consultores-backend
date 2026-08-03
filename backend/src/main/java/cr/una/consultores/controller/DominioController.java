package cr.una.consultores.controller;

import cr.una.consultores.entity.Dominio;
import cr.una.consultores.repository.DominioRepository;
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
}