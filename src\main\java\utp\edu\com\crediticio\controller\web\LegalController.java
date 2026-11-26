package utp.edu.com.crediticio.controller.web;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utp.edu.com.crediticio.model.politica.Politica;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.service.politica.*;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;

@Controller
public class LegalController {
    private final PoliticaService politicas; private final TipoCreditoService tipos;
    private final AuditoriaConfiguracionService auditoria; private final UsuarioRepository usuarios;
    public LegalController(PoliticaService p, TipoCreditoService t, AuditoriaConfiguracionService a, UsuarioRepository u){
        politicas=p;tipos=t;auditoria=a;usuarios=u;
    }
    @GetMapping("/politicas") public String listar(Model model, HttpSession sesion){
        model.addAttribute("politicas",politicas.listar());model.addAttribute("esAdmin",esAdmin(sesion));return "legal/politicas";
    }
    @GetMapping("/politicas/nueva") public String nueva(Model model){
        model.addAttribute("politica",new Politica());model.addAttribute("tipos",tipos.listar());return "legal/nueva-politica";
    }
    @GetMapping("/politicas/editar/{id}") public String editar(@PathVariable Long id, Model model){
        var p=politicas.buscar(id);if(p==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        model.addAttribute("politica",p);model.addAttribute("tipos",tipos.listar());return "legal/nueva-politica";
    }
    @PostMapping("/politicas/guardar") public String guardar(@Valid @ModelAttribute("politica") Politica form, BindingResult errores,
            Model model, RedirectAttributes flash, HttpSession sesion){
        if(!errores.hasErrors()) try {politicas.guardar(form,actor(sesion));flash.addFlashAttribute("mensaje","Política registrada y auditada.");return "redirect:/politicas";}
        catch(IllegalArgumentException e){model.addAttribute("error",e.getMessage());}
        model.addAttribute("tipos",tipos.listar());return "legal/nueva-politica";
    }
    @PostMapping("/politicas/desactivar/{id}") public String desactivar(@PathVariable Long id,HttpSession sesion,RedirectAttributes flash){
        politicas.desactivar(id,actor(sesion));flash.addFlashAttribute("mensaje","Política desactivada y auditada.");return "redirect:/politicas";
    }
    @GetMapping("/politicas/auditoria") public String auditoria(Model model){
        model.addAttribute("eventos",auditoria.listar());return "legal/auditoria";
    }
    private boolean esAdmin(HttpSession sesion){return "Administrador".equals(sesion.getAttribute("rolActual"));}
    private Usuario actor(HttpSession s){return usuarios.findById((Long)s.getAttribute("cuentaId")).orElse(null);}
}
