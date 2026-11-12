package co.edu.bosque.avistamientos.adapter.out.persistence;

import co.edu.bosque.avistamientos.application.port.out.RepositorioDeAvistamientos;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto RepositorioDeAvistamientos usando Spring Data JPA.
 * Es el único lugar que sabe que los datos viven en una base H2 con JPA.
 */
@Component
public class AdaptadorDePersistencia implements RepositorioDeAvistamientos {

    private final AvistamientoRepository jpa;

    public AdaptadorDePersistencia(AvistamientoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Avistamiento> buscarTodos() {
        return jpa.findAll();
    }

    @Override
    public Optional<Avistamiento> buscarPorId(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Avistamiento guardar(Avistamiento avistamiento) {
        return jpa.save(avistamiento);
    }

    @Override
    public boolean existe(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public void eliminar(Long id) {
        jpa.deleteById(id);
    }
}
