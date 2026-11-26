package utp.edu.com.crediticio.model.solicitud;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Exclusivamente los datos de la nueva solicitud; el perfil y las finanzas se obtienen del servidor/API externa. */
public class SolicitudPortalForm {
    @NotBlank(message = "Selecciona el tipo de crédito.")
    @Size(max = 40, message = "El código del crédito es demasiado largo.")
    private String tipoCredito;

    @NotNull(message = "Ingresa el monto solicitado.")
    @DecimalMin(value = "500.00", message = "El monto mínimo es S/ 500.")
    @DecimalMax(value = "10000000.00", message = "El monto máximo absoluto es S/ 10,000,000.")
    @Digits(integer = 8, fraction = 2, message = "El monto debe tener hasta 6 enteros y 2 decimales.")
    private BigDecimal montoSolicitado;

    @NotNull(message = "Selecciona el plazo.")
    @Min(value = 1, message = "El plazo mínimo absoluto es 1 mes.")
    @Max(value = 360, message = "El plazo máximo absoluto es 360 meses.")
    private Integer plazo;

    @NotBlank(message = "Describe brevemente el motivo de la solicitud.")
    @Size(max = 250, message = "El motivo no puede superar 250 caracteres.")
    private String motivoCredito;

    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String v) { tipoCredito = v; }
    public BigDecimal getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(BigDecimal v) { montoSolicitado = v; }
    public Integer getPlazo() { return plazo; }
    public void setPlazo(Integer v) { plazo = v; }
    public String getMotivoCredito() { return motivoCredito; }
    public void setMotivoCredito(String v) { motivoCredito = v; }
}
