package co.edu.bosque.avistamientos.controller;

import co.edu.bosque.avistamientos.entity.Avistamiento;
import co.edu.bosque.avistamientos.repository.AvistamientoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.TreeMap;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/avistamientos")
public class AvistamientoController {

    private final AvistamientoRepository repository;

    public AvistamientoController(AvistamientoRepository repository) {
        this.repository = repository;
    }


    @GetMapping
    public List<Avistamiento> listar() {
        return repository.findAll();
    }


    @GetMapping("/{id}")
    public ResponseEntity<Avistamiento> obtener(@PathVariable Long id) {
        Optional<Avistamiento> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(encontrado.get());
    }

    @GetMapping("/resumen")
    public Map<String, Long> resumen() {
        Map<String, Long> conteo = new TreeMap<>();
        for (Avistamiento a : repository.findAll()) {
            String especie = a.getEspecie();
            conteo.put(especie, conteo.getOrDefault(especie, 0L) + 1);
        }
        return conteo;
    }


    @PostMapping
    public ResponseEntity<Avistamiento> crear(@Valid @RequestBody Avistamiento avistamiento) {
        avistamiento.setId(null);
        Avistamiento guardado = repository.save(avistamiento);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Avistamiento> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody Avistamiento datos) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        datos.setId(id);
        Avistamiento actualizado = repository.save(datos);
        return ResponseEntity.ok(actualizado);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}