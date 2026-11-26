package utp.edu.com.crediticio.service.politica;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import utp.edu.com.crediticio.model.politica.*;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.politica.PoliticaRepository;
class PoliticaServiceRolesTest {
    PoliticaRepository repo=mock(PoliticaRepository.class);
    TipoCreditoService tipos=mock(TipoCreditoService.class);
    AuditoriaConfiguracionService audit=mock(AuditoriaConfiguracionService.class);
    PoliticaService service=new PoliticaService(repo,tipos,audit);
    Usuario usuario(String role) {Usuario u=new Usuario();u.setIdUsuario(4L);u.setCorreo("pruebas@banco.com");u.setRol(role);u.setEstado("Activo");return u;}
    Politica datos(){
        Politica p=new Politica(null,"Crédito agrícola","1.0","2026","Activa","Política de pruebas");
        p.setPuntajeMinimoAprobar(660);p.setPuntajeMaximoRechazo(490);
        p.setDtiMaximoAprobacion(new BigDecimal("30"));p.setDtiMinimoRechazo(new BigDecimal("50"));return p;
    }
    @Test void analistaNoPuedeCrear() {assertThrows(ResponseStatusException.class,()->service.guardar(datos(),usuario("Analista")));verifyNoInteractions(repo,audit);}
    @Test void supervisorNoPuedeModificar() {assertThrows(ResponseStatusException.class,()->service.guardar(datos(),usuario("Supervisor")));verifyNoInteractions(repo,audit);}
    @Test void adminCreaYAudita() {
        TipoCredito t=new TipoCredito();t.setNombre("Crédito agrícola");t.setCodigo("Agro");t.setActivo(true);
        when(tipos.buscarPorNombre("Crédito agrícola")).thenReturn(t);
        when(repo.findByProductoIgnoreCaseAndEstadoIgnoreCase(anyString(),eq("Activa"))).thenReturn(java.util.List.of());
        when(repo.saveAndFlush(any())).thenAnswer(i->{Politica p=i.getArgument(0);p.setId(50L);return p;});
        Politica p=service.guardar(datos(),usuario("Administrador"));
        assertEquals("Crédito agrícola",p.getProducto());
        verify(audit).guardar(eq("POLITICA"),eq(50L),eq("CREADA"),any(),isNull(),anyMap());
    }
}
