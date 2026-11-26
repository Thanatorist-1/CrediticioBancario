package utp.edu.com.crediticio.model.politica;
import jakarta.persistence.*;
import java.time.LocalDateTime;
/** Registro de cambios inmutable y consultable por el administrador. */
@Entity
@Table(name = "auditoria_configuracion")
public class AuditoriaConfiguracion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=32) private String entidad;
    @Column(nullable=false) private Long registroId;
    @Column(nullable=false,length=50) private String accion;
    @Column(nullable=false) private Long actorId;
    @Column(nullable=false,length=180) private String actorCorreo;
    @Column(nullable=false) private LocalDateTime fecha;
    @Column(columnDefinition="TEXT") private String valoresAntes;
    @Column(columnDefinition="TEXT") private String valoresDespues;
    public AuditoriaConfiguracion() {}
    public AuditoriaConfiguracion(String entidad, Long registroId, String accion, Long actorId,
            String actorCorreo, String antes, String despues) {
        this.entidad=entidad; this.registroId=registroId; this.accion=accion;
        this.actorId=actorId; this.actorCorreo=actorCorreo;
        this.valoresAntes=antes; this.valoresDespues=despues; this.fecha=LocalDateTime.now();
    }
    public Long getId(){return id;} public String getEntidad(){return entidad;}
    public Long getRegistroId(){return registroId;} public String getAccion(){return accion;}
    public Long getActorId(){return actorId;} public String getActorCorreo(){return actorCorreo;}
    public LocalDateTime getFecha(){return fecha;} public String getValoresAntes(){return valoresAntes;}
    public String getValoresDespues(){return valoresDespues;}
}
