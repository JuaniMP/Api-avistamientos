package co.edu.bosque.avistamientos.application.service;

import co.edu.bosque.avistamientos.application.port.in.DatosAvistamiento;
import co.edu.bosque.avistamientos.application.port.in.GestionarAvistamientos;
import co.edu.bosque.avistamientos.domain.Avistamiento;
import co.edu.bosque.avistamientos.domain.DatosInvalidosException;
import co.edu.bosque.avistamientos.application.port.out.RepositorioDeAvistamientos;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Implementa el puerto de entrada GestionarAvistamientos: valida, convierte y guarda.
 * Recibe el puerto de salida RepositorioDeAvistamientos y el validador por el constructor
 * (inversión de dependencias): depende de una interfaz propia, no de Spring Data,
 * así en las pruebas unitarias se le puede pasar un doble del repositorio.
 */
@Service
public class AvistamientoService implements GestionarAvistamientos {

    private final RepositorioDeAvistamientos repositorio;
    private final ValidadorDeAvistamiento validador;

    public AvistamientoService(RepositorioDeAvistamientos repositorio, ValidadorDeAvistamiento validador) {
        this.repositorio = repositorio;
        this.validador = validador;
    }

    @Override
    public List<Avistamiento> listar() {
        return repositorio.buscarTodos();
    }

    @Override
    public Optional<Avistamiento> obtener(Long id) {
        return repositorio.buscarPorId(id);
    }

    @Override
    public Map<String, Long> resumen() {
        Map<String, Long> conteo = new TreeMap<>();
        for (Avistamiento a : repositorio.buscarTodos()) {
            conteo.merge(a.getEspecie(), 1L, Long::sum);
        }
        return conteo;
    }

    @Override
    public Avistamiento crear(DatosAvistamiento datos) {
        validarOLanzar(datos);
        Avistamiento nuevo = aDominio(datos);
        return repositorio.guardar(nuevo);
    }

    @Override
    public Optional<Avistamiento> actualizar(Long id, DatosAvistamiento datos) {
        validarOLanzar(datos);
        if (!repositorio.existe(id)) {
            return Optional.empty();
        }
        Avistamiento actualizado = aDominio(datos);
        actualizado.setId(id);
        return Optional.of(repositorio.guardar(actualizado));
    }

    @Override
    public boolean eliminar(Long id) {
        if (!repositorio.existe(id)) {
            return false;
        }
        repositorio.eliminar(id);
        return true;
    }

    private void validarOLanzar(DatosAvistamiento datos) {
        Map<String, String> errores = validador.validar(datos);
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException(errores);
        }
    }

    private Avistamiento aDominio(DatosAvistamiento datos) {
        Avistamiento a = new Avistamiento();
        a.setEspecie(datos.especie().trim());
        a.setLugar(datos.lugar().trim());
        a.setFecha(LocalDate.parse(datos.fecha()));
        a.setObservador(datos.observador().trim());
        return a;
    }
}
