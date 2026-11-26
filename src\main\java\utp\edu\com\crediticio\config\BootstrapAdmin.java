package utp.edu.com.crediticio.config;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;
import utp.edu.com.crediticio.service.PasswordHasher;
/** Primer administrador: solo se crea si el operador suministra credenciales por variables de entorno. */
@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final UsuarioRepository usuarios; private final PasswordHasher hasher;
    @Value("${bootstrap.admin.email:}") private String email;
    @Value("${bootstrap.admin.password:}") private String password;
    public BootstrapAdmin(UsuarioRepository u, PasswordHasher h){usuarios=u;hasher=h;}
    @Override @Transactional public void run(ApplicationArguments args) {
        if(email==null || email.isBlank() || password==null || password.isBlank()) return;
        if(password.length()<12) throw new IllegalArgumentException("BOOTSTRAP_ADMIN_PASSWORD requiere al menos 12 caracteres.");
        if (email.indexOf('@') < 1 || email.indexOf('.', email.indexOf('@')) < 0)
            throw new IllegalArgumentException("BOOTSTRAP_ADMIN_EMAIL inválido.");
        if(usuarios.findFirstByCorreoIgnoreCase(email.trim()).isPresent()) return;
        Usuario admin=new Usuario();admin.setNombreCompleto("Administrador inicial");admin.setCorreo(email.trim().toLowerCase());
        admin.setCargo("Administrador del sistema");admin.setRol("Administrador");admin.setEstado("Activo");
        admin.setPasswordHash(hasher.codificar(password)); usuarios.save(admin);
    }
}
