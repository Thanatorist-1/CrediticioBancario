package utp.edu.com.crediticio.repository.evaluacion;

import utp.edu.com.crediticio.model.evaluacion.Evaluacion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consultas y operaciones CRUD de evaluaciones. */
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {
    Optional<Evaluacion> findFirstBySolicitud_IdSolicitud(Long idSolicitud);
    @Query("select e from Evaluacion e join fetch e.solicitud s where s.idSolicitud in :ids")
    List<Evaluacion> buscarPorSolicitudes(@Param("ids") List<Long> ids);
}



