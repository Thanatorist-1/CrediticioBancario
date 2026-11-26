package utp.edu.com.crediticio.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.text.Normalizer;
import java.util.Locale;
import utp.edu.com.crediticio.repository.cliente.ClienteRepository;
import utp.edu.com.crediticio.repository.solicitud.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.model.cliente.RegistroClienteForm;
import utp.edu.com.crediticio.model.solicitud.Solicitud;
import utp.edu.com.crediticio.service.AutenticacionService;
import utp.edu.com.crediticio.service.cliente.ClienteService;
import utp.edu.com.crediticio.service.externo.ConsultaExternaService;
import utp.edu.com.crediticio.service.externo.PerfilCrediticioExterno;
import utp.edu.com.crediticio.service.solicitud.SolicitudService;

/**
 * Carga intencional y repetible SOLO PARA DEMOSTRACIÓN. Usa la API (8081) para los
 * 50 primeros DNI ficticios; registra sus cuentas por el flujo validado y crea
 * solicitudes por el servicio normal, que activa la evaluación automática.
 * No borra registros anteriores ni duplica solicitudes al reiniciar.
 * Desactivar con DEMO_SEED_ENABLED=false fuera del entorno académico.
 */
@Component
@ConditionalOnProperty(name="demo.seed.enabled", havingValue="true")
public class SemilleroDemoExterno implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SemilleroDemoExterno.class);
    private static final String CLAVE_DEMO = "DemoCredito2026!";
    private static final String[] TIPOS = {"Personal", "Vehicular", "Hipotecario", "Emprendimiento", "Educativo"};
    private static final int[] PLAZOS = {12, 18, 24, 36, 48, 60};
    private final ConsultaExternaService externa;
    private final ClienteService clientes;
    private final AutenticacionService autenticacion;
    private final SolicitudService solicitudes;
    private final int cantidad;
    private final ClienteRepository clienteRepository;
    private final SolicitudRepository solicitudRepository;
    private static final String[] DISTRITOS = {"Miraflores", "Surco", "Pueblo Libre", "San Miguel", "Los Olivos", "Ate", "Chorrillos", "San Borja", "La Molina", "Comas", "Breña", "Lince"};
    private static final String[] CALLES = {"Av. Los Álamos", "Jr. Las Palmeras", "Calle Los Girasoles", "Av. La Marina", "Jr. Santa Rosa", "Av. Próceres", "Calle Las Orquídeas", "Jr. Los Eucaliptos"};
    private static final String[][] EMPLEOS = {
        {"Servicios Logísticos Andinos SAC", "Coordinador de operaciones"},
        {"Comercial San Martín EIRL", "Asesor comercial"},
        {"Tecnología Altura SAC", "Técnico de soporte"},
        {"Clínica Los Cedros SAC", "Asistente administrativo"},
        {"Distribuidora Horizonte SAC", "Encargado de almacén"},
        {"Instituto Horizonte", "Docente"},
        {"Textiles del Pacífico SAC", "Supervisor de producción"},
        {"Estudio Contable Rivera", "Auxiliar contable"},
        {"Transportes Rápidos del Sur SAC", "Conductor de reparto"},
        {"Restaurante Patio del Sol", "Administrador de local"}
    };

    public SemilleroDemoExterno(ConsultaExternaService externa, ClienteService clientes,
            AutenticacionService autenticacion, SolicitudService solicitudes,
            @Value("${demo.seed.count:50}") int cantidad,
            ClienteRepository clienteRepository, SolicitudRepository solicitudRepository) {
        this.externa=externa; this.clientes=clientes; this.autenticacion=autenticacion;
        this.solicitudes=solicitudes;
        this.clienteRepository=clienteRepository;
        this.solicitudRepository=solicitudRepository;
        this.cantidad=Math.min(100, Math.max(50, cantidad));
    }

    @Override
    public void run(ApplicationArguments args) {
        int existentes=0, nuevos=0, nuevasSolicitudes=0;
        for (int n=1; n<=cantidad; n++) {
            String dni=String.format("%08d", 90000000+n);
            // Todas las altas tienen origen en una CONSULTA REAL al servicio externo.
            PerfilCrediticioExterno perfil=externa.verificarPersonaActiva(dni);
            Cliente cuenta=clientes.buscarPorDni(dni);
            if (cuenta==null) {
                RegistroClienteForm form=new RegistroClienteForm();
                form.setDni(dni); form.setNombre(perfil.nombreCompleto());
                form.setFechaNacimiento(perfil.fechaNacimiento());
                form.setCorreo(correoDemostracion(perfil.nombreCompleto(), n));
                form.setTelefono(telefonoDemo(n));
                form.setDireccion(direccionDemo(n));
                String[] empleo = EMPLEOS[(n * 7 + 3) % EMPLEOS.length];
                form.setEmpresa(perfil.trabajaActualmente()?empleo[0]:"Actividad no declarada");
                form.setCargo(perfil.trabajaActualmente()?empleo[1]:"Sin ocupación actual");
                form.setAntiguedadMeses(perfil.trabajaActualmente()?12 + (n * 19) % 126:0);
                form.setIngresosMensuales(perfil.ingresosMensuales());
                form.setDeudasMensuales(perfil.deudaMensual());
                form.setGastosMensuales(gastosDeEjemplo(perfil.ingresosMensuales(), n));
                form.setClave(CLAVE_DEMO);
                autenticacion.registrarCliente(form); // vuelve a cotejar nombre y fecha con la API
                cuenta=clientes.buscarPorDni(dni);
                if (cuenta==null) throw new IllegalStateException("No se creó cliente de prueba " + dni);
                nuevos++;
            } else {
                if (cuenta.getPasswordHash()==null || cuenta.getPasswordHash().isBlank())
                    throw new IllegalStateException("Cliente previo sin cuenta registrada: "+dni+". Ejecute limpieza controlada.");
                existentes++;
                actualizarDatosGenericosSoloDemo(cuenta, perfil, n);
            }
            // Corrige únicamente motivos iniciales genéricos; no modifica solicitudes del cliente.
            actualizarMotivosDemo(cuenta, n);
            // Si alguna solicitud ya existe, NO se sobrescribe ni se duplica en próximos arranques.
            if (!solicitudes.listarPorCliente(cuenta.getIdCliente()).isEmpty()) continue;
            Random random=new Random(20261124L+n);
            int total=1+random.nextInt(3); // entre 1 y 3 solicitudes por cliente
            for (int j=0;j<total;j++) {
                Solicitud solicitud=new Solicitud();
                solicitud.setIdCliente(cuenta.getIdCliente());
                solicitud.setTipoCredito(TIPOS[random.nextInt(TIPOS.length)]);
                solicitud.setMonto(BigDecimal.valueOf(1000+random.nextInt(55)*500L));
                solicitud.setPlazoMeses(PLAZOS[random.nextInt(PLAZOS.length)]);
                solicitud.setMotivo(motivoPorTipo(solicitud.getTipoCredito(), n, j));
                solicitud.setGastosMensuales(cuenta.getGastosMensuales());
                // SolicitudService -> EvaluacionAutomaticaService -> API en tiempo real.
                solicitudes.guardar(solicitud);
                nuevasSolicitudes++;
            }
        }
        log.info("SEMILLA DEMO EXTERNA COMPLETADA: perfiles verificados={}, cuentas nuevas={}, previas={}, solicitudes nuevas={}; DNI 90000001..{}",
                cantidad, nuevos, existentes, nuevasSolicitudes, String.format("%08d", 90000000+cantidad));
        log.warn("Cuentas académicas generadas tienen clave temporal conocida. No usar esta carga en producción (DEMO_SEED_ENABLED=false).");
    }

    private static BigDecimal gastosDeEjemplo(BigDecimal ingreso, int n) {
        return ingreso == null ? BigDecimal.ZERO : ingreso.multiply(BigDecimal.valueOf(19 + (n * 7) % 24))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    private static String correoDemostracion(String nombre, int n) {
        String base=Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ").trim().replaceAll(" +", ".");
        return base + "." + String.format("%03d", n) + "@demo.test";
    }
    private static String telefonoDemo(int n) { return "9" + String.format("%08d", 25000000 + n * 1729); }
    private static String direccionDemo(int n) {
        return CALLES[(n * 3) % CALLES.length] + " " + (125 + n * 13) + ", "
                + DISTRITOS[(n * 5) % DISTRITOS.length] + ", Lima";
    }
    private void actualizarDatosGenericosSoloDemo(Cliente cuenta, PerfilCrediticioExterno perfil, int n) {
        boolean cambios=false;
        if (("cliente"+perfil.dni()+"@demo.test").equals(cuenta.getCorreo())) { cuenta.setCorreo(correoDemostracion(perfil.nombreCompleto(), n)); cambios=true; }
        if (("Calle Simulada " + n + ", Lima").equals(cuenta.getDireccion())) { cuenta.setDireccion(direccionDemo(n)); cambios=true; }
        if (("Empresa ficticia " + n).equals(cuenta.getEmpresa()) && perfil.trabajaActualmente()) {
            String[] empleo=EMPLEOS[(n * 7 + 3) % EMPLEOS.length]; cuenta.setEmpresa(empleo[0]); cambios=true;
        }
        if ("Colaborador de demostración".equals(cuenta.getCargo()) && perfil.trabajaActualmente()) {
            cuenta.setCargo(EMPLEOS[(n * 7 + 3) % EMPLEOS.length][1]); cambios=true;
        }
        if ("No aplica".equals(cuenta.getCargo()) && !perfil.trabajaActualmente()) { cuenta.setCargo("Sin ocupación actual"); cambios=true; }
        if ("Sin empleo actual".equals(cuenta.getEmpresa()) && !perfil.trabajaActualmente()) { cuenta.setEmpresa("Actividad no declarada"); cambios=true; }
        if (telefonoDemoAnterior(n).equals(cuenta.getTelefono())) { cuenta.setTelefono(telefonoDemo(n)); cambios=true; }
        if (perfil.trabajaActualmente() && Integer.valueOf(6+n%96).equals(cuenta.getAntiguedadMeses())) {
            cuenta.setAntiguedadMeses(12 + (n * 19) % 126); cambios=true;
        }
        if (cuenta.getGastosMensuales() != null &&
                cuenta.getGastosMensuales().compareTo(perfil.ingresosMensuales()
                    .multiply(new BigDecimal("0.28")).setScale(2,RoundingMode.HALF_UP)) == 0) {
            // Cambia solo el gasto predeterminado de las cuentas DEMO, sin modificar evaluaciones históricas.
            cuenta.setGastosMensuales(gastosDeEjemplo(perfil.ingresosMensuales(), n)); cambios=true;
        }
        if (cambios) clienteRepository.save(cuenta);
    }
    private static String telefonoDemoAnterior(int n) { return String.format("9%08d", n); }
    private void actualizarMotivosDemo(Cliente cuenta, int n) {
        var lista=solicitudes.listarPorCliente(cuenta.getIdCliente());
        int i=0;
        for (Solicitud solicitud:lista) {
            if (solicitud.getMotivo() != null && solicitud.getMotivo().matches("Prueba académica [0-9]+ de perfil [0-9]{8}")) {
                solicitud.setMotivo(motivoPorTipo(solicitud.getTipoCredito(), n, i));
                solicitudRepository.save(solicitud); // Actualiza solo el texto: NO reevalúa el crédito.
            }
            i++;
        }
    }
    private static String motivoPorTipo(String tipo, int n, int indice) {
        String[] ejemplos=switch(tipo) {
            case "Vehicular" -> new String[]{"Renovar el automóvil utilizado para trasladarse al trabajo", "Adquirir una camioneta para reparto de mercadería", "Comprar una motocicleta para viajes laborales"};
            case "Hipotecario" -> new String[]{"Financiar la primera vivienda familiar", "Adquirir un departamento cerca del centro de trabajo", "Ampliar una vivienda para la familia"};
            case "Emprendimiento" -> new String[]{"Comprar equipos para ampliar un negocio de alimentos", "Adquirir inventario y mobiliario para una tienda", "Modernizar maquinaria de un taller familiar"};
            case "Educativo" -> new String[]{"Cubrir matrícula y materiales de estudios superiores", "Financiar un programa de especialización profesional", "Pagar un diplomado de actualización técnica"};
            default -> new String[]{"Renovar instalaciones eléctricas de la vivienda", "Consolidar gastos de una remodelación del hogar", "Comprar electrodomésticos para el domicilio"};
        };
        String destino = DISTRITOS[(n * 5) % DISTRITOS.length];
        return ejemplos[(n + indice) % ejemplos.length] + " en " + destino + ".";
    }
}
