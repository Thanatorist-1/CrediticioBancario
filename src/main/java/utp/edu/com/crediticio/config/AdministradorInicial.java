package utp.edu.com.crediticio.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;
import utp.edu.com.crediticio.service.PasswordHasher;
/** Aprovisiona el primer administrador SOLO con credenciales explícitas. No reemplaza contraseñas. */
@Component public class AdministradorInicial implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(AdministradorInicial.class);
    private final UsuarioRepository usuarios;private final PasswordHasher hasher;
    private final String correo,clave;
    public AdministradorInicial(UsuarioRepository u,PasswordHasher h,
        @Value("${ADMIN_BOOTSTRAP_EMAIL:}") String correo,@Value("${ADMIN_BOOTSTRAP_PASSWORD:}") String clave){
        usuarios=u;hasher=h;this.correo=correo;this.clave=clave;}
    @Override public void run(ApplicationArguments a){
        if(usuarios.findAll().stream().anyMatch(u->"Administrador".equals(u.getRol()))) return;
        if(correo.isBlank()||clave.isBlank()){
            log.warn("No hay administrador: configura ADMIN_BOOTSTRAP_EMAIL y ADMIN_BOOTSTRAP_PASSWORD para crearlo.");return;
        }
        if(!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||clave.length()<12)
            throw new IllegalArgumentException("ADMIN_BOOTSTRAP_EMAIL válido y ADMIN_BOOTSTRAP_PASSWORD de 12+ caracteres requeridos.");
        if(usuarios.findFirstByCorreoIgnoreCase(correo.trim()).isPresent())
            throw new IllegalArgumentException("El correo del administrador ya corresponde a una cuenta existente.");
        Usuario u=new Usuario();u.setNombreCompleto("Administrador inicial");u.setCorreo(correo.trim().toLowerCase());
        u.setCargo("Administración del sistema");u.setRol("Administrador");u.setEstado("Activo");
        u.setPasswordHash(hasher.codificar(clave));usuarios.save(u);
        log.info("Cuenta administradora inicial creada (correo configurado). Cambia las credenciales tras el primer uso.");
    }
}
