package utp.edu.com.crediticio.service.politica;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import utp.edu.com.crediticio.model.politica.*;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.politica.PoliticaRepository;
import java.math.BigDecimal;
import java.util.*;

@Service
public class PoliticaService {
    private final PoliticaRepository politicas;
    private final TipoCreditoService tipos;
    private final AuditoriaConfiguracionService auditoria;
    public PoliticaService(PoliticaRepository r, TipoCreditoService t, AuditoriaConfiguracionService a) {
        politicas=r; tipos=t; auditoria=a;
    }
    public List<Politica> listar(){return politicas.findAll();}
    public Politica buscar(Long id){return politicas.findById(id).orElse(null);}
    @Transactional
    public Politica guardar(Politica datos, Usuario actor) {
        exigirAdmin(actor);
        if (datos.getProducto() == null || datos.getProducto().isBlank()) throw new IllegalArgumentException("Selecciona un tipo de crédito.");
        TipoCredito tipo = tipos.buscarPorNombre(datos.getProducto());
        if (tipo == null) throw new IllegalArgumentException("El tipo de crédito no existe en el catálogo.");
        if ("Activa".equals(datos.getEstado()) && !tipo.isActivo())
            throw new IllegalArgumentException("Activa primero el tipo de crédito antes de activar su política.");
        if (datos.getVersion() == null || datos.getVersion().isBlank() || datos.getVigencia() == null || datos.getVigencia().isBlank()
                || datos.getDescripcion() == null || datos.getDescripcion().isBlank())
            throw new IllegalArgumentException("Versión, vigencia y descripción son obligatorias.");
        int min = obligatorio(datos.getPuntajeMinimoAprobar(), "Puntaje para aprobar");
        int max = obligatorio(datos.getPuntajeMaximoRechazo(), "Puntaje para rechazar");
        if (min < 300 || min > 850 || max < 0 || max > 849 || max >= min)
            throw new IllegalArgumentException("Puntajes inválidos: rechazo debe ser inferior a aprobación (0 a 850).");
        BigDecimal aprobar = Objects.requireNonNull(datos.getDtiMaximoAprobacion(), "DTI de aprobación obligatorio.");
        BigDecimal rechazar = Objects.requireNonNull(datos.getDtiMinimoRechazo(), "DTI de rechazo obligatorio.");
        if (aprobar.signum() <= 0 || aprobar.compareTo(new BigDecimal("100")) > 0 || rechazar.signum() <= 0 || rechazar.compareTo(new BigDecimal("100")) > 0 ||
                aprobar.compareTo(rechazar) >= 0)
            throw new IllegalArgumentException("DTI inválido: aprobación debe ser menor que rechazo, entre 0 y 100 %.");
        BigDecimal minMonto = datos.getMontoMinimo() == null ? new BigDecimal("500") : datos.getMontoMinimo();
        BigDecimal maxMonto = datos.getMontoMaximo() == null ? new BigDecimal("200000") : datos.getMontoMaximo();
        int minPlazo = datos.getPlazoMinimoMeses() == null ? 6 : datos.getPlazoMinimoMeses();
        int maxPlazo = datos.getPlazoMaximoMeses() == null ? 120 : datos.getPlazoMaximoMeses();
        BigDecimal tasa = datos.getTasaAnualReferencial() == null ? BigDecimal.ZERO : datos.getTasaAnualReferencial();
        if (minMonto.compareTo(new BigDecimal("500")) < 0 || maxMonto.compareTo(new BigDecimal("10000000")) > 0 || minMonto.compareTo(maxMonto) > 0)
            throw new IllegalArgumentException("Monto permitido: entre S/ 500 y S/ 10 000 000, mínimo menor o igual al máximo.");
        if (minPlazo < 1 || maxPlazo > 360 || minPlazo > maxPlazo)
            throw new IllegalArgumentException("Plazos inválidos: mínimo debe ser menor o igual al máximo (hasta 360 meses).");
        if (tasa.signum() < 0 || tasa.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Tasa anual referencial fuera de 0 a 100%.");
        if (datos.getEdadMaximaAnios() != null && (datos.getEdadMaximaAnios() < 18 || datos.getEdadMaximaAnios() > 120))
            throw new IllegalArgumentException("Edad máxima debe estar entre 18 y 120 años.");
        Politica entidad;
        Map<String,Object> antes = null;
        if (datos.getId() == null) {
            entidad = new Politica();
            entidad.setCodigo("POL-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT));
        } else {
            entidad = politicas.findById(datos.getId()).orElseThrow(() -> new IllegalArgumentException("Política no encontrada."));
            antes = snapshot(entidad);
            if (!entidad.getProducto().equalsIgnoreCase(datos.getProducto()))
                throw new IllegalArgumentException("El producto de una política existente no se puede cambiar; crea otra versión.");
        }
        entidad.setProducto(tipo.getNombre()); entidad.setVersion(datos.getVersion().trim());
        entidad.setVigencia(datos.getVigencia().trim()); entidad.setDescripcion(datos.getDescripcion().trim());
        String estado=datos.getEstado()==null?"Borrador":datos.getEstado();
        if(!Set.of("Activa","Borrador","Inactiva").contains(estado))
            throw new IllegalArgumentException("El estado indicado no es válido.");
        entidad.setEstado(estado);
        entidad.setPuntajeMinimoAprobar(min); entidad.setPuntajeMaximoRechazo(max);
        entidad.setDtiMaximoAprobacion(aprobar); entidad.setDtiMinimoRechazo(rechazar);
        entidad.setMontoMinimo(minMonto); entidad.setMontoMaximo(maxMonto);
        entidad.setPlazoMinimoMeses(minPlazo); entidad.setPlazoMaximoMeses(maxPlazo);
        entidad.setTasaAnualReferencial(tasa);
        entidad.setRequiereGarantia(Boolean.TRUE.equals(datos.getRequiereGarantia()));
        entidad.setRequiereEmpleoActivo(!Boolean.FALSE.equals(datos.getRequiereEmpleoActivo()));
        entidad.setEdadMaximaAnios(datos.getEdadMaximaAnios());
        if ("Activa".equals(entidad.getEstado())) {
            for (Politica otra : politicas.findByProductoIgnoreCaseAndEstadoIgnoreCase(tipo.getNombre(), "Activa")) {
                if (!Objects.equals(otra.getId(), entidad.getId())) {
                    Map<String,Object> anterior = snapshot(otra); otra.setEstado("Inactiva"); politicas.save(otra);
                    auditoria.guardar("POLITICA", otra.getId(), "DESACTIVADA_POR_VERSION", actor, anterior, snapshot(otra));
                }
            }
        }
        entidad=politicas.saveAndFlush(entidad);
        auditoria.guardar("POLITICA",entidad.getId(),antes==null?"CREADA":"MODIFICADA", actor, antes, snapshot(entidad));
        return entidad;
    }
    @Transactional
    public void desactivar(Long id, Usuario actor) {
        exigirAdmin(actor);
        Politica p=politicas.findById(id).orElseThrow(() -> new IllegalArgumentException("Política no encontrada."));
        Map<String,Object> antes=snapshot(p);
        p.setEstado("Inactiva");politicas.save(p);
        auditoria.guardar("POLITICA", id, "DESACTIVADA", actor, antes, snapshot(p));
    }
    private static int obligatorio(Integer n, String campo) {if(n==null) throw new IllegalArgumentException(campo+" obligatorio."); return n;}
    private static void exigirAdmin(Usuario u) {
        if (u==null || !"Administrador".equals(u.getRol()) || !"Activo".equalsIgnoreCase(u.getEstado()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el administrador puede modificar políticas.");
    }
    private static Map<String,Object> snapshot(Politica p) {
        Map<String,Object> m=new LinkedHashMap<>(); m.put("codigo",p.getCodigo());m.put("producto",p.getProducto());
        m.put("version",p.getVersion());m.put("vigencia",p.getVigencia());m.put("estado",p.getEstado());
        m.put("descripcion",p.getDescripcion());m.put("puntajeMinimoAprobar",p.getPuntajeMinimoAprobar());
        m.put("puntajeMaximoRechazo",p.getPuntajeMaximoRechazo());m.put("dtiMaximoAprobacion",p.getDtiMaximoAprobacion());
        m.put("dtiMinimoRechazo",p.getDtiMinimoRechazo());
        m.put("montoMinimo",p.getMontoMinimo());m.put("montoMaximo",p.getMontoMaximo());
        m.put("plazoMinimoMeses",p.getPlazoMinimoMeses());m.put("plazoMaximoMeses",p.getPlazoMaximoMeses());
        m.put("tasaAnualReferencial",p.getTasaAnualReferencial());m.put("requiereGarantia",p.getRequiereGarantia());
        m.put("requiereEmpleoActivo",p.getRequiereEmpleoActivo());m.put("edadMaximaAnios",p.getEdadMaximaAnios());return m;
    }
}
