package utp.edu.com.crediticio.service.solicitud;

import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;
import utp.edu.com.crediticio.repository.cliente.ClienteRepository;
import utp.edu.com.crediticio.repository.evaluacion.EvaluacionRepository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas y operaciones de negocio sencillas para solicitudes. */
@Service
public class SolicitudService {
    private final SolicitudRepository solicitudRepository;
    private final ClienteRepository clienteRepository;
    private final EvaluacionRepository evaluacionRepository;

    public SolicitudService(SolicitudRepository solicitudRepository, ClienteRepository clienteRepository,
            EvaluacionRepository evaluacionRepository) {
        this.solicitudRepository = solicitudRepository;
        this.clienteRepository = clienteRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    public List<Solicitud> listar() { return solicitudRepository.findAll(); }
    public List<Solicitud> listarConCliente() { return solicitudRepository.findByClienteIsNotNull(); }
    public List<Solicitud> listarPorCliente(Long idCliente) { return solicitudRepository.findByCliente_IdCliente(idCliente); }
    public Solicitud buscar(Long id) { return solicitudRepository.findById(id).orElse(null); }

    public Solicitud guardar(Solicitud solicitud) {
        if (solicitud.getIdSolicitud() != null && (solicitud.getCodigo() == null || solicitud.getCodigo().isBlank())) {
            Solicitud existente = buscar(solicitud.getIdSolicitud());
            if (existente != null) solicitud.setCodigo(existente.getCodigo());
        }
        if (solicitud.getIdCliente() == null) throw new IllegalArgumentException("Seleccione un cliente.");
        Cliente cliente = clienteRepository.findById(solicitud.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("El cliente seleccionado no existe."));
        solicitud.setCliente(cliente);
        if (solicitud.getCodigo() == null || solicitud.getCodigo().isBlank()) {
            solicitud.setCodigo("SOL-%03d".formatted(solicitudRepository.count() + 1));
        }
        if (solicitud.getFecha() == null || solicitud.getFecha().isBlank()) {
            solicitud.setFecha(LocalDate.now().toString());
        }
        if (solicitud.getEstado() == null || solicitud.getEstado().isBlank()) solicitud.setEstado("Pendiente");
        return solicitudRepository.save(solicitud);
    }

    @Transactional
    public Solicitud actualizarEstado(Long id, String estado) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la solicitud."));
        if (!List.of("Pendiente", "En evaluación", "Revisión manual", "Aprobada", "Rechazada")
                .contains(estado)) {
            throw new IllegalArgumentException("El estado indicado no es válido.");
        }
        solicitud.setEstado(estado);
        Solicitud guardada = solicitudRepository.save(solicitud);
        evaluacionRepository.findFirstBySolicitud_IdSolicitud(id).ifPresent(evaluacion -> {
            evaluacion.setResultado(switch (estado) {
                case "Aprobada" -> "Aprobado";
                case "Rechazada" -> "Rechazado";
                default -> estado;
            });
            evaluacionRepository.save(evaluacion);
        });
        return guardada;
    }

    public void eliminar(Long id) { solicitudRepository.deleteById(id); }
}


