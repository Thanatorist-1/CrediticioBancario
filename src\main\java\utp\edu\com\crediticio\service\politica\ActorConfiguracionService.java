package utp.edu.com.crediticio.service.politica;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;
@Service
public class ActorConfiguracionService {
    private final UsuarioRepository usuarios;
    public ActorConfiguracionService(UsuarioRepository u){ usuarios=u; }
    public Usuario admin(HttpSession session) {
        if(session==null || !"PERSONAL".equals(session.getAttribute("tipoCuenta")) ||
                !(session.getAttribute("cuentaId") instanceof Long id))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el administrador.");
        Usuario usuario=usuarios.findById(id).orElse(null);
        if(usuario==null || !"Activo".equalsIgnoreCase(usuario.getEstado()) || !"Administrador".equals(usuario.getRol()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el administrador.");
        return usuario;
    }
}
