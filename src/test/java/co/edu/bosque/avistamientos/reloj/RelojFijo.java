package co.edu.bosque.avistamientos.reloj;

import java.time.LocalDate;

/**
 * Doble de prueba de tipo STUB: siempre responde el mismo día, el que se le pasa al crearlo.
 * Con él, "hoy" y "mañana" son fechas fijas y las pruebas dan el mismo resultado
 * cualquier día que se corran (propiedad R de FIRST: repetible).
 */
public class RelojFijo implements Reloj {

    private final LocalDate dia;

    public RelojFijo(LocalDate dia) {
        this.dia = dia;
    }

    @Override
    public LocalDate hoy() {
        return dia;
    }
}
