package cr.una.consultores.repository;

import cr.una.consultores.entity.LecturaMetricas;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LecturaMetricasRepository extends JpaRepository<LecturaMetricas, Integer> {
    // La lectura mas reciente de una instancia
    Optional<LecturaMetricas> findTopByInstanciaIdOrderByTomadaEnDesc(Integer instanciaId);
    // Todas las lecturas de una instancia, mas nuevas primero (para el historico)
    List<LecturaMetricas> findByInstanciaIdOrderByTomadaEnDesc(Integer instanciaId);
}
