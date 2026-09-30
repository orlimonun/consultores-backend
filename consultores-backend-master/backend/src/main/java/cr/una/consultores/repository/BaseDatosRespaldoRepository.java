package cr.una.consultores.repository;

import cr.una.consultores.entity.BaseDatosRespaldo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaseDatosRespaldoRepository extends JpaRepository<BaseDatosRespaldo, Integer> {
    List<BaseDatosRespaldo> findByActivaTrue();
}
