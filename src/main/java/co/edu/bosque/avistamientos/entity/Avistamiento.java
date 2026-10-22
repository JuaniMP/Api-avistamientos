package co.edu.bosque.avistamientos.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

/**
 * Lo que se guarda en la base. Las reglas de validación ya no viven aquí,
 * sino en ValidadorDeAvistamiento, para poder probarlas sin levantar la API.
 */
@Entity
public class Avistamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Lo asigna el sistema", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long id;

    @Schema(example = "colibri")
    private String especie;

    @Schema(example = "Humedal La Conejera")
    private String lugar;

    @Schema(description = "Formato AAAA-MM-DD", example = "2026-09-29")
    private LocalDate fecha;

    @Schema(example = "Juanita")
    private String observador;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getObservador() { return observador; }
    public void setObservador(String observador) { this.observador = observador; }
}
