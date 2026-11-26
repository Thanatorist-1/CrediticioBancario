package utp.edu.com.crediticio.service.politica;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import utp.edu.com.crediticio.model.politica.*;
import utp.edu.com.crediticio.model.usuario.Usuario;
import utp.edu.com.crediticio.repository.politica.*;

@Service
public class TipoCreditoService {
    private final TipoCreditoRepository repositorio;
    private final PoliticaRepository politicas;
    private final AuditoriaConfiguracionService auditoria;
    public TipoCreditoService(TipoCreditoRepository r, PoliticaRepository p, AuditoriaConfiguracionService a) {
        repositorio=r; politicas=p; auditoria=a;
    }
    public List<TipoCredito> listar(){return repositorio.findAllByOrderByNombreAsc();}
    public TipoCredito buscar(Long id){return repositorio.findById(id).orElse(null);}
    public TipoCredito buscarPorNombre(String nombre){return repositorio.findFirstByNombreIgnoreCase(nombre).orElse(null);}
    public TipoCredito buscarPorCodigo(String codigo){return repositorio.findFirstByCodigoIgnoreCase(codigo).orElse(null);}
    public List<TipoCredito> disponibles(){return repositorio.findByActivoTrueOrderByNombreAsc().stream()
        .filter(t -> politicas.findFirstByProductoIgnoreCaseAndEstadoIgnoreCaseOrderByIdDesc(t.getNombre(),"Activa").isPresent()).toList();}
    public TipoCredito exigirDisponible(String codigo) {
        TipoCredito tipo=buscarPorCodigo(codigo);
        if (tipo==null || !tipo.isActivo() || politicas.findFirstByProductoIgnoreCaseAndEstadoIgnoreCaseOrderByIdDesc(tipo.getNombre(),"Activa").isEmpty())
            throw new IllegalArgumentException("Este producto crediticio no está disponible o no tiene una política activa.");
        return tipo;
    }
    @Transactional
    public TipoCredito guardar(TipoCredito datos, Usuario actor){
        exigirAdmin(actor);
        if(datos.getCodigo()==null || !datos.getCodigo().matches("[A-Za-z][A-Za-z0-9_-]{2,39}"))
            throw new IllegalArgumentException("Código de 3 a 40 caracteres sin espacios.");
        if(datos.getNombre()==null || datos.getNombre().isBlank() || datos.getNombre().length()>100)
            throw new IllegalArgumentException("El nombre es obligatorio, máximo 100 caracteres.");
        String codigo=datos.getCodigo().trim(); String nombre=datos.getNombre().trim();
        TipoCredito entidad; Map<String,Object> antes=null;
        if(datos.getId()==null){
            if(buscarPorCodigo(codigo)!=null) throw new IllegalArgumentException("Ya existe un tipo de crédito con ese código.");
            entidad=new TipoCredito();entidad.setCodigo(codigo);
        } else {
            entidad=repositorio.findById(datos.getId()).orElseThrow(()->new IllegalArgumentException("Tipo de crédito no encontrado."));
            antes=snapshot(entidad);
            if(!entidad.getCodigo().equals(codigo)) throw new IllegalArgumentException("El código de un producto no se puede cambiar.");
            if(!entidad.getNombre().equals(nombre) && !politicas.findByProductoIgnoreCase(entidad.getNombre()).isEmpty())
                throw new IllegalArgumentException("No puedes cambiar el nombre de un producto con políticas asociadas.");
        }
        TipoCredito repetido=buscarPorNombre(nombre);
        if(repetido!=null && !Objects.equals(repetido.getId(),entidad.getId())) throw new IllegalArgumentException("Ya existe ese nombre de crédito.");
        entidad.setNombre(nombre); entidad.setDescripcion(datos.getDescripcion());entidad.setActivo(datos.isActivo());
        entidad=repositorio.saveAndFlush(entidad);
        auditoria.guardar("TIPO_CREDITO",entidad.getId(),antes==null?"CREADO":"MODIFICADO",actor,antes,snapshot(entidad));
        return entidad;
    }
    @Transactional
    public void desactivar(Long id, Usuario actor) {
        exigirAdmin(actor);
        TipoCredito t=repositorio.findById(id).orElseThrow(()->new IllegalArgumentException("Tipo de crédito no encontrado."));
        Map<String,Object> antes=snapshot(t);t.setActivo(false);repositorio.save(t);
        auditoria.guardar("TIPO_CREDITO",id,"DESACTIVADO",actor,antes,snapshot(t));
    }
    private static void exigirAdmin(Usuario actor){
        if(actor==null||!"Administrador".equals(actor.getRol())||!"Activo".equals(actor.getEstado()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el administrador puede cambiar el catálogo.");
    }
    private static Map<String,Object> snapshot(TipoCredito t){
        Map<String,Object> m=new LinkedHashMap<>();m.put("codigo",t.getCodigo());m.put("nombre",t.getNombre());
        m.put("descripcion",t.getDescripcion());m.put("activo",t.isActivo());return m;
    }
}
