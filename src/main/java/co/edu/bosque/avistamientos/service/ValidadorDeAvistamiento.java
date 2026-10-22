package co.edu.bosque.avistamientos.service;

import co.edu.bosque.avistamientos.dto.AvistamientoRequest;
import co.edu.bosque.avistamientos.reloj.Reloj;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Regla de negocio del taller. Un avistamiento es válido si:
 * - la especie tiene entre 3 y 50 caracteres (y solo letras, espacios, guiones o apóstrofes),
 * - la fecha tiene el formato AAAA-MM-DD, existe y no es posterior a hoy,
 * - el lugar y el observador no están vacíos.
 *
 * No sabe nada de HTTP ni de la base de datos: recibe los datos y devuelve los errores.
 * Por eso se puede probar sin levantar la API.
 */
@Component
public class ValidadorDeAvistamiento {

    public static final int ESPECIE_MIN = 3;
    public static final int ESPECIE_MAX = 50;
    public static final int LUGAR_MAX = 150;
    public static final int OBSERVADOR_MAX = 100;

    private static final String SOLO_LETRAS_ESPECIE = "\\p{L}[\\p{L} '-]*";
    private static final String SOLO_LETRAS_OBSERVADOR = "\\p{L}[\\p{L} '.-]*";
    private static final String FORMATO_FECHA = "\\d{4}-\\d{2}-\\d{2}";

    private final Reloj reloj;

    public ValidadorDeAvistamiento(Reloj reloj) {
        this.reloj = reloj;
    }

    /**
     * Devuelve un mapa campo -> mensaje con lo que falló.
     * Si el mapa está vacío, el avistamiento es válido.
     */
    public Map<String, String> validar(AvistamientoRequest datos) {
        Map<String, String> errores = new TreeMap<>();
        if (datos == null) {
            errores.put("avistamiento", "no se recibieron datos");
            return errores;
        }
        validarEspecie(datos.especie(), errores);
        validarFecha(datos.fecha(), errores);
        validarLugar(datos.lugar(), errores);
        validarObservador(datos.observador(), errores);
        return errores;
    }

    private void validarEspecie(String especie, Map<String, String> errores) {
        if (estaVacio(especie)) {
            errores.put("especie", "la especie es obligatoria");
            return;
        }
        String limpia = especie.trim();
        if (limpia.length() < ESPECIE_MIN || limpia.length() > ESPECIE_MAX) {
            errores.put("especie", "la especie debe tener entre " + ESPECIE_MIN + " y " + ESPECIE_MAX + " caracteres");
            return;
        }
        if (!limpia.matches(SOLO_LETRAS_ESPECIE)) {
            errores.put("especie",
                    "la especie solo puede tener letras, espacios, guiones o apóstrofes, y debe empezar con letra");
        }
    }

    private void validarFecha(String fecha, Map<String, String> errores) {
        if (estaVacio(fecha)) {
            errores.put("fecha", "la fecha es obligatoria");
            return;
        }
        if (!fecha.matches(FORMATO_FECHA)) {
            errores.put("fecha", "la fecha debe tener el formato AAAA-MM-DD");
            return;
        }
        LocalDate dia;
        try {
            dia = LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            errores.put("fecha", "la fecha no existe en el calendario");
            return;
        }
        if (dia.isAfter(reloj.hoy())) {
            errores.put("fecha", "la fecha no puede ser posterior a hoy");
        }
    }

    private void validarLugar(String lugar, Map<String, String> errores) {
        if (estaVacio(lugar)) {
            errores.put("lugar", "el lugar es obligatorio");
        } else if (lugar.trim().length() > LUGAR_MAX) {
            errores.put("lugar", "el lugar no puede tener más de " + LUGAR_MAX + " caracteres");
        }
    }

    private void validarObservador(String observador, Map<String, String> errores) {
        if (estaVacio(observador)) {
            errores.put("observador", "el observador es obligatorio");
        } else if (observador.trim().length() > OBSERVADOR_MAX) {
            errores.put("observador", "el observador no puede tener más de " + OBSERVADOR_MAX + " caracteres");
        } else if (!observador.trim().matches(SOLO_LETRAS_OBSERVADOR)) {
            errores.put("observador",
                    "el observador solo puede tener letras, espacios, puntos, guiones o apóstrofes");
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
