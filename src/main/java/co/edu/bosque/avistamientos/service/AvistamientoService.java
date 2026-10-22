package co.edu.bosque.avistamientos.service;

import co.edu.bosque.avistamientos.dto.AvistamientoRequest;
import co.edu.bosque.avistamientos.entity.Avistamiento;
import co.edu.bosque.avistamientos.exception.DatosInvalidosException;
import co.edu.bosque.avistamientos.repository.AvistamientoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * La lógica de los casos de uso: valida, convierte y guarda.
 * Recibe el repositorio y el validador por el constructor (inversión de dependencias),
 * así en las pruebas unitarias se le puede pasar un doble del repositorio.
 */
@Service
public class AvistamientoService {

    private final AvistamientoRepository repository;
    private final ValidadorDeAvistamiento validador;

    public AvistamientoService(AvistamientoRepository repository, ValidadorDeAvistamiento validador) {
        this.repository = repository;
        this.validador = validador;
    }

    public List<Avistamiento> listar() {
        return repository.findAll();
    }

    public Optional<Avistamiento> obtener(Long id) {
        return repository.findById(id);
    }

    public Map<String, Long> resumen() {
        Map<String, Long> conteo = new TreeMap<>();
        for (Avistamiento a : repository.findAll()) {
            conteo.merge(a.getEspecie(), 1L, Long::sum);
        }
        return conteo;
    }

    public Avistamiento crear(AvistamientoRequest datos) {
        validarOLanzar(datos);
        Avistamiento nuevo = aEntidad(datos);
        return repository.save(nuevo);
    }

    /** Devuelve vacío si no existe un avistamiento con ese id. */
    public Optional<Avistamiento> actualizar(Long id, AvistamientoRequest datos) {
        validarOLanzar(datos);
        if (!repository.existsById(id)) {
            return Optional.empty();
        }
        Avistamiento actualizado = aEntidad(datos);
        actualizado.setId(id);
        return Optional.of(repository.save(actualizado));
    }

    /** Devuelve false si no existía. */
    public boolean eliminar(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private void validarOLanzar(AvistamientoRequest datos) {
        Map<String, String> errores = validador.validar(datos);
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException(errores);
        }
    }

    private Avistamiento aEntidad(AvistamientoRequest datos) {
        Avistamiento a = new Avistamiento();
        a.setEspecie(datos.especie().trim());
        a.setLugar(datos.lugar().trim());
        a.setFecha(LocalDate.parse(datos.fecha()));
        a.setObservador(datos.observador().trim());
        return a;
    }
}
