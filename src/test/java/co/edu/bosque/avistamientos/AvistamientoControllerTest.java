package co.edu.bosque.avistamientos;

import co.edu.bosque.avistamientos.entity.Avistamiento;
import com.jayway.jsonpath.JsonPath;
import co.edu.bosque.avistamientos.reloj.Reloj;
import co.edu.bosque.avistamientos.reloj.RelojFijo;
import co.edu.bosque.avistamientos.repository.AvistamientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de INTEGRACIÓN: levantan Spring completo (controlador, servicio, validador,
 * manejador de errores, JPA y una H2 en memoria) y llaman a la API por HTTP con MockMvc.
 * Comprueban lo que la prueba unitaria del validador no ve: que el JSON se lea bien,
 * que el controlador use el validador, que el error se convierta en 400 con el campo
 * y que los datos queden de verdad guardados, cambiados o borrados en la base.
 */
@SpringBootTest
class AvistamientoControllerTest {

    /** Hoy, para las pruebas. Así "hoy" y "mañana" no cambian según el día en que se corran. */
    static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    /** Reemplaza el reloj real por un stub en el contexto de Spring de estas pruebas. */
    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        Reloj relojFijo() {
            return new RelojFijo(HOY);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AvistamientoRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll(); // cada prueba empieza con la base vacía (independiente)
    }

    // Arma el JSON de un avistamiento con la especie y fecha que le pasemos
    private static String json(String especie, String fecha) {
        return """
               {"especie":"%s","lugar":"Chingaza","fecha":"%s","observador":"Juanita"}
               """.formatted(especie, fecha);
    }

    // Guarda un avistamiento directo en la base (sin pasar por la API) y devuelve su id
    private Long guardar(String especie) {
        Avistamiento a = new Avistamiento();
        a.setEspecie(especie);
        a.setLugar("Humedal La Conejera");
        a.setFecha(LocalDate.of(2026, 9, 29));
        a.setObservador("Juanita");
        return repository.save(a).getId();
    }

    // Saca el id del JSON de respuesta, para poder consultarlo después
    private static Long idDe(MvcResult resultado) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString();
        Number id = JsonPath.read(cuerpo, "$.id");
        return id.longValue();
    }

    // ---------- GET ----------

    @Test
    void listar_conUnAvistamientoGuardado_devuelve200ConLaLista() throws Exception {
        // Arrange
        guardar("colibri");

        // Act + Assert
        mockMvc.perform(get("/avistamientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].especie").value("colibri"));
    }

    @Test
    void obtener_idExistente_devuelve200ConElAvistamiento() throws Exception {
        // Arrange
        Long id = guardar("colibri");

        // Act + Assert
        mockMvc.perform(get("/avistamientos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especie").value("colibri"))
                .andExpect(jsonPath("$.fecha").value("2026-09-29"));
    }

    @Test
    void obtener_idInexistente_devuelve404() throws Exception {
        // Arrange: base vacía (la deja así el @BeforeEach)

        // Act + Assert
        mockMvc.perform(get("/avistamientos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resumen_conVariasEspecies_cuentaPorEspecie() throws Exception {
        // Arrange
        guardar("colibri");
        guardar("colibri");
        guardar("tucan");

        // Act + Assert
        mockMvc.perform(get("/avistamientos/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colibri").value(2))
                .andExpect(jsonPath("$.tucan").value(1));
    }

    // ---------- POST ----------

    @Test
    void crear_conDatosValidos_devuelve201YQuedaGuardado() throws Exception {
        // Arrange
        String cuerpo = json("tucan", "2026-09-28");

        // Act
        MvcResult resultado = mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        // Assert: el efecto, que se pueda consultar después
        Long id = idDe(resultado);
        mockMvc.perform(get("/avistamientos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especie").value("tucan"))
                .andExpect(jsonPath("$.fecha").value("2026-09-28"));
    }

    @Test
    void crear_conFechaDeHoy_devuelve201PorqueHoyEsValido() throws Exception {
        // Arrange
        String cuerpo = json("tucan", HOY.toString());

        // Act + Assert
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    void crear_conFechaDeManana_devuelve400ConElCampoFechaYNoGuarda() throws Exception {
        // Arrange
        String cuerpo = json("tucan", HOY.plusDays(1).toString());

        // Act
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fecha").value("la fecha no puede ser posterior a hoy"));

        // Assert: no quedó nada guardado
        assertThat(repository.count()).isZero();
    }

    @Test
    void crear_conEspecieDe2Caracteres_devuelve400ConElCampoEspecie() throws Exception {
        // Arrange
        String cuerpo = json("ab", "2026-09-28");

        // Act + Assert
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.especie").value("la especie debe tener entre 3 y 50 caracteres"));
    }

    @Test
    void crear_conFechaQueNoExiste_devuelve400DiciendoQueFallaLaFecha() throws Exception {
        // Arrange
        String cuerpo = json("tucan", "2026-13-45");

        // Act + Assert
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fecha").value("la fecha no existe en el calendario"));
    }

    @Test
    void crear_incompleto_devuelve400ConUnMensajePorCadaCampoFaltante() throws Exception {
        // Arrange
        String cuerpo = "{\"especie\":\"tucan\"}";

        // Act + Assert
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.lugar").value("el lugar es obligatorio"))
                .andExpect(jsonPath("$.fecha").value("la fecha es obligatoria"))
                .andExpect(jsonPath("$.observador").value("el observador es obligatorio"));
    }

    @Test
    void crear_conJsonMalEscrito_devuelve400() throws Exception {
        // Arrange
        String cuerpo = "{\"especie\":\"tucan\",";

        // Act + Assert
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ---------- PUT ----------

    @Test
    void actualizar_idExistenteConDatosValidos_devuelve200YQuedaCambiado() throws Exception {
        // Arrange
        Long id = guardar("colibri");

        // Act
        mockMvc.perform(put("/avistamientos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tucan", "2026-09-28")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // Assert: el efecto, en la base quedó el dato nuevo
        assertThat(repository.findById(id)).get()
                .extracting(Avistamiento::getEspecie).isEqualTo("tucan");
    }

    @Test
    void actualizar_conDatosInvalidos_devuelve400YNoCambiaNada() throws Exception {
        // Arrange
        Long id = guardar("colibri");

        // Act
        mockMvc.perform(put("/avistamientos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ab", "2026-09-28")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.especie").exists());

        // Assert: sigue el dato original
        assertThat(repository.findById(id)).get()
                .extracting(Avistamiento::getEspecie).isEqualTo("colibri");
    }

    @Test
    void actualizar_idInexistente_devuelve404() throws Exception {
        // Arrange: base vacía

        // Act + Assert
        mockMvc.perform(put("/avistamientos/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tucan", "2026-09-28")))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE ----------

    @Test
    void eliminar_idExistente_devuelve204YLuegoYaNoExiste() throws Exception {
        // Arrange
        Long id = guardar("colibri");

        // Act
        mockMvc.perform(delete("/avistamientos/" + id))
                .andExpect(status().isNoContent());

        // Assert: el efecto, ya no se puede consultar
        mockMvc.perform(get("/avistamientos/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_idInexistente_devuelve404() throws Exception {
        // Arrange: base vacía

        // Act + Assert
        mockMvc.perform(delete("/avistamientos/999"))
                .andExpect(status().isNotFound());
    }
}
