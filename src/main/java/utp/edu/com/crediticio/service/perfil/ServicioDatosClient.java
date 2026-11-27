package utp.edu.com.crediticio.service.perfil;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import utp.edu.com.crediticio.model.perfil.PerfilFicticioAcademico;
import utp.edu.com.crediticio.model.perfil.ResultadoPerfilFicticio;

@Service
public class ServicioDatosClient {
    private static final Pattern DNI_FICTICIO = Pattern.compile("9[0-9]{7}");
    private final RestClient restClient;

    public ServicioDatosClient(RestClient.Builder restClientBuilder,
            @Value("${servicio.datos.url}") String servicioDatosUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = restClientBuilder
                .baseUrl(servicioDatosUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public ResultadoPerfilFicticio consultarPorDni(String dni) {
        if (dni == null || !DNI_FICTICIO.matcher(dni).matches()) {
            return ResultadoPerfilFicticio.noAplica();
        }
        try {
            JsonNode respuesta = restClient.get()
                    .uri("/api/v1/consultas/{dni}", dni)
                    .retrieve()
                    .body(JsonNode.class);
            return respuesta == null ? ResultadoPerfilFicticio.noDisponible()
                    : ResultadoPerfilFicticio.disponible(convertir(respuesta));
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode().value() == 404
                    ? ResultadoPerfilFicticio.noEncontrado()
                    : ResultadoPerfilFicticio.noDisponible();
        } catch (RestClientException exception) {
            return ResultadoPerfilFicticio.noDisponible();
        }
    }

    private PerfilFicticioAcademico convertir(JsonNode respuesta) {
        return new PerfilFicticioAcademico(
                texto(respuesta, "entorno"),
                texto(respuesta, "identidad", "nombreCompleto", "nombre"),
                booleano(respuesta, "personaActiva", "activo"),
                entero(respuesta, "score"),
                texto(respuesta, "riesgo", "nivelRiesgo"),
                decimal(respuesta, "ingresos", "ingresosMensuales"),
                decimal(respuesta, "deuda", "deudaTotal"),
                entero(respuesta, "cuotasAtrasadas", "cuotasVencidas"),
                booleano(respuesta, "historialNegativo", "tieneHistorialNegativo"));
    }

    private JsonNode valor(JsonNode respuesta, String... nombres) {
        for (String nombre : nombres) {
            JsonNode valor = respuesta.findValue(nombre);
            if (valor != null && !valor.isNull()) return valor;
        }
        return null;
    }

    private String texto(JsonNode respuesta, String... nombres) {
        JsonNode valor = valor(respuesta, nombres);
        return valor == null || valor.isContainerNode() ? "No informado" : valor.asText("No informado");
    }

    private Integer entero(JsonNode respuesta, String... nombres) {
        JsonNode valor = valor(respuesta, nombres);
        return valor != null && valor.canConvertToInt() ? valor.intValue() : null;
    }

    private BigDecimal decimal(JsonNode respuesta, String... nombres) {
        JsonNode valor = valor(respuesta, nombres);
        return valor != null && valor.isNumber() ? valor.decimalValue() : null;
    }

    private Boolean booleano(JsonNode respuesta, String... nombres) {
        JsonNode valor = valor(respuesta, nombres);
        return valor != null && valor.isBoolean() ? valor.booleanValue() : null;
    }
}
