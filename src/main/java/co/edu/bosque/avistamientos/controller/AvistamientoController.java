package co.edu.bosque.avistamientos.controller;

import co.edu.bosque.avistamientos.entity.Avistamiento;
import co.edu.bosque.avistamientos.repository.AvistamientoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Avistamientos", description = "Registro de avistamientos de aves")
public class AvistamientoController {

    private final AvistamientoRepository repository;

    public AvistamientoController(AvistamientoRepository repository) {
        this.repository = repository;
    }


    @GetMapping
    @Operation(summary = "Listar todos los avistamientos")
    public List<Avistamiento> listar() {
        return repository.findAll();
    }


    @GetMapping("/{id}")
    @Operation(summary = "Ver un avistamiento por id")
    @ApiResponse(responseCode = "200", description = "Avistamiento encontrado")
    @ApiResponse(responseCode = "404", description = "No existe un avistamiento con ese id", content = @Content)
    public ResponseEntity<Avistamiento> obtener(@PathVariable Long id) {
        Optional<Avistamiento> encontrado = repository.findById(id);
        if (encontrado.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(encontrado.get());
    }

    @GetMapping("/resumen")
    @Operation(summary = "Contar avistamientos por especie")
    public Map<String, Long> resumen() {
        Map<String, Long> conteo = new TreeMap<>();
        for (Avistamiento a : repository.findAll()) {
            String especie = a.getEspecie();
            conteo.put(especie, conteo.getOrDefault(especie, 0L) + 1);
        }
        return conteo;
    }


    @PostMapping
    @Operation(summary = "Registrar un avistamiento")
    @ApiResponse(responseCode = "201", description = "Avistamiento creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos: dice qué campo falló y por qué",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                    examples = @ExampleObject(value = "{\"fecha\": \"la fecha no puede ser futura\"}")))
    public ResponseEntity<Avistamiento> crear(@Valid @RequestBody Avistamiento avistamiento) {
        avistamiento.setId(null);
        Avistamiento guardado = repository.save(avistamiento);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un avistamiento completo")
    @ApiResponse(responseCode = "200", description = "Avistamiento actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos: dice qué campo falló y por qué",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                    examples = @ExampleObject(value = "{\"fecha\": \"la fecha no puede ser futura\"}")))
    @ApiResponse(responseCode = "404", description = "No existe un avistamiento con ese id", content = @Content)
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
    @Operation(summary = "Eliminar un avistamiento")
    @ApiResponse(responseCode = "204", description = "Avistamiento eliminado")
    @ApiResponse(responseCode = "404", description = "No existe un avistamiento con ese id", content = @Content)
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
