package utp.edu.com.crediticio.service.politica;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import utp.edu.com.crediticio.model.politica.AuditoriaConfiguracion;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.politica.AuditoriaConfiguracionRepository;
import java.util.List;
import java.util.Map;
/** Registra los valores anteriores/nuevos; no expone edición o eliminación del historial. */
@Service
public class AuditoriaConfiguracionService {
    private final AuditoriaConfiguracionRepository repository;
    private final ObjectMapper mapper;
    public AuditoriaConfiguracionService(AuditoriaConfiguracionRepository r, ObjectMapper m) {repository=r; mapper=m;}
    public void guardar(String entidad, Long id, String accion, Usuario actor, Map<String,Object> antes, Map<String,Object> despues) {
        if (actor == null || !"Administrador".equals(actor.getRol()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Solo el administrador puede modificar la configuración.");
        repository.save(new AuditoriaConfiguracion(entidad,id,accion,actor.getIdUsuario(),actor.getCorreo(),json(antes),json(despues)));
    }
    private String json(Map<String,Object> valores) {
        try { return valores == null ? null : mapper.writeValueAsString(valores); }
        catch(JsonProcessingException e) {throw new IllegalStateException("No se pudo registrar la auditoría", e);}
    }
    public List<AuditoriaConfiguracion> listar() {return repository.findAllByOrderByFechaDescIdDesc();}
}
