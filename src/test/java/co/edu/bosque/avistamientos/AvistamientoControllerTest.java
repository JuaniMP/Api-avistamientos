package co.edu.bosque.avistamientos;

import co.edu.bosque.avistamientos.entity.Avistamiento;
import co.edu.bosque.avistamientos.repository.AvistamientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AvistamientoControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AvistamientoRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    // Arma el JSON de un avistamiento con la especie y fecha que le pasemos
    private String json(String especie, String fecha) {
        return """
               {"especie":"%s","lugar":"Chingaza","fecha":"%s","observador":"Juanita"}
               """.formatted(especie, fecha);
    }

    // Guarda un avistamiento directo en la base y devuelve su id
    private Long guardar(String especie) {
        Avistamiento a = new Avistamiento();
        a.setEspecie(especie);
        a.setLugar("Humedal La Conejera");
        a.setFecha(LocalDate.of(2026, 9, 29));
        a.setObservador("Juanita");
        return repository.save(a).getId();
    }

    // ---------- GET ----------

    @Test
    void listarDevuelve200ConLaLista() throws Exception {
        guardar("colibri");
        mockMvc.perform(get("/avistamientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void obtenerExistenteDevuelve200() throws Exception {
        Long id = guardar("colibri");
        mockMvc.perform(get("/avistamientos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especie").value("colibri"))
                .andExpect(jsonPath("$.fecha").value("2026-09-29"));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/avistamientos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resumenCuentaPorEspecie() throws Exception {
        guardar("colibri");
        guardar("colibri");
        guardar("tucan");
        mockMvc.perform(get("/avistamientos/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colibri").value(2))
                .andExpect(jsonPath("$.tucan").value(1));
    }

    // ---------- POST ----------

    @Test
    void crearDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tucan", "2026-09-28")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.especie").value("tucan"));
    }

    @Test
    void crearIncompletoDevuelve400ConMensajes() throws Exception {
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especie\":\"tucan\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.lugar").exists())
                .andExpect(jsonPath("$.fecha").exists())
                .andExpect(jsonPath("$.observador").exists());
    }

    @Test
    void crearConEspecieInvalidaDevuelve400() throws Exception {
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("------", "2026-09-28")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.especie").exists());
    }

    @Test
    void crearConFechaFuturaDevuelve400() throws Exception {
        String futura = LocalDate.now().plusYears(1).toString();
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tucan", futura)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fecha").value("la fecha no puede ser futura"));
    }

    @Test
    void crearConFechaQueNoExisteDevuelve400() throws Exception {
        mockMvc.perform(post("/avistamientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tucan", "2026-13-45")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ---------- PUT ----------

    @Test
    void actualizarDevuelve200() throws Exception {
        Long id = guardar("colibri");
        mockMvc.perform(put("/avistamientos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tucan", "2026-09-28")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.especie").value("tucan"));
    }

    @Test
    void actualizarInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/avistamientos/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("tucan", "2026-09-28")))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE ----------

    @Test
    void eliminarDevuelve204YLuegoYaNoExiste() throws Exception {
        Long id = guardar("colibri");
        mockMvc.perform(delete("/avistamientos/" + id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/avistamientos/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/avistamientos/999"))
                .andExpect(status().isNotFound());
    }
}