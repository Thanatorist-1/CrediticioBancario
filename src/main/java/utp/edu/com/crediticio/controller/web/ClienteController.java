package utp.edu.com.crediticio.controller.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import utp.edu.com.crediticio.service.evaluacion.EvaluacionService;
import utp.edu.com.crediticio.service.evaluacion.ExplicacionDecision;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.service.cliente.ClienteService;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.model.solicitud.SolicitudPortalForm;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;
import utp.edu.com.crediticio.service.solicitud.DocumentoSolicitudService;
import utp.edu.com.crediticio.service.externo.ConsultaExternaService;
import utp.edu.com.crediticio.service.politica.TipoCreditoService;

@Controller
public class ClienteController {
    private final ClienteService clienteService;
    private final SolicitudService solicitudService;
    private final DocumentoSolicitudService documentoService;
    private final ConsultaExternaService consultaExternaService;
    private final EvaluacionService evaluacionService;
    private final TipoCreditoService tipos;

    public ClienteController(ClienteService clienteService, SolicitudService solicitudService,
            DocumentoSolicitudService documentoService, ConsultaExternaService consultaExternaService,
            EvaluacionService evaluacionService, TipoCreditoService tipos) {
        this.clienteService = clienteService;
        this.solicitudService = solicitudService;
        this.documentoService = documentoService;
        this.consultaExternaService = consultaExternaService;
        this.evaluacionService = evaluacionService;
        this.tipos=tipos;
    }

    @GetMapping("/clientes")
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "administracion/clientes";
    }

    @GetMapping("/clientes/registrar")
    public String nuevo(Model model) {
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes flash) {
        Cliente cliente = clienteService.buscar(id);
        if (cliente == null) {
            flash.addFlashAttribute("error", "No se encontró el cliente solicitado.");
            return "redirect:/clientes";
        }
        model.addAttribute("cliente", cliente);
        return "clientes/registrar-cliente";
    }

    @PostMapping("/clientes/guardar")
    public String guardar(@Valid @ModelAttribute("cliente") Cliente cliente, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) return "clientes/registrar-cliente";
        try {
            clienteService.guardar(cliente);
            flash.addFlashAttribute("mensaje", "Cliente guardado correctamente.");
            return "redirect:/clientes";
        } catch (IllegalArgumentException exception) {
            result.rejectValue("dni", "duplicado", exception.getMessage());
            return "clientes/registrar-cliente";
        }
    }

    @PostMapping("/clientes/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes flash) {
        try {
            clienteService.eliminar(id);
            flash.addFlashAttribute("mensaje", "Cliente eliminado correctamente.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", "No se puede eliminar: el cliente tiene solicitudes asociadas.");
        }
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/perfil")
    public String perfil(@RequestParam("id") Long id, Model model, RedirectAttributes flash) {
        Cliente cliente = clienteService.buscar(id);
        if (cliente == null) {
            flash.addFlashAttribute("error", "No se encontró el cliente solicitado.");
            return "redirect:/clientes";
        }
        model.addAttribute("cliente", cliente);
        model.addAttribute("solicitudes", solicitudService.listarPorCliente(id));
        return "clientes/perfil-cliente";
    }

    @GetMapping("/portal-cliente")
    public String portalCliente(HttpSession session, Model model,
            @RequestParam(name="registrada", required=false) Long registrada) {
        Long idCliente = (Long) session.getAttribute("cuentaId");
        Cliente cliente = idCliente == null ? null : clienteService.buscar(idCliente);
        if (cliente == null) return "redirect:/login";
        model.addAttribute("clienteActual", cliente);
        model.addAttribute("solicitudPortal", new SolicitudPortalForm());
        model.addAttribute("tiposDisponibles", tipos.disponibles());
        var historico = solicitudService.listarPorCliente(cliente.getIdCliente());
        model.addAttribute("solicitudes", historico);
        if (registrada != null) {
            historico.stream().filter(s -> registrada.equals(s.getIdSolicitud())).findFirst()
                .ifPresent(s -> model.addAttribute("solicitudReciente", s));
        }
        try {
            model.addAttribute("perfilExterno", consultaExternaService.consultar(cliente.getDni()));
        } catch (RuntimeException ex) {
            model.addAttribute("avisoExterno", "La API externa no responde. Tus datos locales siguen visibles, pero la evaluación de una nueva solicitud requiere la API.");
        }
        return "clientes/portal-cliente";
    }

    /** Vista de un trámite; se verifica la titularidad para evitar leer solicitudes ajenas. */
    @GetMapping("/portal-cliente/solicitudes/{id}")
    public String detalleSolicitudCliente(@PathVariable Long id, HttpSession session, Model model) {
        Long cuentaId = (Long) session.getAttribute("cuentaId");
        Solicitud solicitud = solicitudService.buscar(id);
        if (cuentaId == null || solicitud == null || solicitud.getCliente() == null
                || !cuentaId.equals(solicitud.getCliente().getIdCliente())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada.");
        }
        var evaluacion = evaluacionService.buscarPorSolicitud(id);
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("evaluacion", evaluacion);
        model.addAttribute("explicacion", ExplicacionDecision.visible(evaluacion));
        var documentos = documentoService.listar(id);
        model.addAttribute("documentos", documentos);
        model.addAttribute("documentosRevisionCount", documentos.stream()
                .filter(d -> "REVISION".equals(d.getEtapa())).count());
        return "clientes/detalle-solicitud-cliente";
    }

    /** Compatibilidad: formularios antiguos no pueden saltarse la ruta de creación JSON actual. */
    @PostMapping("/portal-cliente/solicitar")
    public String solicitudPortalAnterior(RedirectAttributes flash) {
        flash.addFlashAttribute("error", "Actualiza esta página. El envío de solicitudes se realiza ahora por JSON.");
        return "redirect:/portal-cliente";
    }
}
