package co.edu.bosque.avistamientos.adapter.out.persistence;

import co.edu.bosque.avistamientos.application.port.out.RepositorioDeAvistamientos;
import co.edu.bosque.avistamientos.domain.Avistamiento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto RepositorioDeAvistamientos usando Spring Data JPA.
 * Es el único lugar que sabe que los datos viven en una base H2 con JPA, y el que convierte
 * entre el modelo de dominio (Avistamiento) y la entidad de la base (AvistamientoEntidadJpa).
 */
@Component
public class AdaptadorDePersistencia implements RepositorioDeAvistamientos {

    private final AvistamientoRepositorioJpa jpa;

    public AdaptadorDePersistencia(AvistamientoRepositorioJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Avistamiento> buscarTodos() {
        return jpa.findAll().stream().map(AdaptadorDePersistencia::aDominio).toList();
    }

    @Override
    public Optional<Avistamiento> buscarPorId(Long id) {
        return jpa.findById(id).map(AdaptadorDePersistencia::aDominio);
    }

    @Override
    public Avistamiento guardar(Avistamiento avistamiento) {
        AvistamientoEntidadJpa guardada = jpa.save(aEntidad(avistamiento));
        return aDominio(guardada);
    }

    @Override
    public boolean existe(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public void eliminar(Long id) {
        jpa.deleteById(id);
    }

    private static Avistamiento aDominio(AvistamientoEntidadJpa e) {
        Avistamiento a = new Avistamiento();
        a.setId(e.getId());
        a.setEspecie(e.getEspecie());
        a.setLugar(e.getLugar());
        a.setFecha(e.getFecha());
        a.setObservador(e.getObservador());
        return a;
    }

    private static AvistamientoEntidadJpa aEntidad(Avistamiento a) {
        AvistamientoEntidadJpa e = new AvistamientoEntidadJpa();
        e.setId(a.getId());
        e.setEspecie(a.getEspecie());
        e.setLugar(a.getLugar());
        e.setFecha(a.getFecha());
        e.setObservador(a.getObservador());
        return e;
    }
}
