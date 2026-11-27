package utp.edu.com.crediticio.model.perfil;

import java.math.BigDecimal;

/** Datos exclusivamente académicos suministrados por el servicio externo simulado. */
public record PerfilFicticioAcademico(
        String entorno,
        String identidad,
        Boolean personaActiva,
        Integer score,
        String riesgo,
        BigDecimal ingresos,
        BigDecimal deuda,
        Integer cuotasAtrasadas,
        Boolean historialNegativo) {
}
