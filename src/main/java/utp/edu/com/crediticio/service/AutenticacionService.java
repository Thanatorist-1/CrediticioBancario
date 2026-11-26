package utp.edu.com.crediticio.service;

import org.springframework.stereotype.Service;
import utp.edu.com.crediticio.service.externo.ConsultaExternaService;
import utp.edu.com.crediticio.service.externo.PerfilCrediticioExterno;
import org.springframework.transaction.annotation.Transactional;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.cliente.RegistroClienteForm;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.cliente.ClienteRepository;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;

@Service
public class AutenticacionService {
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;
    private final ConsultaExternaService consultaExterna;
    public AutenticacionService(ClienteRepository cr, UsuarioRepository ur, PasswordHasher ph, ConsultaExternaService externa) {
        clienteRepository=cr; usuarioRepository=ur; passwordHasher=ph; consultaExterna=externa;
    }
    public Cuenta autenticar(String usuario, String clave) {
        if (usuario == null || clave == null) return null;
        var persona = usuarioRepository.findFirstByCorreoIgnoreCase(usuario.trim());
        if (persona.isPresent() && "Activo".equalsIgnoreCase(persona.get().getEstado())
                && passwordHasher.verificar(clave, persona.get().getPasswordHash())) {
            Usuario u=persona.get(); return new Cuenta("PERSONAL", u.getIdUsuario(), u.getNombreCompleto());
        }
        var cliente = clienteRepository.findFirstByDni(usuario.trim());
        if (cliente.isPresent() && "Activo".equalsIgnoreCase(cliente.get().getEstado())
                && passwordHasher.verificar(clave, cliente.get().getPasswordHash())) {
            Cliente c=cliente.get(); return new Cuenta("CLIENTE", c.getIdCliente(), c.getNombre());
        }
        return null;
    }
    @Transactional
    public void registrarCliente(RegistroClienteForm form) {
        String dni = form.getDni().trim();
        // Nunca permitir el alta de una identidad que no esté en la API de pruebas.
        PerfilCrediticioExterno externo = consultaExterna.verificarPersonaActiva(dni);
        if (form.getFechaNacimiento() == null || !form.getFechaNacimiento().equals(externo.fechaNacimiento()))
            throw new IllegalArgumentException("La fecha de nacimiento no coincide con el registro simulado.");
        if (!normalizar(form.getNombre()).equals(normalizar(externo.nombreCompleto())))
            throw new IllegalArgumentException("El nombre no coincide con el perfil ficticio de la API.");
        if (clienteRepository.findFirstByDni(dni).isPresent())
            throw new IllegalArgumentException("Ya existe una cuenta o un registro para ese DNI. No se creará otra.");
        Cliente c = new Cliente();
        c.setNombre(externo.nombreCompleto());
        c.setDni(dni); c.setCodigo("CL-" + dni);
        c.setFechaNacimiento(externo.fechaNacimiento());
        c.setCorreo(form.getCorreo().trim().toLowerCase());
        c.setTelefono(form.getTelefono().trim()); c.setDireccion(form.getDireccion().trim());
        c.setEstado("Activo"); c.setTrabajaActualmente(externo.trabajaActualmente());
        c.setEmpresa(externo.trabajaActualmente() ? form.getEmpresa().trim() : "Sin empleo actual");
        c.setCargo(externo.trabajaActualmente() ? form.getCargo().trim() : "No aplica");
        c.setAntiguedadMeses(externo.trabajaActualmente() ? form.getAntiguedadMeses() : 0);
        c.setIngresosMensuales(externo.ingresosMensuales() == null ? java.math.BigDecimal.ZERO : externo.ingresosMensuales());
        c.setDeudasMensuales(externo.deudaMensual() == null ? java.math.BigDecimal.ZERO : externo.deudaMensual());
        c.setGastosMensuales(form.getGastosMensuales());
        c.setPasswordHash(passwordHasher.codificar(form.getClave()));
        clienteRepository.saveAndFlush(c);
    }
    private static String normalizar(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }
    public record Cuenta(String tipo, Long id, String nombre) { }
}
