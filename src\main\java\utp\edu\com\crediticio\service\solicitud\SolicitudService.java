package utp.edu.com.crediticio.service.solicitud;

import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;
import utp.edu.com.crediticio.repository.cliente.ClienteRepository;
import utp.edu.com.crediticio.service.evaluacion.EvaluacionAutomaticaService;
import utp.edu.com.crediticio.service.politica.TipoCreditoService;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas y operaciones de negocio sencillas para solicitudes. */
@Service
public class SolicitudService {
    private final SolicitudRepository solicitudRepository;
    private final ClienteRepository clienteRepository;
    private final EvaluacionAutomaticaService evaluacionAutomaticaService;
    private final TipoCreditoService tipos;

    public SolicitudService(SolicitudRepository solicitudRepository, ClienteRepository clienteRepository,
            EvaluacionAutomaticaService evaluacionAutomaticaService, TipoCreditoService tipos) {
        this.solicitudRepository = solicitudRepository;
        this.clienteRepository = clienteRepository;
        this.evaluacionAutomaticaService = evaluacionAutomaticaService;
        this.tipos=tipos;
    }

    public List<Solicitud> listar() { return solicitudRepository.findAll(); }
    public List<Solicitud> listarConCliente() { return solicitudRepository.findByClienteIsNotNull(); }
    public List<Solicitud> listarPorCliente(Long idCliente) {
        return solicitudRepository.findByCliente_IdCliente(idCliente).stream()
                .sorted(java.util.Comparator.comparing(Solicitud::getIdSolicitud).reversed()).toList();
    }
    public Solicitud buscar(Long id) { return solicitudRepository.findById(id).orElse(null); }

    @Transactional
    public Solicitud guardar(Solicitud solicitud) {
        boolean nueva = solicitud.getIdSolicitud() == null;
        Solicitud existente = nueva ? null : solicitudRepository.findById(solicitud.getIdSolicitud()).orElse(null);
        if (!nueva && existente == null) throw new IllegalArgumentException("No se encontró la solicitud para actualizar.");
        if (solicitud.getIdSolicitud() != null && (solicitud.getCodigo() == null || solicitud.getCodigo().isBlank())) {
            if (existente != null) solicitud.setCodigo(existente.getCodigo());
        }
        if (solicitud.getIdCliente() == null || solicitud.getIdCliente() <= 0) {
            throw new IllegalArgumentException("Seleccione un cliente válido.");
        }
        Cliente cliente = clienteRepository.findById(solicitud.getIdCliente())
                .orElseThrow(() -> new IllegalArgumentException("El cliente seleccionado no existe."));
        solicitud.setCliente(cliente);
        if (nueva && (cliente.getPasswordHash() == null || cliente.getPasswordHash().isBlank()
                || !"Activo".equalsIgnoreCase(cliente.getEstado()))) {
            throw new IllegalArgumentException("El cliente debe registrarse y activar su cuenta antes de solicitar crédito.");
        }
        if (nueva) tipos.exigirDisponible(solicitud.getTipoCredito());
        if (nueva) {
            // No se aceptan ingresos/deudas/gastos suministrados en la petición JSON
            // para alterar el resultado automático. La API externa prevalece en el evaluador.
            solicitud.setIngresosMensuales(cliente.getIngresosMensuales());
            solicitud.setDeudasMensuales(cliente.getDeudasMensuales());
            solicitud.setGastosMensuales(cliente.getGastosMensuales());
        } else {
            if (solicitud.getIngresosMensuales() == null) solicitud.setIngresosMensuales(cliente.getIngresosMensuales());
            if (solicitud.getGastosMensuales() == null) solicitud.setGastosMensuales(cliente.getGastosMensuales());
            if (solicitud.getDeudasMensuales() == null) solicitud.setDeudasMensuales(cliente.getDeudasMensuales());
        }
        // El código de cada nueva solicitud se asigna desde su ID real después del INSERT.
        // count()+1 podía duplicarse al borrar solicitudes existentes.
        if (solicitud.getFecha() == null || solicitud.getFecha().isBlank()) {
            solicitud.setFecha(LocalDate.now().toString());
        }
        solicitud.setEstado(nueva ? "Pendiente" : existente.getEstado());
        Solicitud guardada = solicitudRepository.saveAndFlush(solicitud);
        if (nueva) {
            guardada.setCodigo("SOL-%06d".formatted(guardada.getIdSolicitud()));
            guardada = solicitudRepository.saveAndFlush(guardada);
            evaluacionAutomaticaService.evaluarNuevaSolicitud(guardada);
        }
        return guardada;
    }

    public void eliminar(Long id) { solicitudRepository.deleteById(id); }
}


