package utp.edu.com.crediticio.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utp.edu.com.crediticio.model.politica.Politica;
import utp.edu.com.crediticio.service.politica.PoliticaService;

@Controller
public class LegalController {
    private final PoliticaService politicaService;

    public LegalController(PoliticaService politicaService) {
        this.politicaService = politicaService;
    }

    @GetMapping("/politicas")
    public String mostrarPoliticas(Model model) {
        model.addAttribute("politicas", politicaService.listar());
        return "legal/politicas";
    }

    @GetMapping("/politicas/nueva")
    public String nuevaPolitica(Model model) {
        model.addAttribute("politica", new Politica(null, null, null, null, "Borrador", null));
        return "legal/nueva-politica";
    }

    @GetMapping("/politicas/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        Politica politica = politicaService.buscar(id);
        if (politica == null) {
            flash.addFlashAttribute("error", "No se encontró la política.");
            return "redirect:/politicas";
        }
        model.addAttribute("politica", politica);
        return "legal/nueva-politica";
    }

    @PostMapping("/politicas/guardar")
    public String guardarPolitica(@Valid @ModelAttribute("politica") Politica formulario,
            BindingResult result, RedirectAttributes redirectAttributes) {
        Politica politica = formulario.getId() == null ? formulario : politicaService.buscar(formulario.getId());
        if (formulario.getId() != null && politica == null) {
            redirectAttributes.addFlashAttribute("error", "No se encontró la política.");
            return "redirect:/politicas";
        }
        if (politica != formulario) {
            politica.setProducto(formulario.getProducto());
            politica.setVersion(formulario.getVersion());
            politica.setVigencia(formulario.getVigencia());
            politica.setDescripcion(formulario.getDescripcion());
        }
        if (result.hasErrors()) return "legal/nueva-politica";
        politicaService.guardar(politica);
        redirectAttributes.addFlashAttribute("mensaje", "La política se guardó en PostgreSQL correctamente.");
        return "redirect:/politicas";
    }
    @PostMapping("/politicas/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes flash) {
        politicaService.eliminar(id);
        flash.addFlashAttribute("mensaje", "Política eliminada correctamente.");
        return "redirect:/politicas";
    }
}

