package utp.edu.com.crediticio.controller.api;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import utp.edu.com.crediticio.service.politica.ActorConfiguracionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.politica.Politica;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.model.solicitud.SolicitudApiRespuesta;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.exception.api.RecursoNoEncontradoException;
import utp.edu.com.crediticio.service.cliente.ClienteService;
import utp.edu.com.crediticio.service.evaluacion.EvaluacionService;
import utp.edu.com.crediticio.service.politica.PoliticaService;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;
import utp.edu.com.crediticio.service.usuario.UsuarioService;

/** API REST versionada, consumida mediante fetch por los formularios Thymeleaf. */
@RestController
@RequestMapping("/api/v1")
public class ApiRestController {
    private final ClienteService clientes;
    private final SolicitudService solicitudes;
    private final EvaluacionService evaluaciones;
    private final PoliticaService politicas;
    private final UsuarioService usuarios;
    private final ActorConfiguracionService actor;

    public ApiRestController(ClienteService clientes, SolicitudService solicitudes,
            EvaluacionService evaluaciones, PoliticaService politicas, UsuarioService usuarios, ActorConfiguracionService actor) {
        this.clientes = clientes;
        this.solicitudes = solicitudes;
        this.evaluaciones = evaluaciones;
        this.politicas = politicas;
        this.usuarios = usuarios;
        this.actor=actor;
    }

    @GetMapping("/clientes") public List<Cliente> listarClientes() { return clientes.listar(); }
    @GetMapping("/clientes/{id}") public Cliente verCliente(@PathVariable Long id) { return requerido(clientes.buscar(id), "Cliente"); }
    @PostMapping("/clientes") @ResponseStatus(HttpStatus.CREATED)
    public Cliente crearCliente(@Valid @RequestBody Cliente cliente) { cliente.setIdCliente(null); return clientes.guardar(cliente); }
    @PutMapping("/clientes/{id}") public Cliente actualizarCliente(@PathVariable Long id, @Valid @RequestBody Cliente cliente) {
        requerido(clientes.buscar(id), "Cliente"); cliente.setIdCliente(id); return clientes.guardar(cliente);
    }
    @DeleteMapping("/clientes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCliente(@PathVariable Long id) { requerido(clientes.buscar(id), "Cliente"); clientes.eliminar(id); }

    @GetMapping("/solicitudes")
    public List<SolicitudApiRespuesta> listarSolicitudes(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estado) {
        return solicitudes.listarConCliente().stream()
                .filter(s -> coincide(s.getCodigo(), texto)
                        || coincide(s.getCliente() == null ? null : s.getCliente().getNombre(), texto)
                        || coincide(s.getCliente() == null ? null : s.getCliente().getDni(), texto))
                .filter(s -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(s.getEstado()))
                .map(SolicitudApiRespuesta::desde)
                .toList();
    }
    @GetMapping("/solicitudes/{id}") public Solicitud verSolicitud(@PathVariable Long id) { return requerido(solicitudes.buscar(id), "Solicitud"); }
    @PostMapping("/solicitudes") @ResponseStatus(HttpStatus.CREATED)
    public Solicitud crearSolicitud(@Valid @RequestBody Solicitud solicitud) { solicitud.setIdSolicitud(null); return solicitudes.guardar(solicitud); }
    @PutMapping("/solicitudes/{id}") public Solicitud actualizarSolicitud(@PathVariable Long id, @Valid @RequestBody Solicitud solicitud) {
        requerido(solicitudes.buscar(id), "Solicitud"); solicitud.setIdSolicitud(id); return solicitudes.guardar(solicitud);
    }
    @DeleteMapping("/solicitudes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarSolicitud(@PathVariable Long id) { requerido(solicitudes.buscar(id), "Solicitud"); solicitudes.eliminar(id); }

    @GetMapping("/evaluaciones") public List<Evaluacion> listarEvaluaciones() { return evaluaciones.listar(); }
    @GetMapping("/evaluaciones/{id}") public Evaluacion verEvaluacion(@PathVariable Long id) { return requerido(evaluaciones.buscar(id), "Evaluación"); }
    @PostMapping("/evaluaciones") @ResponseStatus(HttpStatus.CREATED)
    public Evaluacion crearEvaluacion(@Valid @RequestBody Evaluacion evaluacion) { evaluacion.setIdEvaluacion(null); return evaluaciones.guardar(evaluacion); }
    @PutMapping("/evaluaciones/{id}") public Evaluacion actualizarEvaluacion(@PathVariable Long id, @Valid @RequestBody Evaluacion evaluacion) {
        requerido(evaluaciones.buscar(id), "Evaluación"); evaluacion.setIdEvaluacion(id); return evaluaciones.guardar(evaluacion);
    }
    @PostMapping("/evaluaciones/{id}/decision") public Evaluacion decidir(@PathVariable Long id, @RequestBody DecisionApi decision) {
        return evaluaciones.registrarDecision(id, decision.resultado(), decision.comentario());
    }
    public record RequerimientoDocumental(String mensaje) {}
    @PostMapping("/evaluaciones/{id}/solicitar-documentos")
    public java.util.Map<String, String> solicitarDocumentos(@PathVariable Long id,
            @RequestBody RequerimientoDocumental payload) {
        evaluaciones.solicitarDocumentacion(id, payload == null ? null : payload.mensaje());
        return java.util.Map.of("mensaje", "El requerimiento documental ya es visible para el cliente.");
    }
    @DeleteMapping("/evaluaciones/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvaluacion(@PathVariable Long id) { requerido(evaluaciones.buscar(id), "Evaluación"); evaluaciones.eliminar(id); }

    @GetMapping("/politicas") public List<Politica> listarPoliticas() { return politicas.listar(); }
    @PostMapping("/politicas") @ResponseStatus(HttpStatus.CREATED)
    public Politica crearPolitica(@Valid @RequestBody Politica politica, HttpSession sesion) { politica.setId(null); return politicas.guardar(politica,actor.admin(sesion)); }
    @PutMapping("/politicas/{id}") public Politica actualizarPolitica(@PathVariable Long id, @Valid @RequestBody Politica politica, HttpSession sesion) {
        requerido(politicas.buscar(id), "Política"); politica.setId(id); return politicas.guardar(politica,actor.admin(sesion));
    }
    @DeleteMapping("/politicas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPolitica(@PathVariable Long id, HttpSession sesion) { requerido(politicas.buscar(id), "Política"); politicas.desactivar(id,actor.admin(sesion)); }

    @GetMapping("/personal") public List<Usuario> listarPersonal() { return usuarios.listar(); }
    @PostMapping("/personal") @ResponseStatus(HttpStatus.CREATED)
    public Usuario crearPersonal(@Valid @RequestBody Usuario usuario) { usuario.setIdUsuario(null); return usuarios.guardar(usuario); }
    @PutMapping("/personal/{id}") public Usuario actualizarPersonal(@PathVariable Long id, @Valid @RequestBody Usuario usuario) {
        requerido(usuarios.buscar(id), "Usuario"); usuario.setIdUsuario(id); return usuarios.guardar(usuario);
    }
    @DeleteMapping("/personal/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPersonal(@PathVariable Long id) { requerido(usuarios.buscar(id), "Usuario"); usuarios.eliminar(id); }


    private static <T> T requerido(T valor, String entidad) {
        if (valor == null) throw new RecursoNoEncontradoException(entidad + " no encontrado.");
        return valor;
    }
    private static boolean coincide(String campo, String filtro) {
        return filtro == null || filtro.isBlank() || campo != null && campo.toLowerCase().contains(filtro.trim().toLowerCase());
    }
    public record DecisionApi(String resultado, String comentario) { }
}

