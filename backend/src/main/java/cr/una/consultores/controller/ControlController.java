package cr.una.consultores.controller;

import cr.una.consultores.dto.PreguntaDTO;
import cr.una.consultores.dto.PreguntaRequest;
import cr.una.consultores.entity.Control;
import cr.una.consultores.entity.Pregunta;
import cr.una.consultores.repository.ControlRepository;
import cr.una.consultores.repository.PreguntaRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/controles")
public class ControlController {

    private final ControlRepository repo;
    private final PreguntaRepository preguntaRepository;

    public ControlController(ControlRepository repo, PreguntaRepository preguntaRepository) {
        this.repo = repo;
        this.preguntaRepository = preguntaRepository;
    }

    @GetMapping
    public List<Control> listar() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Control> obtener(@PathVariable Integer id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Control crear(@RequestBody Control control) {
        control.setId(null);
        return repo.save(control);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/preguntas")
    public ResponseEntity<List<PreguntaDTO>> listarPreguntas(@PathVariable Integer id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        List<PreguntaDTO> preguntas = preguntaRepository.findByControlId(id).stream()
                .map(p -> new PreguntaDTO(p.getId(), p.getTexto()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(preguntas);
    }

    @PostMapping("/{id}/preguntas")
    public ResponseEntity<PreguntaDTO> crearPregunta(@PathVariable Integer id,
                                                     @Valid @RequestBody PreguntaRequest req) {
        Control control = repo.findById(id).orElse(null);
        if (control == null) return ResponseEntity.notFound().build();

        Pregunta p = new Pregunta();
        p.setControl(control);
        p.setTexto(req.texto);
        Pregunta guardada = preguntaRepository.save(p);
        return ResponseEntity.ok(new PreguntaDTO(guardada.getId(), guardada.getTexto()));
    }
}