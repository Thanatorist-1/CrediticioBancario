package utp.edu.com.crediticio.service.reporte;

import utp.edu.com.crediticio.model.reporte.ReporteResumen;
import utp.edu.com.crediticio.model.reporte.ReporteMensual;
import utp.edu.com.crediticio.model.solicitud.Solicitud;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

/** Crea el archivo PDF descargable a partir del mismo resumen que ve Reportes. */
@Service
public class ReportePdfService {
    private static final float MARGEN = 42;
    private static final float ANCHO = PDRectangle.A4.getWidth() - MARGEN * 2;
    private static final float LIMITE_INFERIOR = 48;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(ReporteResumen resumen, String fechaInicio, String fechaFin) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Paginas paginas = new Paginas(documento);
            paginas.titulo("Sistema Crediticio Bancario");
            paginas.texto("Reporte de solicitudes", true, 15);
            paginas.texto("Generado: " + LocalDate.now().format(FECHA), false, 10);
            paginas.texto("Periodo: " + periodo(fechaInicio, fechaFin), false, 10);
            paginas.espacio(10);

            paginas.seccion("Resumen general");
            paginas.texto("Solicitudes: " + resumen.getTotal()
                    + " | Aprobadas: " + resumen.getAprobadas()
                    + " | Rechazadas: " + resumen.getRechazadas()
                    + " | Pendientes: " + resumen.getPendientes(), false, 10);
            paginas.texto("Monto solicitado acumulado: S/ " + moneda(resumen.getMontoSolicitado()), false, 10);
            paginas.texto("Tasa de aprobacion: " + resumen.getTasaAprobacion()
                    + "% | Tasa de rechazo: " + resumen.getTasaRechazo() + "%", false, 10);
            paginas.espacio(10);

            paginas.seccion("Resumen mensual");
            paginas.texto("MES          TOTAL     APROBADAS     RECHAZADAS     PENDIENTES", true, 9);
            if (resumen.getResumenMensual().isEmpty()) paginas.texto("Sin registros para mostrar.", false, 9);
            for (ReporteMensual mes : resumen.getResumenMensual()) {
                paginas.texto(String.format("%-12s %-9d %-14d %-15d %d", mes.getMes(), mes.getTotal(),
                        mes.getAprobadas(), mes.getRechazadas(), mes.getPendientes()), false, 9);
            }
            paginas.espacio(10);

            paginas.seccion("Detalle de solicitudes");
            paginas.texto("CODIGO       CLIENTE                    TIPO              MONTO       FECHA       ESTADO", true, 8);
            if (resumen.getSolicitudes().isEmpty()) paginas.texto("Sin solicitudes para el periodo seleccionado.", false, 9);
            for (Solicitud solicitud : resumen.getSolicitudes()) {
                String codigo = acortar(valor(solicitud.getCodigo(), "SOL-" + solicitud.getIdSolicitud()), 12);
                String cliente = solicitud.getCliente() == null ? "Sin cliente" : valor(solicitud.getCliente().getNombre(), "Sin nombre");
                String tipo = acortar(valor(solicitud.getTipoCredito(), "Sin tipo"), 15);
                String monto = moneda(solicitud.getMonto());
                String fecha = acortar(valor(solicitud.getFecha(), "-"), 10);
                String estado = acortar(valor(solicitud.getEstado(), "Pendiente"), 14);
                paginas.texto(String.format("%-12s %-26s %-17s %-11s %-11s %s",
                        codigo, acortar(cliente, 25), tipo, monto, fecha, estado), false, 8);
            }
            paginas.pie();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el PDF de reportes.", exception);
        }
    }

    private static String periodo(String inicio, String fin) {
        boolean tieneInicio = inicio != null && !inicio.isBlank();
        boolean tieneFin = fin != null && !fin.isBlank();
        if (!tieneInicio && !tieneFin) return "Todos los registros";
        return (tieneInicio ? inicio : "Sin limite inicial") + " a " + (tieneFin ? fin : "Sin limite final");
    }

    private static String valor(String valor, String alternativo) {
        return valor == null || valor.isBlank() ? alternativo : valor;
    }

    private static String moneda(BigDecimal valor) {
        return valor == null ? "0.00" : valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String acortar(String valor, int maximo) {
        String limpio = ascii(valor).replaceAll("\\s+", " ").trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, Math.max(0, maximo - 3)) + "...";
    }

    /** Las fuentes base del PDF admiten WinAnsi; se normalizan acentos para mantener compatibilidad. */
    private static String ascii(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("—", "-").replace("–", "-").replace("S/", "S/")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static final class Paginas {
        private final PDDocument documento;
        private PDPageContentStream contenido;
        private float y;
        private int numero = 0;
        private final List<PDPage> paginas = new ArrayList<>();

        Paginas(PDDocument documento) { this.documento = documento; nuevaPagina(); }

        void titulo(String texto) { texto(texto, true, 19); }
        void seccion(String texto) { texto(texto, true, 12); }
        void espacio(float puntos) { y -= puntos; }

        void texto(String texto, boolean negrita, float tamano) {
            if (y < LIMITE_INFERIOR) nuevaPagina();
            try {
                contenido.beginText();
                contenido.setFont(negrita ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                        : new PDType1Font(Standard14Fonts.FontName.HELVETICA), tamano);
                contenido.newLineAtOffset(MARGEN, y);
                contenido.showText(ascii(texto));
                contenido.endText();
                y -= tamano + 5;
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo escribir el contenido del PDF.", exception);
            }
        }

        private void nuevaPagina() {
            try {
                if (contenido != null) contenido.close();
                PDPage pagina = new PDPage(PDRectangle.A4);
                documento.addPage(pagina);
                paginas.add(pagina);
                numero++;
                contenido = new PDPageContentStream(documento, pagina);
                y = PDRectangle.A4.getHeight() - MARGEN;
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo crear una pagina del PDF.", exception);
            }
        }

        void pie() {
            try {
                if (contenido != null) contenido.close();
                for (int i = 0; i < paginas.size(); i++) {
                    try (PDPageContentStream pie = new PDPageContentStream(documento, paginas.get(i),
                            PDPageContentStream.AppendMode.APPEND, true)) {
                        pie.beginText();
                        pie.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                        pie.newLineAtOffset(ANCHO + MARGEN - 55, 25);
                        pie.showText("Pagina " + (i + 1) + " de " + paginas.size());
                        pie.endText();
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo finalizar el PDF.", exception);
            }
        }
    }
}


