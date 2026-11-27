package utp.edu.com.crediticio.controller.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.service.cliente.ClienteService;
import utp.edu.com.crediticio.service.evaluacion.EvaluacionService;
import utp.edu.com.crediticio.service.externo.ConsultaExternaService;
import utp.edu.com.crediticio.service.politica.TipoCreditoService;
import utp.edu.com.crediticio.service.solicitud.DocumentoSolicitudService;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;

class ClienteControllerTest {
    @Test
    void redirigeAlLoginCuandoNoHayClienteEnSesion() {
        ClienteController controller = controller();
        HttpSession session = mock(HttpSession.class);

        String vista = controller.portalCliente(session, new ExtendedModelMap(), null);

        assertThat(vista).isEqualTo("redirect:/login");
    }

    @Test
    void rechazaDetalleDeSolicitudQueNoPerteneceAlClienteEnSesion() {
        ClienteController controller = controller();
        HttpSession session = mock(HttpSession.class);
        Solicitud solicitud = new Solicitud();
        Cliente titular = new Cliente();
        titular.setIdCliente(2L);
        solicitud.setCliente(titular);
        when(session.getAttribute("cuentaId")).thenReturn(1L);
        when(solicitudService.buscar(10L)).thenReturn(solicitud);

        assertThatThrownBy(() -> controller.detalleSolicitudCliente(10L, session, new ExtendedModelMap()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private final ClienteService clienteService = mock(ClienteService.class);
    private final SolicitudService solicitudService = mock(SolicitudService.class);

    private ClienteController controller() {
        return new ClienteController(clienteService, solicitudService,
                mock(DocumentoSolicitudService.class), mock(ConsultaExternaService.class),
                mock(EvaluacionService.class), mock(TipoCreditoService.class));
    }
}
