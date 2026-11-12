package co.edu.bosque.avistamientos.application.port.in;

import co.edu.bosque.avistamientos.domain.Avistamiento;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Puerto de entrada: lo que la aplicación ofrece hacia afuera (los casos de uso).
 * El controlador REST lo usa sin saber qué clase lo implementa; también lo podría
 * usar una prueba, una consola u otro sistema.
 */
public interface GestionarAvistamientos {

    List<Avistamiento> listar();

    Optional<Avistamiento> obtener(Long id);

    /** Cuántos avistamientos hay de cada especie. */
    Map<String, Long> resumen();

    /** Valida y guarda. Si los datos son inválidos lanza DatosInvalidosException. */
    Avistamiento crear(DatosAvistamiento datos);

    /** Devuelve vacío si no existe un avistamiento con ese id. */
    Optional<Avistamiento> actualizar(Long id, DatosAvistamiento datos);

    /** Devuelve false si no existía. */
    boolean eliminar(Long id);
}
