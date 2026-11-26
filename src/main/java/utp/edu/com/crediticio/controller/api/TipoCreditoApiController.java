package utp.edu.com.crediticio.controller.api;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import utp.edu.com.crediticio.model.politica.TipoCredito;
import utp.edu.com.crediticio.service.politica.*;
@RestController
@RequestMapping("/api/v1/tipos-credito")
public class TipoCreditoApiController {
    private final TipoCreditoService tipos;private final ActorConfiguracionService actores;
    public TipoCreditoApiController(TipoCreditoService t,ActorConfiguracionService a){tipos=t;actores=a;}
    @GetMapping public List<TipoCredito> listar(HttpSession s){actores.admin(s);return tipos.listar();}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TipoCredito crear(@Valid @RequestBody TipoCredito tipo,HttpSession s){tipo.setId(null);return tipos.guardar(tipo,actores.admin(s));}
    @PutMapping("/{id}")
    public TipoCredito actualizar(@PathVariable Long id,@Valid @RequestBody TipoCredito tipo,HttpSession s){tipo.setId(id);return tipos.guardar(tipo,actores.admin(s));}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id,HttpSession s){tipos.desactivar(id,actores.admin(s));}
}
