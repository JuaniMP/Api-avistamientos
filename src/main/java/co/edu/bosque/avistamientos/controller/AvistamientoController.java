package co.edu.bosque.avistamientos.controller;

import co.edu.bosque.avistamientos.entity.Avistamiento;
import co.edu.bosque.avistamientos.repository.AvistamientoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/avistamientos")
public class AvistamientoController {

    private final AvistamientoRepository repository;

    public AvistamientoController(AvistamientoRepository repository) {
        this.repository = repository;
    }

    // GET /avistamientos -> 200 con la lista
    @GetMapping
    public List<Avistamiento> listar() {
        return repository.findAll();
    }

    // GET /avistamientos/{id} -> 200 con el avistamiento, o 404
    @GetMapping("/{id}")
    public ResponseEntity<Avistamiento> obtener(@PathVariable Long id) {
        Optional<Avistamiento> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(encontrado.get());
    }

    // POST /avistamientos -> 201 con el creado, o 400 si faltan datos
    @PostMapping
    public ResponseEntity<Avistamiento> crear(@Valid @RequestBody Avistamiento avistamiento) {
        avistamiento.setId(null);
        Avistamiento guardado = repository.save(avistamiento);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // PUT /avistamientos/{id} -> 200 con el actualizado, o 404
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

    // DELETE /avistamientos/{id} -> 204, o 404
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}