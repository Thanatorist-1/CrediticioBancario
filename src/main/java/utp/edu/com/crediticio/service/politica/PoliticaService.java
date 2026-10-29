package utp.edu.com.crediticio.service.politica;

import utp.edu.com.crediticio.model.politica.Politica;
import utp.edu.com.crediticio.repository.politica.PoliticaRepository;

import java.util.List;
import org.springframework.stereotype.Service;

/** Reglas y operaciones de negocio sencillas para políticas. */
@Service
public class PoliticaService {
    private final PoliticaRepository politicaRepository;

    public PoliticaService(PoliticaRepository politicaRepository) {
        this.politicaRepository = politicaRepository;
    }

    public List<Politica> listar() { return politicaRepository.findAll(); }
    public Politica buscar(Long id) { return politicaRepository.findById(id).orElse(null); }

    public Politica guardar(Politica politica) {
        if (politica.getId() == null && (politica.getCodigo() == null || politica.getCodigo().isBlank())) {
            long consecutivo = politicaRepository.count() + 1;
            String codigo;
            do {
                codigo = "POL-%03d".formatted(consecutivo++);
            } while (politicaRepository.existsByCodigo(codigo));
            politica.setCodigo(codigo);
        }
        if (politica.getEstado() == null || politica.getEstado().isBlank()) politica.setEstado("Borrador");
        return politicaRepository.save(politica);
    }

    public void eliminar(Long id) { politicaRepository.deleteById(id); }
}


