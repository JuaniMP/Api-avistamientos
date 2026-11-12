package co.edu.bosque.avistamientos.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de Spring Data. Solo lo usa AdaptadorDePersistencia. */
public interface AvistamientoRepositorioJpa extends JpaRepository<AvistamientoEntidadJpa, Long> {
}
