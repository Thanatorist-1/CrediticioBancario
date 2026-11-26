package utp.edu.com.crediticio.controller.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import utp.edu.com.crediticio.service.externo.ConsultaExternaService;

/** Endpoint público exclusivamente para datos FICTICIOS del ejercicio académico. */
@RestController
@RequestMapping("/api/v1/registro")
public class RegistroSimuladoController {
    private final ConsultaExternaService externa;
    public RegistroSimuladoController(ConsultaExternaService externa) { this.externa=externa; }

    @GetMapping("/verificar/{dni}")
    public Map<String, Object> verificar(@PathVariable String dni) {
        var p = externa.verificarPersonaActiva(dni);
        return Map.of("dni", p.dni(), "nombreCompleto", p.nombreCompleto(),
                "fechaNacimiento", p.fechaNacimiento(), "ingresosMensuales", p.ingresosMensuales(),
                "deudaMensual", p.deudaMensual(), "saldoDeudaPendiente", p.saldoDeudaPendiente(), "trabajaActualmente", p.trabajaActualmente(), "entorno", "SIMULADO");
    }
}
