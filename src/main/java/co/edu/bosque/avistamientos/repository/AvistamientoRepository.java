package co.edu.bosque.avistamientos.repository;

import co.edu.bosque.avistamientos.entity.Avistamiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvistamientoRepository extends JpaRepository<Avistamiento, Long> {
}