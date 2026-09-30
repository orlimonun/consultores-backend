package cr.una.consultores.repository;

import cr.una.consultores.entity.Organizacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacionRepository extends JpaRepository<Organizacion, Integer> {
}
