package utp.edu.com.crediticio.service.perfil;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import utp.edu.com.crediticio.model.perfil.ResultadoPerfilFicticio;

class ServicioDatosClientTest {
    private static HttpServer server;
    private static ServicioDatosClient client;

    @BeforeAll
    static void iniciarServidor() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/consultas/90000001", exchange -> responder(exchange,
                200, """
                {"entorno":"SIMULADO","identidad":"Persona ficticia","personaActiva":true,
                 "score":720,"riesgo":"BAJO","ingresos":3500.50,"deuda":700.00,
                 "cuotasAtrasadas":0,"historialNegativo":false}
                """));
        server.start();
        client = new ServicioDatosClient(RestClient.builder(),
                "http://localhost:" + server.getAddress().getPort());
    }

    @AfterAll
    static void detenerServidor() {
        server.stop(0);
    }

    @Test
    void devuelvePerfilSoloParaDniFicticioValido() {
        ResultadoPerfilFicticio resultado = client.consultarPorDni("90000001");

        assertThat(resultado.consultaAplicable()).isTrue();
        assertThat(resultado.perfil()).isNotNull();
        assertThat(resultado.perfil().entorno()).isEqualTo("SIMULADO");
        assertThat(resultado.perfil().score()).isEqualTo(720);
    }

    @Test
    void omiteLaConsultaParaDniNoFicticio() {
        ResultadoPerfilFicticio resultado = client.consultarPorDni("12345678");

        assertThat(resultado.consultaAplicable()).isFalse();
        assertThat(resultado.perfil()).isNull();
    }

    @Test
    void controlaPerfilFicticioNoEncontrado() {
        ResultadoPerfilFicticio resultado = client.consultarPorDni("90000002");

        assertThat(resultado.consultaAplicable()).isTrue();
        assertThat(resultado.perfil()).isNull();
        assertThat(resultado.mensaje()).contains("No existe");
    }

    private static void responder(com.sun.net.httpserver.HttpExchange exchange,
            int status, String body) throws IOException {
        byte[] respuesta = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, respuesta.length);
        exchange.getResponseBody().write(respuesta);
        exchange.close();
    }
}
