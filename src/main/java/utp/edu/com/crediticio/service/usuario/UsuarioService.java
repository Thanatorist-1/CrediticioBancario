package utp.edu.com.crediticio.service.usuario;

import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;

import java.util.List;
import org.springframework.stereotype.Service;

/** Reglas de gestión de las cuentas internas del banco. */
@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    public UsuarioService(UsuarioRepository usuarioRepository) { this.usuarioRepository = usuarioRepository; }

    public List<Usuario> listar() { return usuarioRepository.findAll(); }
    public Usuario buscar(Long id) { return usuarioRepository.findById(id).orElse(null); }
    public Usuario guardar(Usuario usuario) {
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            throw new IllegalArgumentException("El correo institucional es obligatorio.");
        }
        usuario.setCorreo(usuario.getCorreo().trim().toLowerCase());
        var existente = usuarioRepository.findFirstByCorreoIgnoreCase(usuario.getCorreo());
        if (existente.isPresent() && !existente.get().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
        }
        if (usuario.getRol() == null || usuario.getRol().isBlank()) usuario.setRol("Analista");
        if (usuario.getEstado() == null || usuario.getEstado().isBlank()) usuario.setEstado("Activo");
        return usuarioRepository.save(usuario);
    }
    public void eliminar(Long id) { usuarioRepository.deleteById(id); }
}


