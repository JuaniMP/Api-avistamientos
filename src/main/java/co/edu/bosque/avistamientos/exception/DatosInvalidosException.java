package co.edu.bosque.avistamientos.exception;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** La lanza el servicio cuando el validador encuentra errores. El ManejadorErrores la convierte en un 400. */
public class DatosInvalidosException extends RuntimeException {

    private final Map<String, String> errores;

    public DatosInvalidosException(Map<String, String> errores) {
        super("Datos inválidos: " + errores);
        this.errores = Collections.unmodifiableMap(new TreeMap<>(errores)); // ordenados por campo
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
