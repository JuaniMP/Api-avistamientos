package co.edu.bosque.avistamientos.application.port.out;

import co.edu.bosque.avistamientos.domain.Avistamiento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: lo que la aplicación necesita para guardar y consultar avistamientos.
 * Lo define la aplicación con sus propias palabras; la base de datos lo implementa desde
 * afuera (AdaptadorDePersistencia). Así el servicio no depende de Spring Data ni de JPA.
 */
public interface RepositorioDeAvistamientos {

    List<Avistamiento> buscarTodos();

    Optional<Avistamiento> buscarPorId(Long id);

    Avistamiento guardar(Avistamiento avistamiento);

    boolean existe(Long id);

    void eliminar(Long id);
}
