package utp.edu.com.crediticio.service.solicitud;

import java.io.IOException;
import java.util.Base64;
import utp.edu.com.crediticio.model.solicitud.DocumentoJsonPayload;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import utp.edu.com.crediticio.model.solicitud.DocumentoSolicitud;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.solicitud.DocumentoSolicitudRepository;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;

@Service
public class DocumentoSolicitudService {
    private static final long MAXIMO_BYTES = 5L * 1024 * 1024;
    private final DocumentoSolicitudRepository documentoRepository;
    private final SolicitudRepository solicitudRepository;

    public DocumentoSolicitudService(DocumentoSolicitudRepository documentoRepository,
            SolicitudRepository solicitudRepository) {
        this.documentoRepository = documentoRepository;
        this.solicitudRepository = solicitudRepository;
    }

    public void validar(List<MultipartFile> archivos) {
        if (archivos == null) return;
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) continue;
            String tipo=archivo.getContentType();
            if (archivo.getSize() > MAXIMO_BYTES) throw new IllegalArgumentException("Cada archivo debe pesar máximo 5 MB.");
            if (tipo == null || !List.of("application/pdf", "image/jpeg", "image/png").contains(tipo)) {
                throw new IllegalArgumentException("Solo se permiten archivos PDF, JPG o PNG.");
            }
        }
    }

    @Transactional
    public List<DocumentoSolicitud> guardar(Long idSolicitud, List<MultipartFile> archivos) {
        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("No existe la solicitud."));
        validar(archivos);
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) continue;
            String tipo = archivo.getContentType();
            try {
                String nombre = archivo.getOriginalFilename() == null ? "documento" : archivo.getOriginalFilename();
                nombre = nombre.replaceAll("[^A-Za-z0-9._-]", "_");
                if (nombre.length() > 180) nombre = nombre.substring(nombre.length() - 180);
                DocumentoSolicitud documento = new DocumentoSolicitud();
                documento.setSolicitud(solicitud);
                documento.setCategoria("OTRO");
                documento.setEtapa("SOLICITUD");
                documento.setNombreArchivo(nombre);
                documento.setTipoContenido(tipo);
                documento.setTamanoBytes(archivo.getSize());
                documento.setContenido(archivo.getBytes());
                documentoRepository.save(documento);
            } catch (IOException e) {
                throw new IllegalArgumentException("No se pudo leer el archivo cargado.");
            }
        }
        return documentoRepository.findBySolicitud_IdSolicitudOrderByFechaSubidaDesc(idSolicitud);
    }

    @Transactional
    public List<DocumentoSolicitud> guardarJson(Long idSolicitud, List<DocumentoJsonPayload> archivos) {
        return guardarJson(idSolicitud, archivos, "SOLICITUD");
    }

    @Transactional
    public List<DocumentoSolicitud> guardarJson(Long idSolicitud, List<DocumentoJsonPayload> archivos, String etapa) {
        if (!List.of("SOLICITUD", "REVISION").contains(etapa))
            throw new IllegalArgumentException("Etapa documental no permitida.");
        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new IllegalArgumentException("No existe la solicitud."));
        if (archivos == null || archivos.isEmpty()) return listar(idSolicitud);
        var anteriores = documentoRepository.findBySolicitud_IdSolicitudOrderByFechaSubidaDesc(idSolicitud);
        long total = anteriores.stream().mapToLong(DocumentoSolicitud::getTamanoBytes).sum();
        if (anteriores.size() + archivos.size() > 20)
            throw new IllegalArgumentException("Máximo 20 documentos acumulados por solicitud.");
        for (DocumentoJsonPayload archivo : archivos) {
            if (archivo == null || archivo.contenidoBase64() == null || archivo.nombreArchivo() == null)
                throw new IllegalArgumentException("Datos del documento incompletos.");
            String categoria = archivo.categoria() == null || archivo.categoria().isBlank() ? "OTRO" : archivo.categoria();
            if (!List.of("BOLETA_SUELDO", "RECIBO_SERVICIOS", "RENTA", "ESTADO_CUENTA", "OTRO").contains(categoria))
                throw new IllegalArgumentException("Categoría documental no permitida.");
            String tipo = archivo.tipoContenido();
            if (!List.of("application/pdf", "image/jpeg", "image/png").contains(tipo))
                throw new IllegalArgumentException("Solo se permiten archivos PDF, JPG o PNG.");
            if (archivo.contenidoBase64().length() > (MAXIMO_BYTES * 4 / 3 + 8))
                throw new IllegalArgumentException("Cada archivo debe pesar máximo 5 MB.");
            byte[] contenido;
            try { contenido = Base64.getDecoder().decode(archivo.contenidoBase64()); }
            catch (IllegalArgumentException error) { throw new IllegalArgumentException("Archivo Base64 inválido."); }
            if (contenido.length == 0 || contenido.length > MAXIMO_BYTES)
                throw new IllegalArgumentException("Cada archivo debe pesar entre 1 byte y 5 MB.");
            total += contenido.length;
            if (total > 20L * 1024 * 1024)
                throw new IllegalArgumentException("Los documentos no pueden superar 20 MB en total.");
            boolean pdf = contenido.length >= 5 && contenido[0] == '%' && contenido[1] == 'P' && contenido[2] == 'D' && contenido[3] == 'F' && contenido[4] == '-';
            boolean jpg = contenido.length >= 3 && (contenido[0] & 0xff) == 0xff && (contenido[1] & 0xff) == 0xd8 && (contenido[2] & 0xff) == 0xff;
            boolean png = contenido.length >= 8 && (contenido[0] & 0xff) == 0x89 && contenido[1] == 'P' && contenido[2] == 'N' && contenido[3] == 'G';
            if (!("application/pdf".equals(tipo) && pdf || "image/jpeg".equals(tipo) && jpg || "image/png".equals(tipo) && png))
                throw new IllegalArgumentException("El archivo no corresponde con su tipo declarado.");
            String nombre = archivo.nombreArchivo().replaceAll("[^A-Za-z0-9._-]", "_");
            if (nombre.length() > 180) nombre = nombre.substring(nombre.length() - 180);
            if (nombre.isBlank()) nombre = "documento";
            DocumentoSolicitud documento = new DocumentoSolicitud();
            documento.setSolicitud(solicitud);
            documento.setCategoria(categoria);
            documento.setEtapa(etapa);
            documento.setNombreArchivo(nombre);
            documento.setTipoContenido(tipo);
            documento.setTamanoBytes(contenido.length);
            documento.setContenido(contenido);
            documentoRepository.save(documento);
        }
        return documentoRepository.findBySolicitud_IdSolicitudOrderByFechaSubidaDesc(idSolicitud);
    }

    @Transactional(readOnly = true)
    public List<DocumentoSolicitud> listar(Long idSolicitud) {
        return documentoRepository.findBySolicitud_IdSolicitudOrderByFechaSubidaDesc(idSolicitud);
    }

    @Transactional(readOnly = true)
    public DocumentoSolicitud buscar(Long id) { return documentoRepository.findById(id).orElse(null); }
}
