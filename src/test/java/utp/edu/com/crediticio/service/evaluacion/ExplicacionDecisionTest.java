package utp.edu.com.crediticio.service.evaluacion;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExplicacionDecisionTest {
    @Test void rechazoExplicaIngresosBajosYCuotasAtrasadas() {
        String texto = ExplicacionDecision.redactar("Rechazado", 480,650,499,
            bd("2100"),bd("900"),bd("1050"),bd("833.33"),bd("89.68"),bd("150"),
            bd("35"),bd("50"),4,true,true);
        assertTrue(texto.contains("S/ 2100"));
        assertTrue(texto.contains("4 cuotas"));
        assertTrue(texto.contains("ingresos"));
        assertTrue(texto.contains("pagos"));
        assertTrue(texto.length() <= 1000);
    }
    @Test void aprobacionExplicaCuotaYDisponible() {
        String texto = ExplicacionDecision.redactar("Aprobado",742,650,499,
            bd("4800"),bd("1500"),bd("650"),bd("416.67"),bd("22.22"),bd("2650"),
            bd("35"),bd("50"),0,false,true);
        assertTrue(texto.contains("S/ 416.67"));
        assertTrue(texto.contains("S/ 2650"));
        assertTrue(texto.contains("no se registraron cuotas atrasadas"));
    }
    @Test void revisionExplicaEmpleoYDocumentos() {
        String texto=ExplicacionDecision.redactar("Revisión manual",600,650,499,
            bd("3100"),bd("900"),bd("800"),bd("350"),bd("37.10"),bd("1400"),
            bd("35"),bd("50"),1,false,false);
        assertTrue(texto.contains("no registras empleo"));
        assertTrue(texto.contains("1 cuota"));
        assertTrue(texto.contains("boletas"));
    }
    private static BigDecimal bd(String n) { return new BigDecimal(n); }
}
