package cr.una.consultores.controller;

import cr.una.consultores.dto.ControlPublicoDTO;
import cr.una.consultores.dto.PreguntaDTO;
import cr.una.consultores.entity.Control;
import cr.una.consultores.repository.ControlRepository;
import cr.una.consultores.repository.PreguntaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

// Endpoint publico de solo lectura, pensado unicamente para alimentar la
// ventana de demostracion sin necesidad de iniciar sesion. No expone
// organizaciones, auditorias ni resultados: solo el catalogo de controles
// y preguntas del instrumento, tal como esta cargado en la base de datos real.
@RestController
@RequestMapping("/api/public")
public class PublicInstrumentoController {

    private final ControlRepository controlRepository;
    private final PreguntaRepository preguntaRepository;

    public PublicInstrumentoController(ControlRepository controlRepository,
                                       PreguntaRepository preguntaRepository) {
        this.controlRepository = controlRepository;
        this.preguntaRepository = preguntaRepository;
    }

    @GetMapping("/instrumento")
    public List<ControlPublicoDTO> instrumento() {
        List<Control> controles = controlRepository.findAll();

        return controles.stream()
                .map(c -> {
                    List<PreguntaDTO> preguntas = preguntaRepository.findByControlId(c.getId()).stream()
                            .map(p -> new PreguntaDTO(p.getId(), p.getTexto()))
                            .collect(Collectors.toList());

                    return new ControlPublicoDTO(
                            c.getId(),
                            c.getCodigo(),
                            c.getNombre(),
                            c.getDominio() != null ? c.getDominio().getNombre() : null,
                            c.getObjetivo(),
                            c.getPeso(),
                            c.getAfectaC(),
                            c.getAfectaI(),
                            c.getAfectaD(),
                            preguntas
                    );
                })
                .collect(Collectors.toList());
    }
}