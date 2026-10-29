package utp.edu.com.crediticio.service.evaluacion;

import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.evaluacion.EvaluacionRepository;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas y operaciones de negocio sencillas para evaluaciones. */
@Service
public class EvaluacionService {
    private final EvaluacionRepository evaluacionRepository;
    private final SolicitudRepository solicitudRepository;

    public EvaluacionService(EvaluacionRepository evaluacionRepository, SolicitudRepository solicitudRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.solicitudRepository = solicitudRepository;
    }

    public List<Evaluacion> listar() { return evaluacionRepository.findAll(); }
    public Evaluacion buscar(Long id) { return evaluacionRepository.findById(id).orElse(null); }

    public Evaluacion guardar(Evaluacion evaluacion) {
        if (evaluacion.getIdEvaluacion() != null && (evaluacion.getCodigo() == null || evaluacion.getCodigo().isBlank())) {
            Evaluacion existente = buscar(evaluacion.getIdEvaluacion());
            if (existente != null) evaluacion.setCodigo(existente.getCodigo());
        }
        if (evaluacion.getIdSolicitud() == null) throw new IllegalArgumentException("Seleccione una solicitud.");
        Solicitud solicitud = solicitudRepository.findById(evaluacion.getIdSolicitud())
                .orElseThrow(() -> new IllegalArgumentException("La solicitud seleccionada no existe."));
        evaluacion.setSolicitud(solicitud);
        if (evaluacion.getCodigo() == null || evaluacion.getCodigo().isBlank()) {
            evaluacion.setCodigo("EV-%03d".formatted(evaluacionRepository.count() + 1));
        }
        if (evaluacion.getResultado() == null || evaluacion.getResultado().isBlank()) evaluacion.setResultado("Pendiente");
        if (evaluacion.getNivelRiesgo() == null || evaluacion.getNivelRiesgo().isBlank()) evaluacion.setNivelRiesgo("Por evaluar");
        return evaluacionRepository.save(evaluacion);
    }

    @Transactional
    public Evaluacion registrarDecision(Long id, String resultado, String comentario) {
        Evaluacion evaluacion = evaluacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la evaluación."));
        if (!List.of("Aprobado", "Rechazado", "Revisión manual").contains(resultado)) {
            throw new IllegalArgumentException("La decisión indicada no es válida.");
        }
        evaluacion.setResultado(resultado);
        if (comentario != null && !comentario.isBlank()) evaluacion.setComentario(comentario.trim());
        Solicitud solicitud = evaluacion.getSolicitud();
        if (solicitud != null) {
            solicitud.setEstado(switch (resultado) {
                case "Aprobado" -> "Aprobada";
                case "Rechazado" -> "Rechazada";
                default -> "Revisión manual";
            });
            solicitudRepository.save(solicitud);
        }
        return evaluacionRepository.save(evaluacion);
    }

    public void eliminar(Long id) { evaluacionRepository.deleteById(id); }
}


