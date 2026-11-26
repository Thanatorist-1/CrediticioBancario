package utp.edu.com.crediticio.model.politica;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/** Catálogo configurable, separado de las políticas que fijan sus umbrales. */
@Entity
@Table(name = "tipos_credito")
public class TipoCredito {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{2,39}$", message = "Código de 3 a 40 caracteres sin espacios.")
    @Column(nullable = false, unique = true, length = 40, updatable = false)
    private String codigo;
    @NotBlank @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
    @Size(max = 350) @Column(length = 350)
    private String descripcion;
    @Column(nullable = false)
    private boolean activo = false;
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    @PrePersist public void alCrear() { fechaCreacion = LocalDateTime.now(); }
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getCodigo(){return codigo;} public void setCodigo(String v){codigo=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public boolean isActivo(){return activo;} public void setActivo(boolean v){activo=v;}
    public LocalDateTime getFechaCreacion(){return fechaCreacion;}
}
