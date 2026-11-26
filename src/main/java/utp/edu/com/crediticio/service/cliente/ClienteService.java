package utp.edu.com.crediticio.service.cliente;

import utp.edu.com.crediticio.model.cliente.Cliente;
import utp.edu.com.crediticio.repository.cliente.ClienteRepository;

import java.util.List;
import org.springframework.stereotype.Service;

/** Reglas y operaciones de negocio sencillas para clientes. */
@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listar() { return clienteRepository.findAll(); }
    public Cliente buscar(Long id) { return clienteRepository.findById(id).orElse(null); }
    public Cliente buscarPorDni(String dni) { return clienteRepository.findFirstByDni(dni).orElse(null); }

    public Cliente guardar(Cliente cliente) {
        if (cliente.getIdCliente() == null)
            throw new IllegalArgumentException("Los clientes solo pueden crear su cuenta desde Registro, validando el DNI con la API simulada.");
        Cliente cuenta = buscar(cliente.getIdCliente());
        if (cuenta == null || cuenta.getPasswordHash() == null || cuenta.getPasswordHash().isBlank())
            throw new IllegalArgumentException("La cuenta del cliente no existe o no fue registrada.");
        // DNI, identidad y datos financieros externos no se modifican desde administración.
        cliente.setDni(cuenta.getDni());
        cliente.setNombre(cuenta.getNombre());
        cliente.setFechaNacimiento(cuenta.getFechaNacimiento());
        cliente.setTrabajaActualmente(cuenta.getTrabajaActualmente());
        cliente.setIngresosMensuales(cuenta.getIngresosMensuales());
        cliente.setDeudasMensuales(cuenta.getDeudasMensuales());
        if (cliente.getEstado() == null || cliente.getEstado().isBlank()) cliente.setEstado("Activo");
        Cliente clienteConDni = buscarPorDni(cliente.getDni());
        if (clienteConDni != null && !clienteConDni.getIdCliente().equals(cliente.getIdCliente())) {
            throw new IllegalArgumentException("Ya existe un cliente con ese DNI.");
        }
        cliente.setCodigo(cuenta.getCodigo());
        cliente.setPasswordHash(cuenta.getPasswordHash());
        return clienteRepository.save(cliente);
    }

    public void eliminar(Long id) { clienteRepository.deleteById(id); }
}


