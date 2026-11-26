package utp.edu.com.crediticio.controller.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import utp.edu.com.crediticio.exception.api.AccesoNoAutorizadoException;
import utp.edu.com.crediticio.exception.api.RecursoNoEncontradoException;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.solicitud.DocumentoJsonPayload;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.model.solicitud.SolicitudPortalForm;
import utp.edu.com.crediticio.service.cliente.ClienteService;
import utp.edu.com.crediticio.service.evaluacion.EvaluacionService;
import utp.edu.com.crediticio.service.solicitud.DocumentoSolicitudService;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;

@RestController
@RequestMapping("/api/v1")
public class SolicitudJsonController {
    private final SolicitudService solicitudes;
    private final ClienteService clientes;
    private final DocumentoSolicitudService documentos;
    private final EvaluacionService evaluaciones;
    public SolicitudJsonController(SolicitudService solicitudes, ClienteService clientes,
           DocumentoSolicitudService documentos, EvaluacionService evaluaciones) {
        this.solicitudes = solicitudes; this.clientes = clientes; this.documentos = documentos;
        this.evaluaciones = evaluaciones;
    }

    public record SolicitudConDocumentos(@NotNull @Valid Solicitud solicitud, List<DocumentoJsonPayload> documentos) {}
    public record PortalConDocumentos(@NotNull @Valid SolicitudPortalForm solicitud, List<DocumentoJsonPayload> documentos) {}

    @PostMapping("/solicitudes/con-documentos") @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> crear(@Valid @RequestBody SolicitudConDocumentos payload) {
        Solicitud solicitud = payload.solicitud();
        solicitud.setIdSolicitud(null);
        Solicitud guardada = solicitudes.guardar(solicitud);
        documentos.guardarJson(guardada.getIdSolicitud(), payload.documentos());
        return Map.of("id", guardada.getIdSolicitud(), "mensaje", "Solicitud registrada correctamente.");
    }

    @PutMapping("/solicitudes/con-documentos/{id}")
    @Transactional
    public Map<String, Object> actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudConDocumentos payload) {
        if (solicitudes.buscar(id) == null) throw new RecursoNoEncontradoException("Solicitud no encontrada.");
        Solicitud solicitud = payload.solicitud();
        solicitud.setIdSolicitud(id);
        Solicitud guardada = solicitudes.guardar(solicitud);
        documentos.guardarJson(guardada.getIdSolicitud(), payload.documentos());
        return Map.of("id", guardada.getIdSolicitud(), "mensaje", "Solicitud actualizada correctamente.");
    }

    @PostMapping("/portal/solicitudes") @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> enviarDesdePortal(@Valid @RequestBody PortalConDocumentos payload, HttpSession session) {
        if (!"CLIENTE".equals(session.getAttribute("tipoCuenta")))
            throw new AccesoNoAutorizadoException("Esta operación requiere una cuenta de cliente.");
        Object cuenta = session.getAttribute("cuentaId");
        if (!(cuenta instanceof Long idCliente)) throw new AccesoNoAutorizadoException("Inicia sesión como cliente.");
        Cliente cliente = clientes.buscar(idCliente);
        if (cliente == null) throw new RecursoNoEncontradoException("Cliente no encontrado.");
        SolicitudPortalForm datos = payload.solicitud();
        if (datos.getPlazo() == null || datos.getPlazo() < 1 || datos.getPlazo() > 360)
            throw new IllegalArgumentException("El plazo debe estar entre 1 y 360 meses.");
        Solicitud solicitud = new Solicitud();
        solicitud.setIdCliente(cliente.getIdCliente());
        solicitud.setTipoCredito(datos.getTipoCredito());
        solicitud.setMonto(datos.getMontoSolicitado());
        solicitud.setPlazoMeses(datos.getPlazo());
        solicitud.setMotivo(datos.getMotivoCredito().trim());
        // El cliente nunca envía ingresos, gastos o deudas para decidir el crédito.
        // SolicitudService y EvaluacionAutomaticaService cargan datos locales y externos confiables.
        solicitud.setEstado("Pendiente");
        // No hay límite de una sola solicitud por cliente. Cada alta es independiente.
        // Los adjuntos siguen siendo opcionales por compatibilidad con versiones previas.
        Solicitud guardada = solicitudes.guardar(solicitud);
        documentos.guardarJson(guardada.getIdSolicitud(), payload.documentos());
        var evaluacion = evaluaciones.buscarPorSolicitud(guardada.getIdSolicitud());
        return Map.of("id", guardada.getIdSolicitud(), "estado", guardada.getEstado(),
                "resultado", evaluacion == null ? "Revisión manual" : evaluacion.getResultado(),
                "mensaje", "Solicitud registrada y evaluada usando la API simulada.");
    }
}
