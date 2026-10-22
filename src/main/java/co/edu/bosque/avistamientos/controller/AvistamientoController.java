package co.edu.bosque.avistamientos.controller;

import co.edu.bosque.avistamientos.dto.AvistamientoRequest;
import co.edu.bosque.avistamientos.entity.Avistamiento;
import co.edu.bosque.avistamientos.service.AvistamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Solo traduce entre HTTP y el servicio: no tiene reglas de negocio.
 * Si los datos son inválidos, el servicio lanza DatosInvalidosException y el
 * ManejadorErrores responde 400.
 */
@RestController
@RequestMapping("/avistamientos")
@Tag(name = "Avistamientos", description = "Registro de avistamientos de aves")
public class AvistamientoController {

    private final AvistamientoService service;

    public AvistamientoController(AvistamientoService service) {
        this.service = service;
    }


    @GetMapping
    @Operation(summary = "Listar todos los avistamientos")
    public List<Avistamiento> listar() {
        return service.listar();
    }


    @GetMapping("/{id}")
    @Operation(summary = "Ver un avistamiento por id")
    @ApiResponse(responseCode = "200", description = "Avistamiento encontrado")
    @ApiResponse(responseCode = "404", description = "No existe un avistamiento con ese id", content = @Content)
    public ResponseEntity<Avistamiento> obtener(@PathVariable Long id) {
        return ResponseEntity.of(service.obtener(id));
    }


    @GetMapping("/resumen")
    @Operation(summary = "Contar avistamientos por especie")
    public Map<String, Long> resumen() {
        return service.resumen();
    }


    @PostMapping
    @Operation(summary = "Registrar un avistamiento")
    @ApiResponse(responseCode = "201", description = "Avistamiento creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos: dice qué campo falló y por qué",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                    examples = @ExampleObject(value = "{\"fecha\": \"la fecha no puede ser posterior a hoy\"}")))
    public ResponseEntity<Avistamiento> crear(@RequestBody AvistamientoRequest datos) {
        Avistamiento guardado = service.crear(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un avistamiento completo")
    @ApiResponse(responseCode = "200", description = "Avistamiento actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos: dice qué campo falló y por qué",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                    examples = @ExampleObject(value = "{\"fecha\": \"la fecha no puede ser posterior a hoy\"}")))
    @ApiResponse(responseCode = "404", description = "No existe un avistamiento con ese id", content = @Content)
    public ResponseEntity<Avistamiento> actualizar(@PathVariable Long id,
                                                   @RequestBody AvistamientoRequest datos) {
        return ResponseEntity.of(service.actualizar(id, datos));
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un avistamiento")
    @ApiResponse(responseCode = "204", description = "Avistamiento eliminado")
    @ApiResponse(responseCode = "404", description = "No existe un avistamiento con ese id", content = @Content)
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!service.eliminar(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
