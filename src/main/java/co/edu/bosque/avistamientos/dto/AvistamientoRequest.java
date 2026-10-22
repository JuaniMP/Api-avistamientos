package co.edu.bosque.avistamientos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Lo que llega en el body del POST y del PUT.
 *
 * Todos los campos llegan como texto, incluida la fecha. Así el formato AAAA-MM-DD
 * lo revisa el ValidadorDeAvistamiento (y se puede probar con pruebas unitarias),
 * en lugar de que Jackson rechace la fecha antes de validar y responda un error
 * que no dice qué campo falló.
 *
 * Las anotaciones @Schema solo documentan las reglas en Swagger; no validan nada.
 * Quien valida es el ValidadorDeAvistamiento.
 */
@Schema(name = "AvistamientoRequest", description = "Datos para registrar o actualizar un avistamiento")
public record AvistamientoRequest(
        @Schema(requiredMode = REQUIRED, minLength = 3, maxLength = 50, pattern = "\\p{L}[\\p{L} '-]*",
                description = "Entre 3 y 50 caracteres; solo letras, espacios, guiones o apóstrofes",
                example = "colibri")
        String especie,

        @Schema(requiredMode = REQUIRED, maxLength = 150, description = "No puede estar vacío",
                example = "Humedal La Conejera")
        String lugar,

        @Schema(requiredMode = REQUIRED, format = "date",
                description = "Formato AAAA-MM-DD, debe existir y no puede ser posterior a hoy",
                example = "2026-09-29")
        String fecha,

        @Schema(requiredMode = REQUIRED, maxLength = 100, pattern = "\\p{L}[\\p{L} '.-]*",
                description = "No puede estar vacío; solo letras, espacios, puntos, guiones o apóstrofes",
                example = "Juanita")
        String observador) {
}
