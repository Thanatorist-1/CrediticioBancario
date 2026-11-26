package utp.edu.com.crediticio.model.politica;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.math.BigDecimal;

/** Entidad JPA de una política crediticia. */
@Entity
@Table(name = "politicas")
public class Politica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String codigo;
    @NotBlank(message = "Selecciona un producto crediticio.")
    @Size(max = 100, message = "El producto no puede superar 100 caracteres.")
    private String producto;
    @Column(name = "version_politica")
    @NotBlank(message = "La versión es obligatoria.")
    @Size(max = 12, message = "La versión no puede superar 12 caracteres.")
    private String version;
    @NotBlank(message = "La vigencia es obligatoria.")
    @Size(max = 30, message = "La vigencia no puede superar 30 caracteres.")
    private String vigencia;
    @Pattern(regexp = "^(Activa|Borrador|Inactiva)?$", message = "Selecciona un estado válido.")
    private String estado;
    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 400, message = "La descripción no puede superar 400 caracteres.")
    private String descripcion;
    @Min(value = 300, message = "El puntaje mínimo para aprobar debe ser al menos 300.")
    private Integer puntajeMinimoAprobar = 650;
    @Min(value = 0, message = "El puntaje máximo para rechazar no puede ser negativo.")
    private Integer puntajeMaximoRechazo = 499;
    @DecimalMin(value = "1.0", message = "El DTI de aprobación debe ser mayor a 0.")
    @DecimalMax(value = "100.0", message = "El DTI no puede superar 100 %.")
    private BigDecimal dtiMaximoAprobacion = new BigDecimal("35.0");
    @DecimalMin(value = "1.0", message = "El DTI de rechazo debe ser mayor a 0.")
    @DecimalMax(value = "100.0", message = "El DTI no puede superar 100 %.")
    private BigDecimal dtiMinimoRechazo = new BigDecimal("50.0");

    @DecimalMin("500.0")
    private BigDecimal montoMinimo = new BigDecimal("500");
    @DecimalMin("500.0")
    private BigDecimal montoMaximo = new BigDecimal("200000");
    @Min(1)
    private Integer plazoMinimoMeses = 6;
    @Min(1)
    private Integer plazoMaximoMeses = 120;
    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal tasaAnualReferencial = BigDecimal.ZERO;
    private Boolean requiereGarantia = false;
    private Boolean requiereEmpleoActivo = true;
    @Min(18)
    private Integer edadMaximaAnios;

    public Politica() { }

    public Politica(String codigo, String producto, String version, String vigencia,
                    String estado, String descripcion) {
        this.codigo = codigo;
        this.producto = producto;
        this.version = version;
        this.vigencia = vigencia;
        this.estado = estado;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getVigencia() { return vigencia; }
    public void setVigencia(String vigencia) { this.vigencia = vigencia; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getPuntajeMinimoAprobar() { return puntajeMinimoAprobar; }
    public void setPuntajeMinimoAprobar(Integer valor) { this.puntajeMinimoAprobar = valor; }
    public Integer getPuntajeMaximoRechazo() { return puntajeMaximoRechazo; }
    public void setPuntajeMaximoRechazo(Integer valor) { this.puntajeMaximoRechazo = valor; }
    public BigDecimal getDtiMaximoAprobacion() { return dtiMaximoAprobacion; }
    public void setDtiMaximoAprobacion(BigDecimal valor) { this.dtiMaximoAprobacion = valor; }
    public BigDecimal getDtiMinimoRechazo() { return dtiMinimoRechazo; }
    public void setDtiMinimoRechazo(BigDecimal valor) { this.dtiMinimoRechazo = valor; }
    public BigDecimal getMontoMinimo(){return montoMinimo;}
    public void setMontoMinimo(BigDecimal x){montoMinimo=x;}
    public BigDecimal getMontoMaximo(){return montoMaximo;}
    public void setMontoMaximo(BigDecimal x){montoMaximo=x;}
    public Integer getPlazoMinimoMeses(){return plazoMinimoMeses;}
    public void setPlazoMinimoMeses(Integer x){plazoMinimoMeses=x;}
    public Integer getPlazoMaximoMeses(){return plazoMaximoMeses;}
    public void setPlazoMaximoMeses(Integer x){plazoMaximoMeses=x;}
    public BigDecimal getTasaAnualReferencial(){return tasaAnualReferencial;}
    public void setTasaAnualReferencial(BigDecimal x){tasaAnualReferencial=x;}
    public Boolean getRequiereGarantia(){return requiereGarantia;}
    public void setRequiereGarantia(Boolean x){requiereGarantia=x;}
    public Boolean getRequiereEmpleoActivo(){return requiereEmpleoActivo;}
    public void setRequiereEmpleoActivo(Boolean x){requiereEmpleoActivo=x;}
    public Integer getEdadMaximaAnios(){return edadMaximaAnios;}
    public void setEdadMaximaAnios(Integer x){edadMaximaAnios=x;}
}

