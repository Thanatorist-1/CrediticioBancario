package utp.edu.com.crediticio.service.solicitud;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;
import utp.edu.com.crediticio.repository.cliente.ClienteRepository;
import utp.edu.com.crediticio.service.evaluacion.EvaluacionAutomaticaService;
import utp.edu.com.crediticio.service.politica.TipoCreditoService;

class SolicitudServiceMultiplesTest {
    @Test void mismoClientePuedeCrearDosSolicitudesYAmbasSeEvaluan() {
        SolicitudRepository repo = mock(SolicitudRepository.class);
        ClienteRepository clientes = mock(ClienteRepository.class);
        EvaluacionAutomaticaService evaluaciones = mock(EvaluacionAutomaticaService.class);
        Cliente cliente = new Cliente(); cliente.setIdCliente(17L); cliente.setEstado("Activo");
        cliente.setPasswordHash("contraseña-hasheada");
        cliente.setGastosMensuales(BigDecimal.valueOf(200));
        cliente.setIngresosMensuales(BigDecimal.valueOf(3000));
        cliente.setDeudasMensuales(BigDecimal.valueOf(100));
        when(clientes.findById(17L)).thenReturn(Optional.of(cliente));
        AtomicLong secuencia = new AtomicLong(40);
        when(repo.saveAndFlush(any(Solicitud.class))).thenAnswer(i -> {
            Solicitud s = i.getArgument(0);
            if (s.getIdSolicitud() == null) s.setIdSolicitud(secuencia.incrementAndGet());
            return s;
        });
        SolicitudService service = new SolicitudService(repo, clientes, evaluaciones, mock(TipoCreditoService.class));
        Solicitud primera = nueva(17L, "Personal", "5000");
        Solicitud segunda = nueva(17L, "Vehicular", "20000");
        service.guardar(primera);
        service.guardar(segunda);
        assertEquals("SOL-000041", primera.getCodigo());
        assertEquals("SOL-000042", segunda.getCodigo());
        assertNotEquals(primera.getIdSolicitud(), segunda.getIdSolicitud());
        verify(evaluaciones, times(2)).evaluarNuevaSolicitud(any(Solicitud.class));
    }
    private static Solicitud nueva(Long id, String tipo, String monto) {
        Solicitud s = new Solicitud();
        s.setIdCliente(id); s.setTipoCredito(tipo); s.setMonto(new BigDecimal(monto));
        s.setPlazoMeses(24); s.setMotivo("Prueba");
        return s;
    }
}
