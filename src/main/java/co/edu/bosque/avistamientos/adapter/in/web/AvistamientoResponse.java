package co.edu.bosque.avistamientos.adapter.in.web;

import co.edu.bosque.avistamientos.domain.Avistamiento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Lo que la API devuelve en JSON. Tiene los mismos campos que antes devolvía la entidad,
 * así que las respuestas no cambian. El nombre del esquema en OpenAPI se mantiene "Avistamiento".
 */
@Schema(name = "Avistamiento")
public record AvistamientoResponse(
        @Schema(description = "Lo asigna el sistema", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
        Long id,
        @Schema(example = "colibri")
        String especie,
        @Schema(example = "Humedal La Conejera")
        String lugar,
        @Schema(description = "Formato AAAA-MM-DD", example = "2026-09-29")
        LocalDate fecha,
        @Schema(example = "Juanita")
        String observador) {

    static AvistamientoResponse desde(Avistamiento a) {
        return new AvistamientoResponse(a.getId(), a.getEspecie(), a.getLugar(), a.getFecha(), a.getObservador());
    }
}
