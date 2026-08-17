package cr.una.consultores.controller;

import cr.una.consultores.dto.RespuestaDTO;
import cr.una.consultores.dto.RespuestaRequest;
import cr.una.consultores.entity.Auditoria;
import cr.una.consultores.entity.Pregunta;
import cr.una.consultores.entity.Respuesta;
import cr.una.consultores.repository.AuditoriaRepository;
import cr.una.consultores.repository.PreguntaRepository;
import cr.una.consultores.repository.RespuestaRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

//@RestController
//@RequestMapping("/api/auditorias/{auditoriaId}/respuestas")
//public class RespuestaController {
//
//    private final RespuestaRepository respuestaRepository;
//    private final AuditoriaRepository auditoriaRepository;
//    private final PreguntaRepository preguntaRepository;
//
//    public RespuestaController(RespuestaRepository respuestaRepository,
//                               AuditoriaRepository auditoriaRepository,
//                               PreguntaRepository preguntaRepository) {
//        this.respuestaRepository = respuestaRepository;
//        this.auditoriaRepository = auditoriaRepository;
//        this.preguntaRepository = preguntaRepository;
//    }
//
//    @GetMapping
//    public List<RespuestaDTO> listar(@PathVariable Integer auditoriaId) {
//        return respuestaRepository.findByAuditoriaId(auditoriaId).stream()
//                .map(this::toDTO).collect(Collectors.toList());
//    }
//
//    @PutMapping
//    public ResponseEntity<RespuestaDTO> guardar(@PathVariable Integer auditoriaId,
//                                                @Valid @RequestBody RespuestaRequest req) {
//        Auditoria auditoria = auditoriaRepository.findById(auditoriaId)
//                .orElseThrow(() -> new IllegalArgumentException("Auditoria no encontrada"));
//        Pregunta pregunta = preguntaRepository.findById(req.preguntaId)
//                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));
//
//        Respuesta r = respuestaRepository.findByAuditoriaIdAndPreguntaId(auditoriaId, req.preguntaId)
//                .orElseGet(Respuesta::new);
//        r.setAuditoria(auditoria);
//        r.setPregunta(pregunta);
//        r.setRespuesta(req.respuesta.toUpperCase());
//        r.setObservacion(req.observacion);
//
//        Respuesta guardada = respuestaRepository.save(r);
//        return ResponseEntity.ok(toDTO(guardada));
//    }
//
//    private RespuestaDTO toDTO(Respuesta r) {
//        RespuestaDTO dto = new RespuestaDTO();
//        dto.id = r.getId();
//        dto.preguntaId = r.getPregunta().getId();
//        dto.respuesta = r.getRespuesta();
//        dto.observacion = r.getObservacion();
//        return dto;
//    }
//}

@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/respuestas")
public class RespuestaController {

    private final RespuestaRepository respuestaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final PreguntaRepository preguntaRepository;

    public RespuestaController(RespuestaRepository respuestaRepository,
                               AuditoriaRepository auditoriaRepository,
                               PreguntaRepository preguntaRepository) {
        this.respuestaRepository = respuestaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.preguntaRepository = preguntaRepository;
    }

    @GetMapping
    public List<RespuestaDTO> listar(@PathVariable Integer auditoriaId) {
        return respuestaRepository.findByAuditoriaId(auditoriaId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @PutMapping
    public ResponseEntity<RespuestaDTO> guardar(@PathVariable Integer auditoriaId,
                                                @Valid @RequestBody RespuestaRequest req) {
        Auditoria auditoria = auditoriaRepository.findById(auditoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Auditoria no encontrada"));
        Pregunta pregunta = preguntaRepository.findById(req.preguntaId)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));

        Respuesta r = respuestaRepository.findByAuditoriaIdAndPreguntaId(auditoriaId, req.preguntaId)
                .orElseGet(Respuesta::new);
        r.setAuditoria(auditoria);
        r.setPregunta(pregunta);
        r.setRespuesta(req.respuesta.toUpperCase());
        r.setNivelMadurez(req.nivelMadurez);
        r.setObservacion(req.observacion);

        Respuesta guardada = respuestaRepository.save(r);
        return ResponseEntity.ok(toDTO(guardada));
    }

    private RespuestaDTO toDTO(Respuesta r) {
        RespuestaDTO dto = new RespuestaDTO();
        dto.id = r.getId();
        dto.preguntaId = r.getPregunta().getId();
        dto.respuesta = r.getRespuesta();
        dto.nivelMadurez = r.getNivelMadurez();
        dto.observacion = r.getObservacion();
        return dto;
    }
}
