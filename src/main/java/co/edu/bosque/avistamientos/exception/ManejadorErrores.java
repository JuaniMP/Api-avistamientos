package co.edu.bosque.avistamientos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    /** Errores de la regla de negocio: un JSON campo -> mensaje. */
    @ExceptionHandler(DatosInvalidosException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> datosInvalidos(DatosInvalidosException ex) {
        return ex.getErrores();
    }

    /** El body no es un JSON que se pueda leer (por ejemplo, le falta una llave). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> jsonInvalido(HttpMessageNotReadableException ex) {
        return Map.of("error", "El cuerpo de la petición no es un JSON válido");
    }
}
