package utp.edu.com.crediticio.service.externo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Consulta el microservicio en cada registro y cada solicitud nueva; nunca inventa perfiles. */
@Service
public class ConsultaExternaService {
    private final RestClient client;

    public ConsultaExternaService(@Value("${servicio.datos-simulados.url:http://localhost:8081}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public PerfilCrediticioExterno consultar(String dni) {
        if (dni == null || !dni.matches("9[0-9]{7}"))
            throw new IllegalArgumentException("Solo se permiten DNI ficticios de 8 dígitos que comiencen por 9.");
        try {
            PerfilCrediticioExterno p = client.get()
                    .uri("/api/v1/consultas/{dni}", dni).accept(MediaType.APPLICATION_JSON)
                    .retrieve().body(PerfilCrediticioExterno.class);
            if (p == null || !dni.equals(p.dni()) || !"SIMULADO".equalsIgnoreCase(p.entorno()))
                throw new ServicioExternoNoDisponibleException();
            return p;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("El DNI ficticio no figura en la API externa. No se puede registrar la cuenta.");
        } catch (HttpClientErrorException ex) {
            throw new ServicioExternoNoDisponibleException();
        } catch (ResourceAccessException ex) {
            throw new ServicioExternoNoDisponibleException();
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new ServicioExternoNoDisponibleException();
        }
    }

    public PerfilCrediticioExterno verificarPersonaActiva(String dni) {
        PerfilCrediticioExterno p = consultar(dni);
        if (!p.identidadVerificada() || !p.personaActiva())
            throw new IllegalArgumentException("El perfil ficticio no está activo o no tiene identidad verificada en la API.");
        return p;
    }
}
