package utp.edu.com.crediticio.service.perfil;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import utp.edu.com.crediticio.service.externo.PerfilCrediticioExterno;

/**
 * Consulta los perfiles académicos directamente en Supabase. La clave de servicio
 * pertenece al servidor y nunca se entrega al navegador.
 */
@Service
public class SupabasePerfilCrediticioClient {
    private final RestClient restClient;
    private final String serviceRoleKey;

    public SupabasePerfilCrediticioClient(RestClient.Builder restClientBuilder,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey) {
        this(restClientBuilder, supabaseUrl, serviceRoleKey, crearRequestFactory());
    }

    SupabasePerfilCrediticioClient(RestClient.Builder restClientBuilder, String supabaseUrl,
            String serviceRoleKey, SimpleClientHttpRequestFactory requestFactory) {
        this.restClient = restClientBuilder.baseUrl(supabaseUrl)
                .requestFactory(requestFactory).build();
        this.serviceRoleKey = serviceRoleKey;
    }

    public Optional<PerfilCrediticioExterno> consultarPorDni(String dni) {
        JsonNode respuesta = restClient.post()
                .uri("/rest/v1/rpc/obtener_perfil_crediticio")
                .header("apikey", serviceRoleKey)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("p_dni", dni))
                .retrieve()
                .body(JsonNode.class);

        if (respuesta == null || !respuesta.isArray() || respuesta.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(convertir(respuesta.get(0)));
    }

    private PerfilCrediticioExterno convertir(JsonNode perfil) {
        return new PerfilCrediticioExterno(
                texto(perfil, "entorno"),
                texto(perfil, "fuente"),
                texto(perfil, "dni"),
                texto(perfil, "nombre_completo"),
                texto(perfil, "nombres"),
                texto(perfil, "apellidos"),
                fecha(perfil, "fecha_nacimiento"),
                booleano(perfil, "identidad_verificada"),
                booleano(perfil, "persona_activa"),
                booleano(perfil, "trabaja_actualmente"),
                entero(perfil, "score_crediticio"),
                texto(perfil, "clasificacion_riesgo"),
                decimal(perfil, "ingresos_mensuales"),
                decimal(perfil, "deuda_mensual"),
                decimal(perfil, "saldo_deuda_pendiente"),
                entero(perfil, "cuotas_atrasadas"),
                booleano(perfil, "historial_negativo"),
                texto(perfil, "advertencia"));
    }

    private static SimpleClientHttpRequestFactory crearRequestFactory() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return requestFactory;
    }

    private static String texto(JsonNode nodo, String nombre) {
        JsonNode valor = nodo.path(nombre);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    private static Boolean booleano(JsonNode nodo, String nombre) {
        JsonNode valor = nodo.path(nombre);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asBoolean();
    }

    private static Integer entero(JsonNode nodo, String nombre) {
        JsonNode valor = nodo.path(nombre);
        return valor.isInt() ? valor.intValue() : null;
    }

    private static BigDecimal decimal(JsonNode nodo, String nombre) {
        JsonNode valor = nodo.path(nombre);
        return valor.isNumber() ? valor.decimalValue() : null;
    }

    private static LocalDate fecha(JsonNode nodo, String nombre) {
        String valor = texto(nodo, nombre);
        return valor == null || valor.isBlank() ? null : LocalDate.parse(valor);
    }
}
