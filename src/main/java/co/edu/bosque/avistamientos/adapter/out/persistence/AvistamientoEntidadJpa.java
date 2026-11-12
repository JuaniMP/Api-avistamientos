package co.edu.bosque.avistamientos.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Cómo se guarda un avistamiento en la base de datos (JPA). Solo la usa el adaptador
 * de persistencia; el resto de la aplicación trabaja con domain.Avistamiento.
 * La tabla se sigue llamando "avistamiento", así los datos que ya existían se conservan.
 */
@Entity
@Table(name = "avistamiento")
public class AvistamientoEntidadJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String especie;
    private String lugar;
    private LocalDate fecha;
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
