package utp.edu.com.crediticio.service.externo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato de respuesta del servicio ACADÉMICO, no RENIEC ni SBS. */
public record PerfilCrediticioExterno(
        String entorno, String fuente, String dni, String nombreCompleto, String nombres, String apellidos,
        LocalDate fechaNacimiento, boolean identidadVerificada, boolean personaActiva, boolean trabajaActualmente,
        Integer scoreCrediticio, String clasificacionRiesgo, BigDecimal ingresosMensuales,
        BigDecimal deudaMensual, BigDecimal saldoDeudaPendiente, Integer cuotasAtrasadas, boolean historialNegativo,
        String advertencia) { }
