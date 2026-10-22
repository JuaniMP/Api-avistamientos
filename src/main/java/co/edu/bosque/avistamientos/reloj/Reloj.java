package co.edu.bosque.avistamientos.reloj;

import java.time.LocalDate;

/**
 * Dice qué día es hoy.
 *
 * Existe para que la validación no dependa directamente del reloj del sistema:
 * en producción se usa RelojDelSistema y en las pruebas un RelojFijo (stub),
 * así la prueba de "fecha de mañana" da el mismo resultado hoy y dentro de un mes.
 */
public interface Reloj {

    LocalDate hoy();
}
