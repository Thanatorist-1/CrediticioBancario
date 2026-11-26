package utp.edu.com.crediticio.controller.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** Pasarela local para consultar perfiles ficticios sin CORS ni exponer claves. */
@Controller
public class ConsultaSimuladaController {
    private final RestClient cliente;

    public ConsultaSimuladaController(@Value("${servicio.datos-simulados.url:http://localhost:8081}") String url) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);
        cliente = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    @GetMapping("/consultas-simuladas")
    public String pagina() {
        return "evaluaciones/consulta-simulada";
    }

    @ResponseBody
    @GetMapping(value = "/api/v1/consultas-simuladas/{dni}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> consultar(@PathVariable String dni) {
        if (!dni.matches("9[0-9]{7}")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "DNI_FICTICIO_INVALIDO",
                    "mensaje", "Utiliza un DNI ficticio de 8 dígitos que empiece por 9."));
        }
        try {
            String respuesta = cliente.get().uri("/api/v1/consultas/{dni}", dni)
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
            if (respuesta == null || respuesta.isBlank()) {
                return ResponseEntity.status(502).body(Map.of(
                        "error", "RESPUESTA_VACIA", "mensaje", "La API simulada no devolvió información."));
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(respuesta);
        } catch (HttpClientErrorException ex) {
            String contenido = ex.getResponseBodyAsString();
            return ResponseEntity.status(ex.getStatusCode()).contentType(MediaType.APPLICATION_JSON)
                    .body(contenido.isBlank() ? "{\"error\":\"CONSULTA_RECHAZADA\"}" : contenido);
        } catch (ResourceAccessException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "SERVICIO_NO_DISPONIBLE",
                    "mensaje", "No se pudo acceder al servicio simulado en el puerto 8081. Comprueba que esté iniciado."));
        } catch (Exception ex) {
            return ResponseEntity.status(502).body(Map.of(
                    "error", "ERROR_CONSULTA",
                    "mensaje", "El servicio simulado no pudo completar la consulta."));
        }
    }
}
