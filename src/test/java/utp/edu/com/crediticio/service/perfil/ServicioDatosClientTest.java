package utp.edu.com.crediticio.service.perfil;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        server.createContext("/rest/v1/rpc/obtener_perfil_crediticio", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())
                    || !"clave-prueba".equals(exchange.getRequestHeaders().getFirst("apikey"))
                    || !"Bearer clave-prueba".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                responder(exchange, 401, "[]");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            responder(exchange, body.contains("90000001") ? 200 : 200, body.contains("90000001") ? """
                [{"entorno":"SIMULADO","fuente":"Supabase","dni":"90000001",
                  "nombre_completo":"Persona ficticia","nombres":"Persona","apellidos":"Ficticia",
                  "fecha_nacimiento":"1990-01-01","identidad_verificada":true,"persona_activa":true,
                  "trabaja_actualmente":true,"score_crediticio":720,"clasificacion_riesgo":"Bajo",
                  "ingresos_mensuales":3500.50,"deuda_mensual":700.00,"saldo_deuda_pendiente":8400.00,
                  "cuotas_atrasadas":0,"historial_negativo":false,"advertencia":"Simulado"}]
                """ : "[]");
        });
        server.start();
        var perfiles = new SupabasePerfilCrediticioClient(RestClient.builder(),
                "http://localhost:" + server.getAddress().getPort(), "clave-prueba",
                new SimpleClientHttpRequestFactory());
        client = new ServicioDatosClient(perfiles);
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
