package co.edu.bosque.avistamientos.reloj;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** El reloj real, el que usa la API en producción. */
@Component
public class RelojDelSistema implements Reloj {

    @Override
    public LocalDate hoy() {
        return LocalDate.now();
    }
}
