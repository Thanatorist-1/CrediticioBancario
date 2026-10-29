package utp.edu.com.crediticio.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import utp.edu.com.crediticio.model.reporte.ReporteResumen;
import utp.edu.com.crediticio.service.reporte.ReporteService;
import utp.edu.com.crediticio.service.reporte.ReportePdfService;

@Controller
public class ReporteController {
    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;

    public ReporteController(ReporteService reporteService, ReportePdfService reportePdfService) {
        this.reporteService = reporteService;
        this.reportePdfService = reportePdfService;
    }

    @GetMapping("/reportes")
    public String mostrarReportes(
            @RequestParam(name = "fechaInicio", required = false) String fechaInicio,
            @RequestParam(name = "fechaFin", required = false) String fechaFin,
            Model model) {
        try {
            ReporteResumen resumen = reporteService.generar(fechaInicio, fechaFin);
            model.addAttribute("resumen", resumen);
            model.addAttribute("fechaInicio", fechaInicio == null ? "" : fechaInicio);
            model.addAttribute("fechaFin", fechaFin == null ? "" : fechaFin);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("resumen", reporteService.generar(null, null));
            model.addAttribute("fechaInicio", "");
            model.addAttribute("fechaFin", "");
        }
        return "reportes/reportes";
    }

    @GetMapping("/reportes/detalles")
    public String verDetalles(
            @RequestParam(name = "fechaInicio", required = false) String fechaInicio,
            @RequestParam(name = "fechaFin", required = false) String fechaFin,
            Model model) {
        try {
            ReporteResumen resumen = reporteService.generar(fechaInicio, fechaFin);
            model.addAttribute("solicitudes", resumen.getSolicitudes());
            model.addAttribute("fechaInicio", fechaInicio == null ? "" : fechaInicio);
            model.addAttribute("fechaFin", fechaFin == null ? "" : fechaFin);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("solicitudes", reporteService.generar(null, null).getSolicitudes());
            model.addAttribute("fechaInicio", "");
            model.addAttribute("fechaFin", "");
        }
        return "reportes/detalle-reportes";
    }

    @GetMapping("/reportes/descargar")
    public Object descargarPdf(
            @RequestParam(name = "fechaInicio", required = false) String fechaInicio,
            @RequestParam(name = "fechaFin", required = false) String fechaFin,
            RedirectAttributes redirectAttributes) {
        try {
            ReporteResumen resumen = reporteService.generar(fechaInicio, fechaFin);
            byte[] pdf = reportePdfService.generar(resumen, fechaInicio, fechaFin);
            String nombre = "reporte-crediticio-" + java.time.LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(nombre, StandardCharsets.UTF_8))
                    .body(pdf);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/reportes";
        }
    }
}

