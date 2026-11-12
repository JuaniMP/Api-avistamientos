package co.edu.bosque.avistamientos.domain;

import java.time.LocalDate;

/**
 * Modelo de dominio: un avistamiento de un ave. Es una clase Java pura, sin anotaciones
 * de JPA, de Spring ni de Swagger, así que no depende de cómo se guarda ni de cómo se
 * muestra. La base de datos usa su propia clase (AvistamientoEntidadJpa) y la web la suya
 * (AvistamientoResponse); los adaptadores convierten entre ellas y esta.
 */
public class Avistamiento {

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
