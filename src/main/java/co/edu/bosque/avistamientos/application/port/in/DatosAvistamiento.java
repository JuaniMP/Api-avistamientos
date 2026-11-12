package co.edu.bosque.avistamientos.application.port.in;

/**
 * Los datos que llegan para registrar o actualizar un avistamiento, tal como los manda
 * quien usa la aplicación (todavía sin validar, por eso son texto). No depende de la web:
 * el adaptador web convierte su AvistamientoRequest en esto.
 */
public record DatosAvistamiento(String especie, String lugar, String fecha, String observador) {
}
