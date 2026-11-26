package utp.edu.com.crediticio.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.usuario.UsuarioRepository;

/** Autoriza cada ruta del servidor (incluidos endpoints REST) a partir del usuario real en BD. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final UsuarioRepository usuarios;
    public AuthInterceptor(UsuarioRepository usuarios) { this.usuarios=usuarios; }

    @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws IOException {
        String path=req.getRequestURI().substring(req.getContextPath().length());
        String method=req.getMethod();
        boolean lectura="GET".equals(method)||"HEAD".equals(method)||"OPTIONS".equals(method);
        if(path.equals("/")||path.equals("/login")||path.equals("/logout")||path.equals("/registro-cliente")
            ||path.startsWith("/api/v1/auth/")||path.startsWith("/api/v1/registro/")
            ||path.startsWith("/css/")||path.startsWith("/js/")||path.startsWith("/img/")
            ||path.equals("/favicon.ico")||path.equals("/error")) return true;
        HttpSession session=req.getSession(false);
        String tipo=session==null?null:(String)session.getAttribute("tipoCuenta");
        if(tipo==null) return negar(req,resp,401,"Inicia sesión para continuar.");
        boolean cliente="CLIENTE".equals(tipo);
        boolean portal=path.equals("/portal-cliente")||path.equals("/portal-cliente/solicitar")
            ||path.startsWith("/portal-cliente/solicitudes/")||path.startsWith("/api/v1/portal/")
            ||path.startsWith("/api/v1/clientes/mi-cuenta");
        boolean docs=path.matches("/api/v1/solicitudes/[0-9]+/documentos(?:/.*)?");
        if(cliente) {
            if(portal||docs) return true; // DocumentoApiController valida propiedad y estado del trámite.
            return negar(req,resp,403,"La cuenta de cliente no tiene acceso a esta operación.");
        }
        if(!"PERSONAL".equals(tipo)||!(session.getAttribute("cuentaId") instanceof Long id)) {
            session.invalidate(); return negar(req,resp,401,"Sesión inválida.");
        }
        Usuario usuario=usuarios.findById(id).orElse(null);
        if(usuario==null||!"Activo".equals(usuario.getEstado())) {
            session.invalidate();return negar(req,resp,401,"Cuenta inactiva o eliminada.");
        }
        String rol=usuario.getRol();
        if(!"Analista".equals(rol)&&!"Supervisor".equals(rol)&&!"Administrador".equals(rol))
            return negar(req,resp,403,"Rol no reconocido.");
        req.setAttribute("rolActual",rol);
        session.setAttribute("rolActual",rol); // Únicamente presentación; permiso se calcula de la BD arriba.
        boolean admin="Administrador".equals(rol);
        boolean supervisor="Supervisor".equals(rol);
        if(portal) return negar(req,resp,403,"Solo disponible para clientes.");
        if(path.startsWith("/usuarios")||path.startsWith("/api/v1/personal")) {
            return admin?true:negar(req,resp,403,"Solo el administrador gestiona al personal.");
        }
        if(path.startsWith("/politicas/auditoria")||path.startsWith("/api/v1/auditoria-configuracion"))
            return admin?true:negar(req,resp,403,"Auditoría reservada a administración.");
        if(path.startsWith("/tipos-credito")||path.startsWith("/api/v1/tipos-credito")) {
            return admin?true:negar(req,resp,403,"Solo administración gestiona tipos de crédito.");
        }
        if(path.startsWith("/politicas")||path.startsWith("/api/v1/politicas")) {
            // Consulta de políticas: cualquier miembro del banco. Mutación: únicamente administración.
            if(lectura && (path.equals("/politicas")||path.equals("/api/v1/politicas")||path.matches("/api/v1/politicas/[0-9]+"))) return true;
            return admin?true:negar(req,resp,403,"Solo el administrador puede crear o modificar políticas.");
        }
        // El personal consulta clientes; solo el administrador puede alterar datos administrativos.
        if((path.startsWith("/api/v1/clientes")||path.startsWith("/clientes/editar")
            ||path.startsWith("/clientes/guardar")||path.startsWith("/clientes/eliminar"))&&!lectura&&!admin)
            return negar(req,resp,403,"No puedes modificar clientes.");
        if(path.startsWith("/clientes/editar")&&!admin) return negar(req,resp,403,"Solo administración puede editar clientes.");
        // Los préstamos nuevos solo los presenta el titular desde /api/v1/portal/solicitudes.
        if((path.startsWith("/api/v1/solicitudes")&&!docs&&!lectura)
          ||(path.startsWith("/solicitudes/")&&!lectura)
          ||path.equals("/solicitudes/registrar")||path.startsWith("/solicitudes/editar"))
            return negar(req,resp,403,"Las solicitudes nuevas deben presentarlas los clientes desde su portal.");
        if(path.startsWith("/api/v1/evaluaciones")||path.startsWith("/evaluaciones")) {
            boolean decision=path.matches("(?:/api/v1)?/evaluaciones/[0-9]+/decision");
            boolean pedirDocs=path.matches("/api/v1/evaluaciones/[0-9]+/solicitar-documentos");
            if(decision) return supervisor?true:negar(req,resp,403,"La decisión manual final corresponde al supervisor.");
            if(pedirDocs) return (supervisor||"Analista".equals(rol))?true:negar(req,resp,403,"Solo analista o supervisor solicitan documentos.");
            if(!lectura) return negar(req,resp,403,"No está permitido modificar evaluaciones fuera del flujo de revisión.");
            if(path.equals("/evaluaciones/nueva")||path.startsWith("/evaluaciones/editar"))
                return negar(req,resp,403,"No se pueden editar las evaluaciones automáticas.");
        }
        return true;
    }
    private boolean negar(HttpServletRequest req,HttpServletResponse resp,int estado,String mensaje) throws IOException {
        if(req.getRequestURI().substring(req.getContextPath().length()).startsWith("/api/")) {
            resp.setStatus(estado);resp.setContentType("application/json;charset=UTF-8");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.getWriter().write("{\"error\":\""+mensaje+"\"}");
        } else if(estado==401) resp.sendRedirect("/login");
        else resp.sendError(403,mensaje);
        return false;
    }
}
