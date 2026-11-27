package utp.edu.com.crediticio.service.reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utp.edu.com.crediticio.model.evaluacion.Evaluacion;
import utp.edu.com.crediticio.model.reporte.ReporteResumen;
import utp.edu.com.crediticio.model.solicitud.DocumentoSolicitud;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.repository.evaluacion.EvaluacionRepository;
import utp.edu.com.crediticio.repository.solicitud.DocumentoSolicitudRepository;

/** Crea un libro Excel con el resumen y el detalle asociado a cada solicitud. */
@Service
public class ReporteExcelService {
    private final EvaluacionRepository evaluaciones;
    private final DocumentoSolicitudRepository documentos;
    private final ReporteAnaliticaService analiticaService;
    public ReporteExcelService(EvaluacionRepository e, DocumentoSolicitudRepository d, ReporteAnaliticaService analitica) {
        evaluaciones=e; documentos=d; analiticaService=analitica;
    }

    @Transactional(readOnly = true)
    public byte[] generar(ReporteResumen resumen) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle encabezado=workbook.createCellStyle(); Font negrita=workbook.createFont(); negrita.setBold(true); encabezado.setFont(negrita);
            Sheet resumenHoja=workbook.createSheet("Resumen");
            String[] columnasResumen={"Indicador","Resultado"};
            Row r=resumenHoja.createRow(0); escribir(r,columnasResumen,encabezado);
            String[][] valores={{"Solicitudes",String.valueOf(resumen.getTotal())},{"Aprobadas",String.valueOf(resumen.getAprobadas())},
                {"Rechazadas",String.valueOf(resumen.getRechazadas())},{"Revisión manual + pendiente",String.valueOf(resumen.getPendientes())},
                {"Monto solicitado",String.valueOf(resumen.getMontoSolicitado())},{"Tasa de aprobación (%)",String.valueOf(resumen.getTasaAprobacion())},
                {"Tasa de rechazo (%)",String.valueOf(resumen.getTasaRechazo())}};
            for(int i=0;i<valores.length;i++) escribir(resumenHoja.createRow(i+1),valores[i],null);
            int fila=valores.length+3; escribir(resumenHoja.createRow(fila++),new String[]{"Mes","Total","Aprobadas","Rechazadas","Revisión / pendiente"},encabezado);
            for(var mes:resumen.getResumenMensual()) escribir(resumenHoja.createRow(fila++),new String[]{mes.getMes(),""+mes.getTotal(),""+mes.getAprobadas(),""+mes.getRechazadas(),""+mes.getPendientes()},null);
            resumenHoja.autoSizeColumn(0); resumenHoja.autoSizeColumn(1);

            Sheet detalle=workbook.createSheet("Solicitudes");
            String[] titulos={"Código","Cliente","DNI","Correo","Tipo crédito","Monto","Plazo (meses)","Fecha","Estado",
                "Ingresos","Gastos","Deudas","Motivo","Puntaje","DTI (%)","Riesgo","Resultado evaluación","Política aplicada","Comentario / motivo de decisión","Documentos adjuntos"};
            escribir(detalle.createRow(0),titulos,encabezado);
            int n=1;
            for(Solicitud s:resumen.getSolicitudes()) {
                var c=s.getCliente(); Evaluacion ev=evaluaciones.findFirstBySolicitud_IdSolicitud(s.getIdSolicitud()).orElse(null);
                List<DocumentoSolicitud> docs=documentos.findBySolicitud_IdSolicitudOrderByFechaSubidaDesc(s.getIdSolicitud());
                String archivos=docs.stream().map(DocumentoSolicitud::getNombreArchivo).reduce((a,b)->a+", "+b).orElse("Sin adjuntos");
                escribir(detalle.createRow(n++),new String[]{s.getCodigo(),c==null?"":c.getNombre(),c==null?"":c.getDni(),c==null?"":c.getCorreo(),s.getTipoCredito(),texto(s.getMonto()),texto(s.getPlazoMeses()),s.getFecha(),s.getEstado(),texto(s.getIngresosMensuales()),texto(s.getGastosMensuales()),texto(s.getDeudasMensuales()),s.getMotivo(),ev==null?"":texto(ev.getPuntaje()),ev==null?"":texto(ev.getDti()),ev==null?"":ev.getNivelRiesgo(),ev==null?"":ev.getResultado(),ev==null?"":ev.getPolitica(),ev==null?"":ev.getComentario(),archivos},null);
            }
            for(int i=0;i<titulos.length;i++) detalle.autoSizeColumn(i);
            detalle.createFreezePane(0,1);
            if (n > 1) detalle.setAutoFilter(new CellRangeAddress(0,n-1,0,titulos.length-1));
            // Datos analíticos en una hoja de Excel adicional (mismos filtros que la pantalla).
            var datos = analiticaService.calcular(resumen);
            Sheet analitica=workbook.createSheet("Indicadores de riesgo");
            int pos=0;
            escribir(analitica.createRow(pos++),new String[]{"Indicador","Valor"},encabezado);
            String[][] indicadores={
                {"Clientes únicos",texto(datos.get("clientesUnicos"))},
                {"Evaluaciones con registro",texto(datos.get("evaluadas"))},
                {"Revisiones manuales",texto(datos.get("revision"))},
                {"Pendientes sin decisión",texto(datos.get("pendientes"))},
                {"Monto solicitado aprobado (NO desembolsado)",texto(datos.get("montoAprobado"))},
                {"Monto solicitado en revisión",texto(datos.get("montoRevision"))},
                {"Promedio score histórico",texto(datos.get("scorePromedio"))},
                {"Promedio DTI histórico (%)",texto(datos.get("dtiPromedio"))},
                {"Con cuotas atrasadas",texto(datos.get("casosConAtrasos"))},
                {"Con DTI desde 50%",texto(datos.get("casosDtiAlto"))},
                {"Sin empleo actual",texto(datos.get("casosSinTrabajo"))},
                {"Con historial negativo",texto(datos.get("casosHistorial"))}
            };
            for (String[] item : indicadores) escribir(analitica.createRow(pos++),item,null);
            pos+=2;
            escribir(analitica.createRow(pos++),new String[]{"Tipo de crédito", "Solicitudes", "Aprobadas", "Rechazadas", "Revisión/pendiente", "Tasa aprobación (%)"},encabezado);
            @SuppressWarnings("unchecked")
            List<java.util.Map<String,Object>> porProducto = (List<java.util.Map<String,Object>>)datos.get("productos");
            for (var grupo : porProducto) escribir(analitica.createRow(pos++),new String[]{
                texto(grupo.get("etiqueta")), texto(grupo.get("total")), texto(grupo.get("aprobadas")),
                texto(grupo.get("rechazadas")), texto(grupo.get("revision")), texto(grupo.get("tasaAprobacion"))},null);
            pos+=2;
            escribir(analitica.createRow(pos++),new String[]{"Segmento de riesgo", "Solicitudes", "Aprobadas", "Rechazadas", "Revisión/pendiente", "Tasa aprobación (%)"},encabezado);
            @SuppressWarnings("unchecked")
            List<java.util.Map<String,Object>> porRiesgo = (List<java.util.Map<String,Object>>)datos.get("riesgos");
            for (var grupo : porRiesgo) escribir(analitica.createRow(pos++),new String[]{
                texto(grupo.get("etiqueta")), texto(grupo.get("total")), texto(grupo.get("aprobadas")),
                texto(grupo.get("rechazadas")), texto(grupo.get("revision")), texto(grupo.get("tasaAprobacion"))},null);
            analitica.createFreezePane(0,1);
            for(int c=0;c<6;c++) analitica.setColumnWidth(c, c == 0 ? 12800 : 5600);
            workbook.write(salida); return salida.toByteArray();
        } catch(IOException ex) { throw new IllegalStateException("No se pudo generar el archivo Excel.",ex); }
    }
    private static String texto(Object valor) { return valor==null?"":valor.toString(); }
    private static void escribir(Row fila,String[] valores,CellStyle estilo) {
        for(int i=0;i<valores.length;i++) { Cell celda=fila.createCell(i); celda.setCellValue(valores[i]==null?"":valores[i]); if(estilo!=null) celda.setCellStyle(estilo); }
    }
}
