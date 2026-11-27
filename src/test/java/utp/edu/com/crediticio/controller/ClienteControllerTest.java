package utp.edu.com.crediticio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.perfil.PerfilFicticioAcademico;
import utp.edu.com.crediticio.model.perfil.ResultadoPerfilFicticio;
import utp.edu.com.crediticio.service.cliente.ClienteService;
import utp.edu.com.crediticio.service.perfil.ServicioDatosClient;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;

class ClienteControllerTest {
    @Test
    void agregaPerfilFicticioSoloComoDatoDeVista() {
        ClienteService clienteService = org.mockito.Mockito.mock(ClienteService.class);
        SolicitudService solicitudService = org.mockito.Mockito.mock(SolicitudService.class);
        ServicioDatosClient servicioDatosClient = org.mockito.Mockito.mock(ServicioDatosClient.class);
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1L);
        cliente.setDni("90000001");
        ResultadoPerfilFicticio perfil = ResultadoPerfilFicticio.disponible(
                new PerfilFicticioAcademico("SIMULADO", "Persona ficticia", true, 720,
                        "BAJO", null, null, 0, false));
        when(clienteService.buscar(1L)).thenReturn(cliente);
        when(solicitudService.listarPorCliente(1L)).thenReturn(List.of());
        when(servicioDatosClient.consultarPorDni("90000001")).thenReturn(perfil);
        ClienteController controller = new ClienteController(clienteService, solicitudService, servicioDatosClient);
        Model model = new ExtendedModelMap();

        String vista = controller.perfil(1L, model, null);

        assertThat(vista).isEqualTo("clientes/perfil-cliente");
        assertThat(model.getAttribute("cliente")).isSameAs(cliente);
        assertThat(model.getAttribute("perfilFicticio")).isSameAs(perfil);
        verify(servicioDatosClient).consultarPorDni("90000001");
    }
}
