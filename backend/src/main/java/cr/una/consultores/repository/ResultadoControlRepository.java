package cr.una.consultores.repository;

import cr.una.consultores.entity.ResultadoControl;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResultadoControlRepository extends JpaRepository<ResultadoControl, Integer> {
    List<ResultadoControl> findByAuditoriaId(Integer auditoriaId);
    void deleteByAuditoriaId(Integer auditoriaId);
}
