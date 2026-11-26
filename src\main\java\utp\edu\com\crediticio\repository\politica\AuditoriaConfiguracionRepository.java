package utp.edu.com.crediticio.repository.politica;
import org.springframework.data.jpa.repository.JpaRepository;
import utp.edu.com.crediticio.model.politica.AuditoriaConfiguracion;
import java.util.List;
public interface AuditoriaConfiguracionRepository extends JpaRepository<AuditoriaConfiguracion, Long> {
    List<AuditoriaConfiguracion> findAllByOrderByFechaDescIdDesc();
}
