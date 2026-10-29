package utp.edu.com.crediticio.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.service.evaluacion.EvaluacionService;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;

@Controller
public class EvaluacionController {
    private final EvaluacionService evaluacionService;
    private final SolicitudService solicitudService;

    public EvaluacionController(EvaluacionService evaluacionService, SolicitudService solicitudService) {
        this.evaluacionService = evaluacionService;
        this.solicitudService = solicitudService;
    }

    @GetMapping("/evaluaciones")
    public String listar(Model model) {
        var evaluaciones = evaluacionService.listar();
        model.addAttribute("evaluaciones", evaluaciones);
        model.addAttribute("evaluacionesTotal", evaluaciones.size());
        model.addAttribute("aprobadasTotal", evaluaciones.stream().filter(e -> "Aprobado".equalsIgnoreCase(e.getResultado())).count());
        model.addAttribute("rechazadasTotal", evaluaciones.stream().filter(e -> "Rechazado".equalsIgnoreCase(e.getResultado())).count());
        return "evaluaciones/evaluaciones";
    }

    @GetMapping("/evaluaciones/nueva")
    public String nueva(Model model) {
        model.addAttribute("evaluacion", new Evaluacion());
        model.addAttribute("solicitudes", solicitudService.listarConCliente());
        return "evaluaciones/registrar-evaluacion";
    }

    @GetMapping("/evaluaciones/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        Evaluacion evaluacion = evaluacionService.buscar(id);
        if (evaluacion == null) {
            flash.addFlashAttribute("error", "No se encontró la evaluación.");
            return "redirect:/evaluaciones";
        }
        if (evaluacion.getSolicitud() != null) {
            evaluacion.setIdSolicitud(evaluacion.getSolicitud().getIdSolicitud());
        }
        model.addAttribute("evaluacion", evaluacion);
        model.addAttribute("solicitudes", solicitudService.listarConCliente());
        return "evaluaciones/registrar-evaluacion";
    }

    @PostMapping("/evaluaciones/guardar")
    public String guardar(@Valid @ModelAttribute("evaluacion") Evaluacion evaluacion, BindingResult result,
            Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("solicitudes", solicitudService.listarConCliente());
            return "evaluaciones/registrar-evaluacion";
        }
        try {
            evaluacionService.guardar(evaluacion);
            flash.addFlashAttribute("mensaje", "Evaluación guardada correctamente.");
            return "redirect:/evaluaciones";
        } catch (IllegalArgumentException exception) {
            result.reject("evaluacion.invalida", exception.getMessage());
            model.addAttribute("solicitudes", solicitudService.listarConCliente());
            return "evaluaciones/registrar-evaluacion";
        }
    }

    @PostMapping("/evaluaciones/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes flash) {
        evaluacionService.eliminar(id);
        flash.addFlashAttribute("mensaje", "Evaluación eliminada correctamente.");
        return "redirect:/evaluaciones";
    }

    @GetMapping("/evaluaciones/detalle")
    public String detalle(@RequestParam("id") Long id, Model model, RedirectAttributes flash) {
        Evaluacion evaluacion = evaluacionService.buscar(id);
        if (evaluacion == null) {
            flash.addFlashAttribute("error", "No se encontró la evaluación.");
            return "redirect:/evaluaciones";
        }
        model.addAttribute("evaluacion", evaluacion);
        return "evaluaciones/detalle-evaluacion";
    }

    @PostMapping("/evaluaciones/{id}/decision")
    public String registrarDecision(@PathVariable("id") Long id,
            @RequestParam("resultado") String resultado,
            @RequestParam(name = "comentario", required = false) String comentario,
            RedirectAttributes flash) {
        try {
            evaluacionService.registrarDecision(id, resultado, comentario);
            flash.addFlashAttribute("mensaje", "Decisión registrada correctamente.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/evaluaciones/detalle?id=" + id;
    }

    @GetMapping("/oferta")
    public String mostrarOferta() { return "administracion/oferta"; }
}

