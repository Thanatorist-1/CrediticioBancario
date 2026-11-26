package utp.edu.com.crediticio.repository.politica;
import org.springframework.data.jpa.repository.JpaRepository;
import utp.edu.com.crediticio.model.politica.TipoCredito;
import java.util.*;
public interface TipoCreditoRepository extends JpaRepository<TipoCredito, Long> {
    Optional<TipoCredito> findFirstByCodigoIgnoreCase(String codigo);
    Optional<TipoCredito> findFirstByNombreIgnoreCase(String nombre);
    List<TipoCredito> findAllByOrderByNombreAsc();
    List<TipoCredito> findByActivoTrueOrderByNombreAsc();
}
