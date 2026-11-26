package utp.edu.com.crediticio.service.evaluacion;

import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.evaluacion.EvaluacionRepository;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;
import utp.edu.com.crediticio.repository.solicitud.DocumentoSolicitudRepository;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas y operaciones de negocio sencillas para evaluaciones. */
@Service
public class EvaluacionService {
    private final EvaluacionRepository evaluacionRepository;
    private final SolicitudRepository solicitudRepository;
    private final DocumentoSolicitudRepository documentos;

    public EvaluacionService(EvaluacionRepository evaluacionRepository, SolicitudRepository solicitudRepository,
            DocumentoSolicitudRepository documentos) {
        this.evaluacionRepository = evaluacionRepository;
        this.solicitudRepository = solicitudRepository;
        this.documentos = documentos;
    }

    public List<Evaluacion> listar() { return evaluacionRepository.findAll(); }
    public Evaluacion buscar(Long id) { return evaluacionRepository.findById(id).orElse(null); }
    public Evaluacion buscarPorSolicitud(Long idSolicitud) { return evaluacionRepository.findFirstBySolicitud_IdSolicitud(idSolicitud).orElse(null); }

    public Evaluacion guardar(Evaluacion evaluacion) {
        if (evaluacion.getIdEvaluacion() != null && (evaluacion.getCodigo() == null || evaluacion.getCodigo().isBlank())) {
            Evaluacion existente = buscar(evaluacion.getIdEvaluacion());
            if (existente != null) evaluacion.setCodigo(existente.getCodigo());
        }
        if (evaluacion.getIdSolicitud() == null || evaluacion.getIdSolicitud() <= 0) {
            throw new IllegalArgumentException("Seleccione una solicitud válida.");
        }
        Solicitud solicitud = solicitudRepository.findById(evaluacion.getIdSolicitud())
                .orElseThrow(() -> new IllegalArgumentException("La solicitud seleccionada no existe."));
        evaluacion.setSolicitud(solicitud);
        if (evaluacion.getCodigo() == null || evaluacion.getCodigo().isBlank()) {
            evaluacion.setCodigo("EV-%03d".formatted(evaluacionRepository.count() + 1));
        }
        if (evaluacion.getResultado() == null || evaluacion.getResultado().isBlank()) evaluacion.setResultado("Revisión manual");
        if (evaluacion.getNivelRiesgo() == null || evaluacion.getNivelRiesgo().isBlank()) evaluacion.setNivelRiesgo("Por evaluar");
        return evaluacionRepository.save(evaluacion);
    }

    @Transactional
    public Evaluacion registrarDecision(Long id, String resultado, String comentario) {
        Evaluacion evaluacion = evaluacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la evaluación."));
        if (resultado == null || !List.of("Aprobado", "Rechazado").contains(resultado)) {
            throw new IllegalArgumentException("La revisión manual solo permite aprobar o rechazar.");
        }
        String estadoActual = evaluacion.getResultado();
        if (estadoActual == null || !List.of("Revisión manual").contains(estadoActual)) {
            throw new IllegalArgumentException("Esta evaluación ya tiene una decisión registrada.");
        }
        if (comentario == null || comentario.trim().length() < 25 || comentario.trim().length() > 1000)
            throw new IllegalArgumentException("Describe con al menos 25 caracteres los motivos concretos: ingresos, pagos, documentos revisados y conclusión.");
        String causa = comentario.trim().toLowerCase(java.util.Locale.ROOT);
        if (java.util.List.of("ingres", "deud", "cuot", "gasto", "atras", "historial",
                "emple", "document", "boleta", "pago", "capacidad", "renta", "alquiler",
                "recibo", "compromiso").stream().noneMatch(causa::contains))
            throw new IllegalArgumentException("Explica una causa concreta (ingresos, cuotas, atrasos, documentos, gastos o capacidad de pago), no solo el puntaje.");
        if (evaluacion.getSolicitud() == null || documentos.countBySolicitud_IdSolicitudAndEtapa(
                evaluacion.getSolicitud().getIdSolicitud(), "REVISION") == 0)
            throw new IllegalArgumentException("Antes de decidir, espera la documentación de revisión enviada por el cliente.");
        evaluacion.setResultado(resultado);
        evaluacion.setComentario(comentario.trim());
        Solicitud solicitud = evaluacion.getSolicitud();
        if (solicitud != null) {
            solicitud.setEstado(switch (resultado) {
                case "Aprobado" -> "Aprobada";
                case "Rechazado" -> "Rechazada";
                default -> "Revisión manual";
            });
            solicitudRepository.saveAndFlush(solicitud);
        }
        // La solicitud y la evaluación se confirman juntas por @Transactional.
        return evaluacionRepository.saveAndFlush(evaluacion);
    }

    @Transactional
    public void solicitarDocumentacion(Long id, String requerimiento) {
        Evaluacion evaluacion = evaluacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la evaluación."));
        if (!"Revisión manual".equals(evaluacion.getResultado()))
            throw new IllegalArgumentException("Solo se pueden solicitar documentos durante una revisión manual.");
        if (requerimiento == null || requerimiento.trim().length() < 15 || requerimiento.trim().length() > 800)
            throw new IllegalArgumentException("Explica en 15 a 800 caracteres qué documentos necesita el analista.");
        evaluacion.setRequerimientoDocumental(requerimiento.trim());
        evaluacionRepository.save(evaluacion);
    }

    public void eliminar(Long id) { evaluacionRepository.deleteById(id); }
}


