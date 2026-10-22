package co.edu.bosque.avistamientos.service;

import co.edu.bosque.avistamientos.dto.AvistamientoRequest;
import co.edu.bosque.avistamientos.reloj.RelojFijo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas UNITARIAS de la regla de negocio. No levantan Spring ni la base:
 * crean el validador con new y le pasan un RelojFijo (stub) que dice que hoy es 21/10/2026.
 * Los casos salen de la tabla de partición de equivalencia y valores límite
 * (docs/justificacion-pruebas.md, sección 2).
 */
class ValidadorDeAvistamientoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    private ValidadorDeAvistamiento validador;

    @BeforeEach
    void crearValidadorConRelojFijo() {
        validador = new ValidadorDeAvistamiento(new RelojFijo(HOY));
    }

    // Un avistamiento válido en todo, para cambiarle un solo campo en cada prueba
    private static AvistamientoRequest valido() {
        return new AvistamientoRequest("colibri", "Humedal La Conejera", "2026-09-29", "Juanita");
    }

    private static AvistamientoRequest conEspecie(String especie) {
        AvistamientoRequest v = valido();
        return new AvistamientoRequest(especie, v.lugar(), v.fecha(), v.observador());
    }

    private static AvistamientoRequest conFecha(String fecha) {
        AvistamientoRequest v = valido();
        return new AvistamientoRequest(v.especie(), v.lugar(), fecha, v.observador());
    }

    @Test
    void validar_conTodosLosCamposValidos_noDevuelveErrores() {
        // Arrange
        AvistamientoRequest datos = valido();

        // Act
        Map<String, String> errores = validador.validar(datos);

        // Assert
        assertThat(errores).isEmpty();
    }

    @Nested
    class Especie {

        @Test
        void validar_especieVacia_fallaPorqueEsObligatoria() {
            // Arrange
            AvistamientoRequest datos = conEspecie("");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsOnlyKeys("especie");
            assertThat(errores.get("especie")).isEqualTo("la especie es obligatoria");
        }

        @Test
        void validar_especieDe2Caracteres_fallaPorDebajoDelMinimo() {
            // Arrange
            AvistamientoRequest datos = conEspecie("ab");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsEntry("especie", "la especie debe tener entre 3 y 50 caracteres");
        }

        @Test
        void validar_especieDe3Caracteres_esValidaEnElLimiteInferior() {
            // Arrange
            AvistamientoRequest datos = conEspecie("abc");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).isEmpty();
        }

        @Test
        void validar_especieDe50Caracteres_esValidaEnElLimiteSuperior() {
            // Arrange
            AvistamientoRequest datos = conEspecie("a".repeat(50));

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).isEmpty();
        }

        @Test
        void validar_especieDe51Caracteres_fallaPorEncimaDelMaximo() {
            // Arrange
            AvistamientoRequest datos = conEspecie("a".repeat(51));

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsEntry("especie", "la especie debe tener entre 3 y 50 caracteres");
        }

        @Test
        void validar_especieDe2LetrasRodeadaDeEspacios_fallaPorqueLosEspaciosNoCuentan() {
            // Arrange
            AvistamientoRequest datos = conEspecie("   ab   ");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsEntry("especie", "la especie debe tener entre 3 y 50 caracteres");
        }

        @Test
        void validar_especieConSimbolos_fallaPorqueSoloAdmiteLetras() {
            // Arrange
            AvistamientoRequest datos = conEspecie("------");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsKey("especie");
            assertThat(errores.get("especie")).startsWith("la especie solo puede tener letras");
        }
    }

    @Nested
    class Fecha {

        @Test
        void validar_fechaPasada_esValida() {
            // Arrange
            AvistamientoRequest datos = conFecha("2026-09-29");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).isEmpty();
        }

        @Test
        void validar_fechaDeHoy_esValidaEnElLimite() {
            // Arrange
            AvistamientoRequest datos = conFecha("2026-10-21");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).isEmpty();
        }

        @Test
        void validar_fechaDeManana_fallaPorqueEsPosteriorAHoy() {
            // Arrange
            AvistamientoRequest datos = conFecha("2026-10-22");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsEntry("fecha", "la fecha no puede ser posterior a hoy");
        }

        @Test
        void validar_fechaConOtroFormato_fallaPorFormato() {
            // Arrange
            AvistamientoRequest datos = conFecha("21/10/2026");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsEntry("fecha", "la fecha debe tener el formato AAAA-MM-DD");
        }

        @Test
        void validar_fechaConFormatoCorrectoQueNoExiste_fallaPorqueNoEstaEnElCalendario() {
            // Arrange
            AvistamientoRequest datos = conFecha("2026-02-30");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsEntry("fecha", "la fecha no existe en el calendario");
        }

        @Test
        void validar_fechaNula_fallaPorqueEsObligatoria() {
            // Arrange
            AvistamientoRequest datos = conFecha(null);

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsEntry("fecha", "la fecha es obligatoria");
        }
    }

    @Nested
    class LugarYObservador {

        @Test
        void validar_lugarSoloConEspacios_fallaPorqueEsObligatorio() {
            // Arrange
            AvistamientoRequest datos = new AvistamientoRequest("colibri", "   ", "2026-09-29", "Juanita");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsOnlyKeys("lugar");
            assertThat(errores.get("lugar")).isEqualTo("el lugar es obligatorio");
        }

        @Test
        void validar_observadorVacio_fallaPorqueEsObligatorio() {
            // Arrange
            AvistamientoRequest datos = new AvistamientoRequest("colibri", "Chingaza", "2026-09-29", "");

            // Act
            Map<String, String> errores = validador.validar(datos);

            // Assert
            assertThat(errores).containsOnlyKeys("observador");
            assertThat(errores.get("observador")).isEqualTo("el observador es obligatorio");
        }
    }

    @Test
    void validar_variosCamposInvalidos_reportaUnMensajePorCadaCampo() {
        // Arrange
        AvistamientoRequest datos = new AvistamientoRequest("ab", "", "2026-10-22", null);

        // Act
        Map<String, String> errores = validador.validar(datos);

        // Assert
        assertThat(errores).containsOnlyKeys("especie", "lugar", "fecha", "observador");
    }
}
