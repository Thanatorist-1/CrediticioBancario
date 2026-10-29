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

/** Datos recibidos desde el formulario público de solicitud. */
public class SolicitudPortalForm {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres.")
    private String nombreCompleto;

    @NotBlank(message = "El DNI es obligatorio.")
    @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener exactamente 8 dígitos.")
    private String dni;

    @NotBlank(message = "Selecciona el tipo de crédito.")
    @Pattern(regexp = "^(Personal|Vehicular|Emprendimiento|Educativo|Hipotecario)$", message = "Selecciona un tipo de crédito válido.")
    private String tipoCredito;

    @NotNull(message = "Ingresa el monto solicitado.")
    @DecimalMin(value = "500.00", message = "El monto mínimo es S/ 500.")
    @DecimalMax(value = "200000.00", message = "El monto máximo es S/ 200,000.")
    @Digits(integer = 6, fraction = 2, message = "El monto debe tener hasta 6 enteros y 2 decimales.")
    private BigDecimal montoSolicitado;

    @NotNull(message = "Selecciona el plazo.")
    @Min(value = 6, message = "El plazo mínimo es 6 meses.")
    @Max(value = 120, message = "El plazo máximo es 120 meses.")
    private Integer plazo;

    @NotBlank(message = "Describe el motivo de la solicitud.")
    @Size(max = 250, message = "El motivo no puede superar 250 caracteres.")
    private String motivoCredito;

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String tipoCredito) { this.tipoCredito = tipoCredito; }
    public BigDecimal getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(BigDecimal montoSolicitado) { this.montoSolicitado = montoSolicitado; }
    public Integer getPlazo() { return plazo; }
    public void setPlazo(Integer plazo) { this.plazo = plazo; }
    public String getMotivoCredito() { return motivoCredito; }
    public void setMotivoCredito(String motivoCredito) { this.motivoCredito = motivoCredito; }
}
