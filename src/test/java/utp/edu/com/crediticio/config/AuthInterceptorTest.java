package utp.edu.com.crediticio.config;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;
class AuthInterceptorTest {
    UsuarioRepository repo=mock(UsuarioRepository.class);
    AuthInterceptor interceptor=new AuthInterceptor(repo);
    HttpServletRequest req(String path,String metodo,String rol) {
        var request=mock(HttpServletRequest.class);var sesion=mock(HttpSession.class);
        when(request.getRequestURI()).thenReturn(path);when(request.getContextPath()).thenReturn("");
        when(request.getMethod()).thenReturn(metodo);when(request.getSession(false)).thenReturn(sesion);
        when(sesion.getAttribute("tipoCuenta")).thenReturn("CLIENTE".equals(rol)?"CLIENTE":"PERSONAL");
        when(sesion.getAttribute("cuentaId")).thenReturn(8L);
        if(!"CLIENTE".equals(rol)){Usuario u=new Usuario();u.setRol(rol);u.setEstado("Activo");
            when(repo.findById(8L)).thenReturn(Optional.of(u));}
        return request;
    }
    boolean permite(String ruta,String metodo,String rol) throws Exception {
        HttpServletResponse response=new MockHttpServletResponse();return interceptor.preHandle(req(ruta,metodo,rol),response,new Object());
    }
    @Test void clientePuedeCrearSolicitudesPeroNoVerClientesAjeno() throws Exception {
        assertTrue(permite("/api/v1/portal/solicitudes","POST","CLIENTE"));
        assertFalse(permite("/api/v1/clientes","GET","CLIENTE"));
    }
    @Test void soloAdministradorEditaPoliticas() throws Exception {
        assertTrue(permite("/api/v1/politicas","GET","Analista"));
        assertFalse(permite("/api/v1/politicas","POST","Analista"));
        assertFalse(permite("/api/v1/politicas/4","PUT","Supervisor"));
        assertTrue(permite("/api/v1/politicas","POST","Administrador"));
        assertTrue(permite("/politicas/nueva","GET","Administrador"));
        assertFalse(permite("/politicas/nueva","GET","Analista"));
    }
    @Test void catalogoDeCreditoYUsuariosSoloAdministrador() throws Exception {
        assertFalse(permite("/tipos-credito","GET","Supervisor"));
        assertTrue(permite("/tipos-credito","GET","Administrador"));
        assertFalse(permite("/api/v1/tipos-credito","POST","Analista"));
        assertFalse(permite("/api/v1/personal","POST","Supervisor"));
    }
    @Test void decisionFinalSoloSupervisorYRequerimientoAnalista() throws Exception {
        assertFalse(permite("/api/v1/evaluaciones/3/decision","POST","Analista"));
        assertTrue(permite("/api/v1/evaluaciones/3/decision","POST","Supervisor"));
        assertFalse(permite("/api/v1/evaluaciones/3/decision","POST","Administrador"));
        assertTrue(permite("/api/v1/evaluaciones/3/solicitar-documentos","POST","Analista"));
    }
    @Test void cuentaInactivaEsBloqueada() throws Exception {
        var r=req("/politicas","GET","Analista");Usuario u=new Usuario();u.setRol("Analista");u.setEstado("Inactivo");
        when(repo.findById(8L)).thenReturn(Optional.of(u));
        assertFalse(interceptor.preHandle(r,new MockHttpServletResponse(),new Object()));
    }
}
