package utp.edu.com.crediticio.service.externo;
public class ServicioExternoNoDisponibleException extends RuntimeException {
    public ServicioExternoNoDisponibleException() {
        super("No se pudo verificar el perfil en la API crediticia simulada (8081). No se registró ni evaluó la solicitud.");
    }
}
