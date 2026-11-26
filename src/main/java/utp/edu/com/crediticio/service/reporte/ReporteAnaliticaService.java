package utp.edu.com.crediticio.service.reporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.reporte.ReporteResumen;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.evaluacion.EvaluacionRepository;

/**
 * Indicadores académicos a partir de decisiones persistidas, con los mismos filtros
 * de ReporteResumen. No interpreta solicitudes aprobadas como desembolsos ni mora real.
 * NO consulta la API externa: usa la fotografía de la evaluación guardada al decidir.
 */
@Service
public class ReporteAnaliticaService {
    private final EvaluacionRepository evaluaciones;

    public ReporteAnaliticaService(EvaluacionRepository evaluaciones) {
        this.evaluaciones = evaluaciones;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calcular(ReporteResumen resumen) {
        List<Solicitud> solicitudes = resumen.getSolicitudes();
        Set<Long> ids = solicitudes.stream().map(Solicitud::getIdSolicitud)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Evaluacion> porSolicitud = new HashMap<>();
        if (!ids.isEmpty()) {
            // Carga de evaluaciones: una consulta, sin realizar una llamada remota por fila.
            for (Evaluacion e : evaluaciones.buscarPorSolicitudes(new ArrayList<>(ids))) {
                porSolicitud.putIfAbsent(e.getSolicitud().getIdSolicitud(), e);
            }
        }
        long aprobadas = solicitudes.stream().filter(s -> estado(s).equals("Aprobada")).count();
        long rechazadas = solicitudes.stream().filter(s -> estado(s).equals("Rechazada")).count();
        long revision = solicitudes.stream().filter(s -> estado(s).equals("Revisión manual")).count();
        long pendientes = solicitudes.size() - aprobadas - rechazadas - revision;
        BigDecimal montoAprobado = sumaMonto(solicitudes.stream().filter(s -> estado(s).equals("Aprobada")).toList());
        BigDecimal montoRevision = sumaMonto(solicitudes.stream().filter(s -> estado(s).equals("Revisión manual")).toList());

        long casosConAtrasos = 0, casosSinTrabajo = 0, casosDtiAlto = 0, casosHistorial = 0;
        BigDecimal sumaScores = BigDecimal.ZERO, sumaDti = BigDecimal.ZERO;
        int scoresValidos = 0, dtisValidos = 0;
        for (Solicitud s : solicitudes) {
            Evaluacion e = porSolicitud.get(s.getIdSolicitud());
            if (e == null) continue;
            if (e.getPuntaje() != null && e.getPuntaje() > 0) {
                sumaScores = sumaScores.add(BigDecimal.valueOf(e.getPuntaje())); scoresValidos++;
            }
            if (e.getDti() != null) { sumaDti = sumaDti.add(e.getDti()); dtisValidos++; }
            if (e.getCuotasAtrasadas() != null && e.getCuotasAtrasadas() > 0) casosConAtrasos++;
            if (Boolean.FALSE.equals(e.getTrabajaActualmente())) casosSinTrabajo++;
            if (e.getDti() != null && e.getDti().compareTo(BigDecimal.valueOf(50)) >= 0) casosDtiAlto++;
            if (Boolean.TRUE.equals(e.getHistorialNegativo())) casosHistorial++;
        }

        Map<String,List<Solicitud>> porProducto = solicitudes.stream().collect(Collectors.groupingBy(
                s -> nombre(s.getTipoCredito(), "Sin tipo"), LinkedHashMap::new, Collectors.toList()));
        Map<String,List<Solicitud>> porRiesgo = solicitudes.stream().collect(Collectors.groupingBy(s -> {
            Evaluacion e = porSolicitud.get(s.getIdSolicitud());
            return e == null ? "Sin evaluar" : nombre(e.getNivelRiesgo(), "Sin clasificar");
        }, LinkedHashMap::new, Collectors.toList()));
        List<Map<String,Object>> tipos = grupos(porProducto);
        List<Map<String,Object>> riesgos = grupos(porRiesgo);
        List<Map<String,Object>> meses = new ArrayList<>();
        long maxMes = resumen.getResumenMensual().stream().mapToLong(m -> m.getTotal()).max().orElse(1);
        for (var mes : resumen.getResumenMensual()) {
            long restantes = mes.getPendientes(); // revisión manual + otros pendientes
            meses.add(barra(mes.getMes(), mes.getTotal(), mes.getAprobadas(), mes.getRechazadas(), restantes,
                    0, BigDecimal.ZERO, maxMes));
        }
        Map<String,Object> datos = new LinkedHashMap<>();
        datos.put("total", solicitudes.size());
        datos.put("clientesUnicos", solicitudes.stream().filter(s -> s.getCliente() != null)
                .map(s -> s.getCliente().getIdCliente()).distinct().count());
        datos.put("evaluadas", porSolicitud.size());
        datos.put("aprobadas", aprobadas);
        datos.put("rechazadas", rechazadas);
        datos.put("revision", revision);
        datos.put("pendientes", pendientes);
        datos.put("montoAprobado", montoAprobado);
        datos.put("montoRevision", montoRevision);
        datos.put("scorePromedio", promedio(sumaScores, scoresValidos, 0));
        datos.put("dtiPromedio", promedio(sumaDti, dtisValidos, 1));
        datos.put("tieneScore", scoresValidos > 0);
        datos.put("tieneDti", dtisValidos > 0);
        datos.put("casosConAtrasos", casosConAtrasos);
        datos.put("casosSinTrabajo", casosSinTrabajo);
        datos.put("casosDtiAlto", casosDtiAlto);
        datos.put("casosHistorial", casosHistorial);
        datos.put("pctAprob", porcentaje(aprobadas, solicitudes.size()));
        datos.put("pctRech", porcentaje(rechazadas, solicitudes.size()));
        datos.put("pctRevision", porcentaje(revision, solicitudes.size()));
        datos.put("pctPend", porcentaje(pendientes, solicitudes.size()));
        datos.put("pctRechAcum", porcentaje(aprobadas + rechazadas, solicitudes.size()));
        datos.put("pctRevAcum", porcentaje(aprobadas + rechazadas + revision, solicitudes.size()));
        datos.put("productos", tipos);
        datos.put("riesgos", riesgos);
        datos.put("meses", meses);
        return datos;
    }

    private static String nombre(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s.trim();
    }
    private static String estado(Solicitud s) {
        String valor = nombre(s.getEstado(), "Pendiente");
        if (valor.equalsIgnoreCase("Aprobado")) return "Aprobada";
        if (valor.equalsIgnoreCase("Rechazado")) return "Rechazada";
        return valor;
    }
    private static BigDecimal sumaMonto(List<Solicitud> lista) {
        return lista.stream().map(Solicitud::getMonto).filter(x -> x != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static BigDecimal promedio(BigDecimal total, long cantidad, int escala) {
        return cantidad == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(cantidad), escala, RoundingMode.HALF_UP);
    }
    private static BigDecimal porcentaje(long parte, long total) {
        return total <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(parte * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP);
    }
    private static List<Map<String,Object>> grupos(Map<String,List<Solicitud>> porGrupo) {
        long max = porGrupo.values().stream().mapToLong(List::size).max().orElse(1);
        return porGrupo.entrySet().stream().sorted((a,b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .map(e -> {
                    List<Solicitud> lista=e.getValue();
                    long ap=lista.stream().filter(s -> estado(s).equals("Aprobada")).count();
                    long re=lista.stream().filter(s -> estado(s).equals("Rechazada")).count();
                    long rv=lista.size()-ap-re;
                    return barra(e.getKey(), lista.size(), ap, re, rv, 0, sumaMonto(lista), max);
                }).toList();
    }
    private static Map<String,Object> barra(String etiqueta, long total, long aprobadas,
            long rechazadas, long revision, long pendientes, BigDecimal monto, long maximo) {
        Map<String,Object> valores=new LinkedHashMap<>();
        valores.put("etiqueta", etiqueta); valores.put("total", total);
        valores.put("aprobadas", aprobadas); valores.put("rechazadas", rechazadas);
        valores.put("revision", revision); valores.put("pendientes", pendientes);
        valores.put("monto", monto);
        valores.put("pctMax", porcentaje(total, maximo));
        valores.put("pctAprob", porcentaje(aprobadas, total));
        valores.put("pctRech", porcentaje(rechazadas, total));
        valores.put("pctRevision", porcentaje(revision, total));
        valores.put("pctPend", porcentaje(pendientes, total));
        valores.put("tasaAprobacion", porcentaje(aprobadas, total));
        return valores;
    }
}
