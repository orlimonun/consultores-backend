package cr.una.consultores.repository;

import cr.una.consultores.entity.EstrategiaRespaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EstrategiaRespaldoRepository extends JpaRepository<EstrategiaRespaldo, Integer> {

    List<EstrategiaRespaldo> findByBaseDatosId(Integer baseDatosId);

    List<EstrategiaRespaldo> findByEstado(String estado);

    /** Estrategias activas que tienen programación activa: las que se ejecutarán. */
    @Query("""
        SELECT DISTINCT e FROM EstrategiaRespaldo e
        JOIN ProgramacionRespaldo p ON p.estrategia = e
        WHERE e.estado = 'ACTIVA' AND p.activa = true
    """)
    List<EstrategiaRespaldo> findActivasConProgramacion();
}
