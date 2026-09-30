package cr.una.consultores.repository;

import cr.una.consultores.entity.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PreguntaRepository extends JpaRepository<Pregunta, Integer> {
    List<Pregunta> findByControlId(Integer controlId);
}
