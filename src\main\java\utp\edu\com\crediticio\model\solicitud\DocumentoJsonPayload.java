package utp.edu.com.crediticio.model.solicitud;
/** Documento enviado como JSON Base64; categoría opcional en solicitudes nuevas. */
public record DocumentoJsonPayload(String nombreArchivo, String tipoContenido, String contenidoBase64, String categoria) {}
