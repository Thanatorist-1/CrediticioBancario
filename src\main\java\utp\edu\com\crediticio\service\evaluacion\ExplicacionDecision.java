package utp.edu.com.crediticio.service.evaluacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.solicitud.Solicitud;

/** Explicaciones comprensibles basadas exclusivamente en datos usados al decidir. */
public final class ExplicacionDecision {
    private ExplicacionDecision() {}

    public static String redactar(String resultado, Integer score, int scoreAprobar, int scoreRechazar,
            BigDecimal ingresos, BigDecimal gastos, BigDecimal deudas, BigDecimal cuota,
            BigDecimal dti, BigDecimal disponible, BigDecimal dtiAprobar, BigDecimal dtiRechazar,
            int atrasos, boolean historialNegativo, boolean trabajaActualmente) {
        List<String> factores = new ArrayList<>();
        if ("Rechazado".equals(resultado)) {
            // Dar prioridad a dificultades de pago y atrasos reales, no a un mensaje genérico de puntaje.
            if (atrasos >= 3) factores.add("registras " + atrasos + " cuotas de créditos anteriores atrasadas");
            if (historialNegativo) factores.add("el historial consultado muestra incidencias negativas en pagos anteriores");
            if (ingresos != null && ingresos.signum() > 0 && dti != null && dti.compareTo(dtiRechazar) >= 0)
                factores.add("tus ingresos mensuales de S/ " + dinero(ingresos) + " no son suficientes en relación con tus pagos: ya destinas S/ " + dinero(deudas) + " a deudas y la nueva cuota estimada sería S/ " + dinero(cuota) + " (en conjunto comprometerían el " + pct(dti) + " de tus ingresos)");
            if (disponible != null && disponible.signum() <= 0)
                factores.add("después de gastos mensuales de S/ " + dinero(gastos) + " y deudas de S/ " + dinero(deudas) + ", tus ingresos no dejan dinero disponible para asumir otra cuota");
            if (score != null && score <= scoreRechazar)
                factores.add("el historial de riesgo reflejado en tu puntuación de " + score + " también muestra una probabilidad de incumplimiento mayor a la admitida para este producto");
            return limitado("Tu solicitud fue rechazada porque " + enumerar(factores) + ". La evaluación consideró también el monto y plazo solicitados, que determinan el importe de la cuota mensual estimada.");
        }
        if ("Aprobado".equals(resultado)) {
            return limitado("Tu solicitud fue aprobada porque tus ingresos mensuales de S/ " + dinero(ingresos) +
                    " permiten asumir una cuota estimada de S/ " + dinero(cuota) +
                    " para el préstamo solicitado. Después de gastos y pagos de deudas, quedan S/ " + dinero(disponible) +
                    " disponibles al mes. La carga conjunta de deudas y nueva cuota equivale al " + pct(dti) +
                    " de tus ingresos; además, no se registraron cuotas atrasadas ni incidencias negativas de pago y mantienes actividad laboral. " +
                    "La puntuación crediticia de " + score + " también fue compatible con la política del producto.");
        }
        if (!trabajaActualmente) factores.add("actualmente no registras empleo y necesitamos verificar de dónde provienen tus ingresos");
        if (atrasos > 0) factores.add("figuran " + atrasos + (atrasos == 1 ? " cuota pendiente" : " cuotas pendientes") + " de regularización y debemos revisar sus comprobantes de pago");
        if (disponible != null && cuota != null && disponible.compareTo(cuota) < 0)
            factores.add("los S/ " + dinero(disponible) + " disponibles al mes podrían no alcanzar para la cuota estimada de S/ " + dinero(cuota));
        if (dti != null && dti.compareTo(dtiAprobar) > 0)
            factores.add("la nueva cuota elevaría los compromisos mensuales de deuda al " + pct(dti) + " de tus ingresos");
        if (score != null && score < scoreAprobar) factores.add("necesitamos examinar con mayor detalle tu historial de pagos");
        if (factores.isEmpty()) factores.add("tu situación financiera requiere validar los documentos que respaldan ingresos, gastos y compromisos vigentes");
        return limitado("Tu solicitud está en revisión porque " + enumerar(factores) + ". Por favor, adjunta boletas de pago o sustento de ingresos, recibos de servicios esenciales y comprobantes de deudas que correspondan a tu caso. Un analista revisará los documentos antes de decidir.");
    }

    public static String datosIncompletos() {
        return "Tu solicitud necesita revisión porque no se pudieron verificar todos los datos financieros o de identidad del perfil consultado. Adjunta documentación de identidad, sustento de ingresos y gastos para que el analista compruebe la información antes de decidir.";
    }

    /** Motivo de registros de versiones anteriores: no atribuirles una política nueva retroactivamente. */
    public static String visible(Evaluacion e) {
        if (e == null) return "Todavía no existe una evaluación registrada para esta solicitud.";
        String c = e.getComentario();
        if (c != null && !c.isBlank() && !esAntiguo(c)) return c;
        List<String> datos = new ArrayList<>();
        Solicitud solicitud = e.getSolicitud();
        BigDecimal cuota = solicitud == null || solicitud.getMonto() == null || solicitud.getPlazoMeses() == null
                || solicitud.getPlazoMeses() <= 0 ? null
                : solicitud.getMonto().divide(BigDecimal.valueOf(solicitud.getPlazoMeses()), 2, RoundingMode.HALF_UP);
        if (e.getCuotasAtrasadas() != null && e.getCuotasAtrasadas() > 0)
            datos.add("se registraron " + e.getCuotasAtrasadas() + (e.getCuotasAtrasadas() == 1 ? " cuota atrasada" : " cuotas atrasadas") + " de créditos anteriores");
        if (Boolean.TRUE.equals(e.getHistorialNegativo()))
            datos.add("el historial consultado reflejaba pagos con incidencias negativas");
        if (e.getIngresosExternos() != null && e.getDeudaExterna() != null && cuota != null)
            datos.add("los ingresos eran S/ " + dinero(e.getIngresosExternos()) +
                    " al mes, los pagos de deuda S/ " + dinero(e.getDeudaExterna()) +
                    " y la nueva cuota solicitada se estimaba en S/ " + dinero(cuota));
        if (e.getIngresoDisponible() != null && cuota != null && e.getIngresoDisponible().compareTo(cuota) < 0)
            datos.add("el ingreso que quedaba tras gastos y deudas no alcanzaba para esa cuota estimada");
        if (Boolean.FALSE.equals(e.getTrabajaActualmente())) datos.add("no se registraba empleo vigente");
        if (e.getDti() != null) datos.add("las deudas y la nueva cuota consumirían el " + pct(e.getDti()) + " del ingreso");
        if (e.getPuntaje() != null && datos.isEmpty()) datos.add("el puntaje registrado era " + e.getPuntaje());
        String tipo = switch (e.getResultado() == null ? "" : e.getResultado()) {
            case "Aprobado" -> "La solicitud fue aprobada";
            case "Rechazado" -> "La solicitud fue rechazada";
            default -> "La solicitud se encuentra en revisión";
        };
        return limitado(tipo + ". En la evaluación se observó que " +
            (datos.isEmpty() ? "no se conservaron suficientes indicadores para explicar los factores concretos" : enumerar(datos)) +
            ". Esta explicación resume las cifras históricas almacenadas; la versión anterior no dejó constancia detallada de la causa exacta de la decisión.");
    }

    private static boolean esAntiguo(String c) {
        return c.startsWith("API simulada:") || c.startsWith("Evaluación académica automática")
            || c.startsWith("Perfil externo sin datos financieros") || c.startsWith("API académica:");
    }
    private static String limitado(String s) { return s.length() <= 1000 ? s : s.substring(0, 996) + "..."; }
    private static String dinero(BigDecimal n) { return n == null ? "sin dato" : n.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private static String pct(BigDecimal n) { return n == null ? "sin dato" : n.stripTrailingZeros().toPlainString() + " %"; }
    private static String enumerar(List<String> partes) {
        if (partes.isEmpty()) return "los compromisos actuales dificultan asumir una nueva cuota";
        if (partes.size() == 1) return partes.get(0);
        return String.join(", ", partes.subList(0, partes.size()-1)) + " y " + partes.get(partes.size()-1);
    }
}
