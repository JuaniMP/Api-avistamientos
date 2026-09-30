package co.edu.bosque.avistamientos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Entity
public class Avistamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "la especie es obligatoria")
    @Size(max = 100, message = "la especie no puede tener más de 100 caracteres")
    @Pattern(regexp = "\\p{L}[\\p{L} '-]*",
            message = "la especie solo puede tener letras, espacios, guiones o apóstrofes, y debe empezar con letra")
    private String especie;

    @NotBlank(message = "el lugar es obligatorio")
    @Size(max = 150, message = "el lugar no puede tener más de 150 caracteres")
    private String lugar;

    @NotNull(message = "la fecha es obligatoria")
    @PastOrPresent(message = "la fecha no puede ser futura")
    private LocalDate fecha;

    @NotBlank(message = "el observador es obligatorio")
    @Size(max = 100, message = "el observador no puede tener más de 100 caracteres")
    @Pattern(regexp = "\\p{L}[\\p{L} '.-]*",
            message = "el observador solo puede tener letras, espacios, puntos, guiones o apóstrofes")
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