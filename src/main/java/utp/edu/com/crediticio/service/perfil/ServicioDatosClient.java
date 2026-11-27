package utp.edu.com.crediticio.service.perfil;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import utp.edu.com.crediticio.model.perfil.PerfilFicticioAcademico;
import utp.edu.com.crediticio.model.perfil.ResultadoPerfilFicticio;

@Service
public class ServicioDatosClient {
    private static final Pattern DNI_FICTICIO = Pattern.compile("9[0-9]{7}");
    private final SupabasePerfilCrediticioClient perfiles;

    public ServicioDatosClient(SupabasePerfilCrediticioClient perfiles) {
        this.perfiles = perfiles;
    }

    public ResultadoPerfilFicticio consultarPorDni(String dni) {
        if (dni == null || !DNI_FICTICIO.matcher(dni).matches()) {
            return ResultadoPerfilFicticio.noAplica();
        }
        try {
            return perfiles.consultarPorDni(dni)
                    .map(this::convertir)
                    .map(ResultadoPerfilFicticio::disponible)
                    .orElseGet(ResultadoPerfilFicticio::noEncontrado);
        } catch (RestClientException exception) {
            return ResultadoPerfilFicticio.noDisponible();
        }
    }

    private PerfilFicticioAcademico convertir(utp.edu.com.crediticio.service.externo.PerfilCrediticioExterno respuesta) {
        return new PerfilFicticioAcademico(
                respuesta.entorno(),
                respuesta.nombreCompleto(),
                respuesta.personaActiva(),
                respuesta.scoreCrediticio(),
                respuesta.clasificacionRiesgo(),
                respuesta.ingresosMensuales(),
                respuesta.deudaMensual(),
                respuesta.cuotasAtrasadas(),
                respuesta.historialNegativo());
    }
}
