package utp.edu.com.crediticio.service.externo;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import utp.edu.com.crediticio.service.perfil.SupabasePerfilCrediticioClient;

/** Consulta Supabase en cada registro y cada solicitud nueva; nunca inventa perfiles. */
@Service
public class ConsultaExternaService {
    private final SupabasePerfilCrediticioClient perfiles;

    public ConsultaExternaService(SupabasePerfilCrediticioClient perfiles) {
        this.perfiles = perfiles;
    }

    public PerfilCrediticioExterno consultar(String dni) {
        if (dni == null || !dni.matches("9[0-9]{7}"))
            throw new IllegalArgumentException("Solo se permiten DNI ficticios de 8 dígitos que comiencen por 9.");
        try {
            PerfilCrediticioExterno p = perfiles.consultarPorDni(dni).orElseThrow(
                    () -> new IllegalArgumentException("El DNI ficticio no figura en Supabase. No se puede registrar la cuenta."));
            if (!dni.equals(p.dni()) || !"SIMULADO".equalsIgnoreCase(p.entorno()))
                throw new ServicioExternoNoDisponibleException();
            return p;
        } catch (RestClientException ex) {
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
