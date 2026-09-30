package cr.una.consultores.repository;

import cr.una.consultores.entity.EjecucionRespaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EjecucionRespaldoRepository extends JpaRepository<EjecucionRespaldo, Integer> {

    List<EjecucionRespaldo> findByEstrategiaIdOrderByInicioDesc(Integer estrategiaId);

    /** Últimas N ejecuciones globales para el panel de historial. */
    List<EjecucionRespaldo> findTop50ByOrderByInicioDesc();

    /** Ejecuciones fallidas o con advertencias recientes (para alertas). */
    @Query("""
        SELECT e FROM EjecucionRespaldo e
        WHERE e.resultado IN ('FALLIDO', 'CON_ADVERTENCIAS')
          AND e.inicio >= :desde
        ORDER BY e.inicio DESC
    """)
    List<EjecucionRespaldo> findFallidasDesde(@Param("desde") LocalDateTime desde);

    /** Verifica si una estrategia tuvo ejecución exitosa en las últimas horas. */
    @Query("""
        SELECT COUNT(e) > 0 FROM EjecucionRespaldo e
        WHERE e.estrategia.id = :estrategiaId
          AND e.resultado = 'EXITOSO'
          AND e.inicio >= :desde
    """)
    boolean tieneEjecucionExitosaDesde(@Param("estrategiaId") Integer estrategiaId,
                                       @Param("desde") LocalDateTime desde);
}
