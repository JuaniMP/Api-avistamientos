package co.edu.bosque.avistamientos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
public class Avistamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "la especie es obligatoria")
    private String especie;

    @NotBlank(message = "el lugar es obligatorio")
    private String lugar;

    @NotBlank(message = "la fecha es obligatoria")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "la fecha debe tener formato AAAA-MM-DD")
    private String fecha;

    @NotBlank(message = "el observador es obligatorio")
    private String observador;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getObservador() { return observador; }
    public void setObservador(String observador) { this.observador = observador; }
}