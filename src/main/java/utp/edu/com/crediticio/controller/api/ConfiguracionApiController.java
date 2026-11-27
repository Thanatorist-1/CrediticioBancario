package utp.edu.com.crediticio.controller.api;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import utp.edu.com.crediticio.model.politica.*;
import utp.edu.com.crediticio.service.politica.*;
@RestController @RequestMapping("/api/v1") public class ConfiguracionApiController {
    private final PoliticaService politicas;
    private final AuditoriaConfiguracionService auditoria;
    public ConfiguracionApiController(PoliticaService p, AuditoriaConfiguracionService a){
        politicas=p;
        auditoria=a;
    }
    @GetMapping("/auditoria-configuracion") public List<AuditoriaConfiguracion> auditoria(){return auditoria.listar();}
}
