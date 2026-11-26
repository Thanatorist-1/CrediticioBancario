package utp.edu.com.crediticio.controller.api;

import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import utp.edu.com.crediticio.model.solicitud.DocumentoJsonPayload;
import jakarta.servlet.http.HttpSession;
import utp.edu.com.crediticio.exception.api.AccesoNoAutorizadoException;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;
import utp.edu.com.crediticio.model.solicitud.DocumentoSolicitud;
import utp.edu.com.crediticio.exception.api.RecursoNoEncontradoException;
import utp.edu.com.crediticio.service.solicitud.DocumentoSolicitudService;

@RestController
@RequestMapping("/api/v1/solicitudes/{idSolicitud}/documentos")
public class DocumentoApiController {
    private final DocumentoSolicitudService servicio;
    private final SolicitudService solicitudes;
    public DocumentoApiController(DocumentoSolicitudService servicio, SolicitudService solicitudes) {
        this.servicio = servicio; this.solicitudes = solicitudes;
    }
    private void comprobarAcceso(Long idSolicitud, HttpSession session) {
        if (!"CLIENTE".equals(session.getAttribute("tipoCuenta"))) return;
        var solicitud = solicitudes.buscar(idSolicitud);
        if (solicitud == null || solicitud.getCliente() == null ||
                !solicitud.getCliente().getIdCliente().equals(session.getAttribute("cuentaId")))
            throw new AccesoNoAutorizadoException("No puede acceder a los documentos de otra solicitud.");
    }

    @GetMapping public List<DocumentoSolicitud> listar(@PathVariable Long idSolicitud, HttpSession session) {
        comprobarAcceso(idSolicitud, session); return servicio.listar(idSolicitud);
    }
    public record DocumentosJson(List<DocumentoJsonPayload> documentos) {}
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<DocumentoSolicitud> subir(@PathVariable Long idSolicitud,
            @RequestBody DocumentosJson payload, HttpSession session) {
        if (!"CLIENTE".equals(session.getAttribute("tipoCuenta")))
            throw new AccesoNoAutorizadoException("Solo el cliente puede aportar documentación adicional.");
        comprobarAcceso(idSolicitud, session);
        var solicitud = solicitudes.buscar(idSolicitud);
        if (solicitud == null || !"Revisión manual".equals(solicitud.getEstado()))
            throw new IllegalArgumentException("Los documentos adicionales solo se aceptan mientras la solicitud está en revisión manual.");
        if (payload == null || payload.documentos() == null || payload.documentos().isEmpty())
            throw new IllegalArgumentException("Debes seleccionar al menos un documento.");
        return servicio.guardarJson(idSolicitud, payload.documentos(), "REVISION");
    }
    @GetMapping("/{idDocumento}/descargar")
    public ResponseEntity<ByteArrayResource> descargar(@PathVariable Long idDocumento, @PathVariable Long idSolicitud, HttpSession session) {
        comprobarAcceso(idSolicitud, session);
        DocumentoSolicitud documento = servicio.buscar(idDocumento);
        if (documento == null || documento.getSolicitud() == null || !documento.getSolicitud().getIdSolicitud().equals(idSolicitud))
            throw new RecursoNoEncontradoException("Documento no encontrado.");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(documento.getTipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documento.getNombreArchivo().replaceAll("[^A-Za-z0-9._-]", "_") + "\"")
                .body(new ByteArrayResource(documento.getContenido()));
    }
}

