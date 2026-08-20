package cr.una.consultores.repository;

import cr.una.consultores.entity.InstanciaMonitoreada;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstanciaMonitoreadaRepository extends JpaRepository<InstanciaMonitoreada, Integer> {
    List<InstanciaMonitoreada> findByActivoTrue();
}
