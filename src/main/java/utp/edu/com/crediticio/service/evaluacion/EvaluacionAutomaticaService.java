package utp.edu.com.crediticio.service.evaluacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.LocalDate;
import utp.edu.com.crediticio.service.externo.ConsultaExternaService;
import utp.edu.com.crediticio.service.externo.PerfilCrediticioExterno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.politica.Politica;
import utp.edu.com.crediticio.service.politica.TipoCreditoService;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.evaluacion.EvaluacionRepository;
import utp.edu.com.crediticio.repository.politica.PoliticaRepository;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;

/** Aplica reglas demostrativas y transparentes al registrar una solicitud. */
@Service
public class EvaluacionAutomaticaService {
    private final EvaluacionRepository evaluacionRepository;
    private final SolicitudRepository solicitudRepository;
    private final PoliticaRepository politicaRepository;
    private final ConsultaExternaService externa;
    private final TipoCreditoService tipos;

    public EvaluacionAutomaticaService(EvaluacionRepository evaluacionRepository,
            SolicitudRepository solicitudRepository, PoliticaRepository politicaRepository,
            ConsultaExternaService externa, TipoCreditoService tipos) {
        this.evaluacionRepository = evaluacionRepository;
        this.solicitudRepository = solicitudRepository;
        this.politicaRepository = politicaRepository;
        this.externa = externa;
        this.tipos=tipos;
    }

    @Transactional
    public Evaluacion evaluarNuevaSolicitud(Solicitud solicitud) {
        String producto = tipos.exigirDisponible(solicitud.getTipoCredito()).getNombre();
        Politica politica = politicaRepository.findFirstByProductoIgnoreCaseAndEstadoIgnoreCaseOrderByIdDesc(
                producto, "Activa").orElseThrow(() -> new IllegalArgumentException(
                "El producto no tiene una política activa. No se puede evaluar."));
        BigDecimal minimoMonto = politica.getMontoMinimo()==null ? new BigDecimal("500") : politica.getMontoMinimo();
        BigDecimal maximoMonto = politica.getMontoMaximo()==null ? new BigDecimal("200000") : politica.getMontoMaximo();
        Integer minPlazo = politica.getPlazoMinimoMeses()==null ? 6 : politica.getPlazoMinimoMeses();
        Integer maxPlazo = politica.getPlazoMaximoMeses()==null ? 120 : politica.getPlazoMaximoMeses();
        if(solicitud.getMonto()==null || solicitud.getMonto().compareTo(minimoMonto)<0 || solicitud.getMonto().compareTo(maximoMonto)>0
           ||solicitud.getPlazoMeses()==null || solicitud.getPlazoMeses()<minPlazo || solicitud.getPlazoMeses()>maxPlazo)
           throw new IllegalArgumentException("El monto o plazo no cumple los límites de la política vigente para " + producto + ".");
        int minimoAprobar = politica.getPuntajeMinimoAprobar() == null ? 650 : politica.getPuntajeMinimoAprobar();
        int maximoRechazo = politica == null || politica.getPuntajeMaximoRechazo() == null ? 499 : politica.getPuntajeMaximoRechazo();
        BigDecimal dtiAprobar = politica == null || politica.getDtiMaximoAprobacion() == null ? new BigDecimal("35") : politica.getDtiMaximoAprobacion();
        BigDecimal dtiRechazar = politica == null || politica.getDtiMinimoRechazo() == null ? new BigDecimal("50") : politica.getDtiMinimoRechazo();

        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setSolicitud(solicitud);
        evaluacion.setCodigo("EV-AUTO-%03d".formatted(solicitud.getIdSolicitud()));
        evaluacion.setPolitica(politica == null ? "Regla general" : politica.getCodigo() + " · " + politica.getVersion());
        // La API externa es la fuente del score, ingresos y obligaciones existentes.
        // Fallar sin respuesta detiene toda la transacción y NO genera una aprobación ficticia.
        PerfilCrediticioExterno perfil = externa.consultar(solicitud.getCliente().getDni());
        BigDecimal ingresos = perfil.ingresosMensuales();
        BigDecimal gastos = cero(solicitud.getGastosMensuales());
        BigDecimal deudas = cero(perfil.deudaMensual());
        Integer puntaje = perfil.scoreCrediticio();
        int atrasadas = perfil.cuotasAtrasadas() == null ? 0 : perfil.cuotasAtrasadas();
        evaluacion.setFuenteScore("Servicio Académico de Información Crediticia");
        evaluacion.setFechaConsultaExterna(LocalDateTime.now());
        evaluacion.setIngresosExternos(ingresos);
        evaluacion.setDeudaExterna(deudas);
        evaluacion.setSaldoDeudaPendiente(perfil.saldoDeudaPendiente());
        evaluacion.setCuotasAtrasadas(atrasadas);
        evaluacion.setHistorialNegativo(perfil.historialNegativo());
        evaluacion.setTrabajaActualmente(perfil.trabajaActualmente());
        evaluacion.setIdentidadVerificadaExterna(perfil.identidadVerificada());
        evaluacion.setPersonaActivaExterna(perfil.personaActiva());
        evaluacion.setPuntaje(puntaje);
        evaluacion.setNivelRiesgo(normalizarRiesgo(perfil.clasificacionRiesgo()));
        // Guardar las cifras externas en la solicitud para que coincidan con las usadas al evaluar.
        solicitud.setIngresosMensuales(ingresos);
        solicitud.setDeudasMensuales(deudas);
        if (!perfil.identidadVerificada() || !perfil.personaActiva()
                || puntaje == null || puntaje <= 0 || puntaje > 850
                || ingresos == null || ingresos.signum() <= 0 || perfil.deudaMensual() == null
                || perfil.cuotasAtrasadas() == null || deudas.signum() < 0
                || perfil.saldoDeudaPendiente() == null || perfil.saldoDeudaPendiente().signum() < 0
                || solicitud.getMonto() == null || solicitud.getPlazoMeses() == null) {
            marcar(evaluacion, solicitud, "Revisión manual",
                    ExplicacionDecision.datosIncompletos());
        } else {
            // Cuota DEMOSTRATIVA de capital; no incluye intereses ni costos bancarios reales.
            BigDecimal cuota = cuotaEstimada(solicitud.getMonto(), solicitud.getPlazoMeses(), politica.getTasaAnualReferencial());
            BigDecimal dti = deudas.add(cuota).multiply(BigDecimal.valueOf(100))
                    .divide(ingresos, 2, RoundingMode.HALF_UP);
            BigDecimal disponible = ingresos.subtract(gastos).subtract(deudas);
            evaluacion.setDti(dti);
            evaluacion.setIngresoDisponible(disponible);
            evaluacion.setCapacidadNuevaCuota(disponible.max(BigDecimal.ZERO));
            evaluacion.setMontoMaximoEstimado(disponible.max(BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(solicitud.getPlazoMeses())));
            // Cuotas vencidas o historial negativo: NO aprobación automática.
            // Un atraso aislado va a revisión; 3+ o historial negativo -> rechazo demostrativo.
            boolean garantia = Boolean.TRUE.equals(politica.getRequiereGarantia());
            boolean faltaEmpleo = !Boolean.FALSE.equals(politica.getRequiereEmpleoActivo()) && !perfil.trabajaActualmente();
            boolean excedeEdad = politica.getEdadMaximaAnios()!=null && perfil.fechaNacimiento()!=null
                && Period.between(perfil.fechaNacimiento(), LocalDate.now()).getYears()>politica.getEdadMaximaAnios();
            String resultado;
            if (puntaje <= maximoRechazo || dti.compareTo(dtiRechazar) >= 0 ||
                    disponible.signum() <= 0 || atrasadas >= 3 || perfil.historialNegativo()) {
                resultado = "Rechazado";
            } else if (faltaEmpleo || atrasadas > 0 || garantia || excedeEdad) {
                resultado = "Revisión manual";
            } else if (puntaje >= minimoAprobar && dti.compareTo(dtiAprobar) <= 0 &&
                    disponible.compareTo(cuota) >= 0) {
                resultado = "Aprobado";
            } else {
                resultado = "Revisión manual";
            }
            String explicacion=ExplicacionDecision.redactar(
                    resultado, puntaje, minimoAprobar, maximoRechazo, ingresos, gastos, deudas,
                    cuota, dti, disponible, dtiAprobar, dtiRechazar, atrasadas,
                    perfil.historialNegativo(), perfil.trabajaActualmente());
            if("Revisión manual".equals(resultado)) {
                if(garantia) explicacion += " Este tipo de crédito requiere verificar una garantía antes de resolver la solicitud.";
                if(excedeEdad) explicacion += " Se requiere comprobar la edad y evaluar las condiciones particulares con un supervisor.";
                if(faltaEmpleo) explicacion += " Se necesitan sustentos adicionales porque no registra empleo actual.";
            }
            marcar(evaluacion, solicitud, resultado, explicacion);
        }
        solicitudRepository.save(solicitud);
        return evaluacionRepository.save(evaluacion);
    }

    /** Cuota referencial de anualidad a tasa fija; no equivale a TCEA ni comprende comisiones. */
    static BigDecimal cuotaEstimada(BigDecimal monto, int meses, BigDecimal tasaAnual) {
        if(meses<=0) throw new IllegalArgumentException("El plazo debe ser positivo.");
        if(tasaAnual==null || tasaAnual.signum()==0) return monto.divide(BigDecimal.valueOf(meses),2,RoundingMode.HALF_UP);
        double tasaMensual=tasaAnual.doubleValue()/1200.0;
        double factor=1.0-Math.pow(1.0+tasaMensual,-meses);
        return BigDecimal.valueOf(monto.doubleValue()*tasaMensual/factor).setScale(2,RoundingMode.HALF_UP);
    }

    private static void marcar(Evaluacion ev, Solicitud solicitud, String resultado, String comentario) {
        ev.setResultado(resultado);
        ev.setComentario(comentario);
        solicitud.setEstado(switch (resultado) {
            case "Aprobado" -> "Aprobada";
            case "Rechazado" -> "Rechazada";
            default -> "Revisión manual";
        });
    }
    private static String normalizarRiesgo(String r) {
        return r == null || !java.util.List.of("Bajo", "Medio", "Alto").contains(r) ? "Por evaluar" : r;
    }
    private static BigDecimal cero(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }
}
