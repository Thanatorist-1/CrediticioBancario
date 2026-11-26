package utp.edu.com.crediticio.service.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.reporte.ReporteResumen;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.evaluacion.EvaluacionRepository;

class ReporteAnaliticaServiceTest {
    @Test void agregaIndicadoresConEvaluacionesHistoricas() {
        EvaluacionRepository repo=mock(EvaluacionRepository.class);
        Solicitud primera=solicitud(1L,1L,"Personal","Aprobada","5000");
        Solicitud segunda=solicitud(2L,2L,"Vehicular","Rechazada","20000");
        Solicitud tercera=solicitud(3L,1L,"Educativo","Revisión manual","3000");
        Evaluacion e1=evaluacion(primera,760,"Bajo","24.00",0,true);
        Evaluacion e2=evaluacion(segunda,460,"Alto","68.00",3,true);
        Evaluacion e3=evaluacion(tercera,620,"Medio","41.00",1,false);
        when(repo.buscarPorSolicitudes(anyList())).thenReturn(List.of(e1,e2,e3));
        ReporteResumen datos=new ReporteResumen(List.of(primera,segunda,tercera),List.of(),3,1,1,1,
                new BigDecimal("28000"),new BigDecimal("33.3"),new BigDecimal("33.3"));
        var resultado=new ReporteAnaliticaService(repo).calcular(datos);
        assertEquals(2L,resultado.get("clientesUnicos"));
        assertEquals(3,resultado.get("evaluadas"));
        assertEquals(1L,resultado.get("aprobadas"));
        assertEquals(1L,resultado.get("rechazadas"));
        assertEquals(1L,resultado.get("revision"));
        assertEquals(0L,resultado.get("pendientes"));
        assertEquals(2L,resultado.get("casosConAtrasos"));
        assertEquals(1L,resultado.get("casosDtiAlto"));
        assertEquals(1L,resultado.get("casosSinTrabajo"));
        assertEquals(new BigDecimal("5000"),resultado.get("montoAprobado"));
        assertEquals(3,((List<?>)resultado.get("productos")).size());
    }

    @Test void soportaReporteVacioSinConsultarBase() {
        EvaluacionRepository repo=mock(EvaluacionRepository.class);
        var base=new ReporteResumen(List.of(),List.of(),0,0,0,0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);
        var resultado=new ReporteAnaliticaService(repo).calcular(base);
        assertEquals(0,resultado.get("evaluadas"));
        assertEquals(false,resultado.get("tieneScore"));
        verify(repo,never()).buscarPorSolicitudes(anyList());
    }

    private static Solicitud solicitud(Long id,Long clienteId,String tipo,String estado,String monto) {
        Cliente cliente=new Cliente(); cliente.setIdCliente(clienteId);
        Solicitud s=new Solicitud(); s.setIdSolicitud(id);s.setCliente(cliente);
        s.setTipoCredito(tipo);s.setEstado(estado);s.setMonto(new BigDecimal(monto));
        return s;
    }
    private static Evaluacion evaluacion(Solicitud s,int puntaje,String riesgo,String dti,int atrasos,boolean trabaja) {
        Evaluacion e=new Evaluacion();e.setSolicitud(s);e.setPuntaje(puntaje);
        e.setNivelRiesgo(riesgo);e.setDti(new BigDecimal(dti));
        e.setCuotasAtrasadas(atrasos);e.setTrabajaActualmente(trabaja);
        return e;
    }
}
