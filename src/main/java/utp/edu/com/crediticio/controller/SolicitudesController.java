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
import java.util.List;
import utp.edu.com.crediticio.service.cliente.ClienteService;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;

@Controller
public class SolicitudesController {
    private final SolicitudService solicitudService;
    private final ClienteService clienteService;

    public SolicitudesController(SolicitudService solicitudService, ClienteService clienteService) {
        this.solicitudService = solicitudService;
        this.clienteService = clienteService;
    }

    @GetMapping("/solicitudes")
    public String listar(@RequestParam(name = "texto", required = false) String texto,
            @RequestParam(name = "estado", required = false) String estado, Model model) {
        var solicitudes = solicitudService.listar().stream()
                .filter(solicitud -> texto == null || texto.isBlank()
                        || (solicitud.getCodigo() != null && solicitud.getCodigo().toLowerCase().contains(texto.toLowerCase()))
                        || (solicitud.getCliente() != null && ((solicitud.getCliente().getNombre() != null
                                && solicitud.getCliente().getNombre().toLowerCase().contains(texto.toLowerCase()))
                                || (solicitud.getCliente().getDni() != null
                                && solicitud.getCliente().getDni().contains(texto)))))
                .filter(solicitud -> estado == null || estado.isBlank() || estado.equals(solicitud.getEstado()))
                .toList();
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("texto", texto);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("estados", List.of("Pendiente", "En evaluación", "Revisión manual", "Aprobada", "Rechazada"));
        return "solicitudes/solicitudes";
    }

    @GetMapping("/solicitudes/registrar")
    public String nuevo(Model model) {
        model.addAttribute("solicitud", new Solicitud());
        model.addAttribute("clientes", clienteService.listar());
        return "solicitudes/registrar-solicitud";
    }

    @GetMapping("/solicitudes/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        Solicitud solicitud = solicitudService.buscar(id);
        if (solicitud == null) {
            flash.addFlashAttribute("error", "No se encontró la solicitud.");
            return "redirect:/solicitudes";
        }
        if (solicitud.getCliente() != null) {
            solicitud.setIdCliente(solicitud.getCliente().getIdCliente());
        }
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("clientes", clienteService.listar());
        return "solicitudes/registrar-solicitud";
    }

    @PostMapping("/solicitudes/guardar")
    public String guardar(@Valid @ModelAttribute("solicitud") Solicitud solicitud, BindingResult result,
            Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("clientes", clienteService.listar());
            return "solicitudes/registrar-solicitud";
        }
        try {
            solicitudService.guardar(solicitud);
            flash.addFlashAttribute("mensaje", "Solicitud guardada correctamente.");
            return "redirect:/solicitudes";
        } catch (IllegalArgumentException exception) {
            result.reject("solicitud.invalida", exception.getMessage());
            model.addAttribute("clientes", clienteService.listar());
            return "solicitudes/registrar-solicitud";
        }
    }

    @PostMapping("/solicitudes/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes flash) {
        try {
            solicitudService.eliminar(id);
            flash.addFlashAttribute("mensaje", "Solicitud eliminada correctamente.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", "No se puede eliminar: existe una evaluación asociada.");
        }
        return "redirect:/solicitudes";
    }

    @PostMapping("/solicitudes/{id}/estado")
    public String actualizarEstado(@PathVariable("id") Long id,
            @RequestParam("estado") String estado, RedirectAttributes flash) {
        try {
            solicitudService.actualizarEstado(id, estado);
            flash.addFlashAttribute("mensaje", "Estado actualizado a: " + estado + ".");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/solicitudes";
    }

    @GetMapping("/solicitudes/detalle")
    public String detalle(@RequestParam("id") Long id, Model model, RedirectAttributes flash) {
        Solicitud solicitud = solicitudService.buscar(id);
        if (solicitud == null) {
            flash.addFlashAttribute("error", "No se encontró la solicitud.");
            return "redirect:/solicitudes";
        }
        model.addAttribute("solicitud", solicitud);
        return "solicitudes/detalle-solicitud";
    }
}

