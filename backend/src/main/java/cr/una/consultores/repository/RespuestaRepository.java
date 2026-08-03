package cr.una.consultores.repository;

import cr.una.consultores.entity.Respuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RespuestaRepository extends JpaRepository<Respuesta, Integer> {
    List<Respuesta> findByAuditoriaId(Integer auditoriaId);
    Optional<Respuesta> findByAuditoriaIdAndPreguntaId(Integer auditoriaId, Integer preguntaId);
}
