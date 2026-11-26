package utp.edu.com.crediticio.controller.web;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utp.edu.com.crediticio.model.politica.TipoCredito;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;
import utp.edu.com.crediticio.service.politica.TipoCreditoService;
@Controller public class TipoCreditoController {
    private final TipoCreditoService tipos;private final UsuarioRepository usuarios;
    public TipoCreditoController(TipoCreditoService t,UsuarioRepository u){tipos=t;usuarios=u;}
    @GetMapping("/tipos-credito") public String listar(Model model){model.addAttribute("tipos",tipos.listar());return "legal/tipos-credito";}
    @GetMapping("/tipos-credito/nuevo") public String nuevo(Model model){model.addAttribute("tipo",new TipoCredito());return "legal/formulario-tipo";}
    @GetMapping("/tipos-credito/editar/{id}") public String editar(@PathVariable Long id,Model model){
        TipoCredito tipo=tipos.buscar(id);if(tipo==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        model.addAttribute("tipo",tipo);return "legal/formulario-tipo";
    }
    @PostMapping("/tipos-credito/guardar") public String guardar(@Valid @ModelAttribute("tipo") TipoCredito tipo,BindingResult errores,
            Model model,HttpSession sesion,RedirectAttributes flash){
        if(!errores.hasErrors())try {tipos.guardar(tipo,usuarios.findById((Long)sesion.getAttribute("cuentaId")).orElse(null));
            flash.addFlashAttribute("mensaje","Tipo de crédito guardado. Para publicarlo requiere política activa.");return "redirect:/tipos-credito";
        }catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());}
        return "legal/formulario-tipo";
    }
    @PostMapping("/tipos-credito/desactivar/{id}") public String desactivar(@PathVariable Long id,HttpSession sesion,RedirectAttributes flash){
        tipos.desactivar(id,usuarios.findById((Long)sesion.getAttribute("cuentaId")).orElse(null));
        flash.addFlashAttribute("mensaje","Tipo de crédito desactivado y auditado.");return "redirect:/tipos-credito";
    }
}
