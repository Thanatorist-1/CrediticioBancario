package utp.edu.com.crediticio.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Valida credenciales del personal mediante Supabase Auth, sin exponer claves al navegador. */
@Service
public class SupabaseAuthClient {
    private final RestClient restClient;
    private final String anonKey;

    public SupabaseAuthClient(RestClient.Builder builder,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.anon-key}") String anonKey) {
        this.restClient = builder.baseUrl(supabaseUrl).build();
        this.anonKey = anonKey;
    }

    public Optional<CuentaSupabase> autenticar(String email, String password) {
        try {
            JsonNode respuesta = restClient.post()
                    .uri("/auth/v1/token?grant_type=password")
                    .header("apikey", anonKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + anonKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(JsonNode.class);
            if (respuesta == null || respuesta.path("access_token").asText().isBlank()) return Optional.empty();
            JsonNode usuario = respuesta.path("user");
            String correo = usuario.path("email").asText("").trim().toLowerCase();
            String rol = usuario.path("app_metadata").path("rol").asText("");
            if (correo.isBlank() || !rolValido(rol)) return Optional.empty();
            return Optional.of(new CuentaSupabase(correo, rol));
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }

    private static boolean rolValido(String rol) {
        return "Analista".equals(rol) || "Supervisor".equals(rol) || "Administrador".equals(rol);
    }

    public record CuentaSupabase(String correo, String rol) { }
}
