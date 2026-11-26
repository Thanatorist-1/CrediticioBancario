package utp.edu.com.crediticio.controller.api;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import utp.edu.com.crediticio.model.politica.*;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;
import utp.edu.com.crediticio.service.politica.*;
@RestController @RequestMapping("/api/v1") public class ConfiguracionApiController {
    private final PoliticaService politicas;private final TipoCreditoService tipos;
    private final AuditoriaConfiguracionService auditoria;private final UsuarioRepository usuarios;
    public ConfiguracionApiController(PoliticaService p,TipoCreditoService t,AuditoriaConfiguracionService a,UsuarioRepository u){
        politicas=p;tipos=t;auditoria=a;usuarios=u;}
    private Usuario actor(HttpSession s){return usuarios.findById((Long)s.getAttribute("cuentaId")).orElse(null);}
    @GetMapping("/tipos-credito") public List<TipoCredito> tipos(){return tipos.listar();}
    @PostMapping("/tipos-credito") @ResponseStatus(HttpStatus.CREATED)
    public TipoCredito crearTipo(@Valid @RequestBody TipoCredito datos,HttpSession s){datos.setId(null);return tipos.guardar(datos,actor(s));}
    @PutMapping("/tipos-credito/{id}") public TipoCredito editarTipo(@PathVariable Long id,@Valid @RequestBody TipoCredito datos,HttpSession s){datos.setId(id);return tipos.guardar(datos,actor(s));}
    @PostMapping("/tipos-credito/{id}/desactivar") public void desactivarTipo(@PathVariable Long id,HttpSession s){tipos.desactivar(id,actor(s));}
    @GetMapping("/auditoria-configuracion") public List<AuditoriaConfiguracion> auditoria(){return auditoria.listar();}
}
