package utp.edu.com.crediticio.service.evaluacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.evaluacion.EvaluacionRepository;
import utp.edu.com.crediticio.repository.politica.PoliticaRepository;
import utp.edu.com.crediticio.model.politica.Politica;
import utp.edu.com.crediticio.model.politica.TipoCredito;
import utp.edu.com.crediticio.service.politica.TipoCreditoService;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;
import utp.edu.com.crediticio.service.externo.ConsultaExternaService;
import utp.edu.com.crediticio.service.externo.PerfilCrediticioExterno;
import utp.edu.com.crediticio.service.externo.ServicioExternoNoDisponibleException;

class EvaluacionAutomaticaServiceTest {
    EvaluacionRepository evaluaciones = mock(EvaluacionRepository.class);
    SolicitudRepository solicitudes = mock(SolicitudRepository.class);
    PoliticaRepository politicas = mock(PoliticaRepository.class);
    ConsultaExternaService externa = mock(ConsultaExternaService.class);
    TipoCreditoService tipos = mock(TipoCreditoService.class);
    EvaluacionAutomaticaService service;


    @BeforeEach void crear() {
        service = new EvaluacionAutomaticaService(evaluaciones, solicitudes, politicas, externa, tipos);
        TipoCredito tipo = new TipoCredito();tipo.setCodigo("Personal");tipo.setNombre("Crédito personal");tipo.setActivo(true);
        when(tipos.exigirDisponible(anyString())).thenReturn(tipo);
        when(politicas.findFirstByProductoIgnoreCaseAndEstadoIgnoreCaseOrderByIdDesc(anyString(), anyString()))
                .thenReturn(Optional.of(new Politica("DEMO","Crédito personal","1.0","2026","Activa","Prueba")));
        when(evaluaciones.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    Solicitud solicitud(String dni) {
        Cliente c = new Cliente(); c.setDni(dni); c.setGastosMensuales(new BigDecimal("1500"));
        Solicitud s = new Solicitud(); s.setIdSolicitud(1L); s.setCliente(c);
        s.setTipoCredito("Personal"); s.setMonto(new BigDecimal("10000")); s.setPlazoMeses(24);
        s.setGastosMensuales(new BigDecimal("1500")); return s;
    }
    PerfilCrediticioExterno perfil(String dni, int score, int cuotas, boolean negativo, int ingreso, int deuda) {
        return new PerfilCrediticioExterno("SIMULADO", "API académica", dni, "Perfil SIMULADO", "Nombre", "Apellido",
            LocalDate.of(1995, 4, 18), true, true, true, score, "Bajo",
            BigDecimal.valueOf(ingreso), BigDecimal.valueOf(deuda), BigDecimal.valueOf(deuda*12L), cuotas, negativo, "Datos ficticios");
    }
    @Test void aprobarConScoreExternoYDTIBajo() {
        var s=solicitud("90000001");
        when(externa.consultar("90000001")).thenReturn(perfil("90000001",742,0,false,4800,650));
        Evaluacion ev=service.evaluarNuevaSolicitud(s);
        assertEquals(742, ev.getPuntaje()); assertEquals("Aprobado",ev.getResultado());
        assertEquals("Aprobada",s.getEstado()); assertNotNull(ev.getFechaConsultaExterna());
    }
    @Test void revisarAtrasoDeUnaCuota() {
        var s=solicitud("90000002");
        when(externa.consultar("90000002")).thenReturn(perfil("90000002",661,1,false,3500,1150));
        assertEquals("Revisión manual",service.evaluarNuevaSolicitud(s).getResultado());
    }
    @Test void rechazarConMoraGraveEHistorialNegativo() {
        var s=solicitud("90000003");
        when(externa.consultar("90000003")).thenReturn(perfil("90000003",512,3,true,2800,1450));
        assertEquals("Rechazado",service.evaluarNuevaSolicitud(s).getResultado());
        assertEquals("Rechazada",s.getEstado());
    }
    @Test void noGuardarSiLaApiEstaCaida() {
        when(externa.consultar("90000001")).thenThrow(new ServicioExternoNoDisponibleException());
        assertThrows(ServicioExternoNoDisponibleException.class,
                     () -> service.evaluarNuevaSolicitud(solicitud("90000001")));
        verifyNoInteractions(evaluaciones, solicitudes);
    }
    @Test void politicasConGarantiaPidenRevisionManual() {
        var s=solicitud("90000001");
        when(externa.consultar("90000001")).thenReturn(perfil("90000001",742,0,false,4800,650));
        Politica politica=new Politica();politica.setCodigo("POL-GARANTIA");politica.setVersion("2");
        politica.setRequiereGarantia(true);
        when(politicas.findFirstByProductoIgnoreCaseAndEstadoIgnoreCaseOrderByIdDesc(anyString(), anyString()))
            .thenReturn(Optional.of(politica));
        var r=service.evaluarNuevaSolicitud(s);
        assertEquals("Revisión manual",r.getResultado());
        assertTrue(r.getComentario().contains("garantía"));
    }
    @Test void montoFueraDePoliticaSeBloquea() {
        var s=solicitud("90000001");s.setMonto(new BigDecimal("60000"));
        Politica politica=new Politica();politica.setCodigo("POL-LIMITE");politica.setVersion("1");
        politica.setMontoMaximo(new BigDecimal("15000"));
        when(politicas.findFirstByProductoIgnoreCaseAndEstadoIgnoreCaseOrderByIdDesc(anyString(), anyString()))
            .thenReturn(Optional.of(politica));
        assertThrows(IllegalArgumentException.class,()->service.evaluarNuevaSolicitud(s));
        verifyNoInteractions(evaluaciones,solicitudes);
    }
    @Test void desempleoNoApruebaSinRevision() {
        var s=solicitud("90000005");
        var p = new PerfilCrediticioExterno("SIMULADO", "API académica", "90000005",
            "Ana Prueba", "Ana", "Prueba", LocalDate.of(1995, 4, 18), true, true, false,
            780, "Bajo", BigDecimal.valueOf(4800), BigDecimal.valueOf(100), BigDecimal.valueOf(1200), 0, false, "Ficticio");
        when(externa.consultar("90000005")).thenReturn(p);
        assertEquals("Revisión manual", service.evaluarNuevaSolicitud(s).getResultado());
    }

}
