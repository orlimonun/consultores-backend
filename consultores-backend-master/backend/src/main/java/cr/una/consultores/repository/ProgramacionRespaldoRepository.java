package cr.una.consultores.repository;

import cr.una.consultores.entity.ProgramacionRespaldo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgramacionRespaldoRepository extends JpaRepository<ProgramacionRespaldo, Integer> {

    List<ProgramacionRespaldo> findByEstrategiaId(Integer estrategiaId);

    Optional<ProgramacionRespaldo> findFirstByEstrategiaIdAndActivaTrue(Integer estrategiaId);
}
