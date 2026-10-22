package co.edu.bosque.avistamientos.service;

import co.edu.bosque.avistamientos.dto.AvistamientoRequest;
import co.edu.bosque.avistamientos.entity.Avistamiento;
import co.edu.bosque.avistamientos.exception.DatosInvalidosException;
import co.edu.bosque.avistamientos.reloj.RelojFijo;
import co.edu.bosque.avistamientos.repository.AvistamientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas UNITARIAS del servicio. El repositorio se reemplaza por un MOCK de Mockito:
 * - como stub, cuando hay que decidir qué responde (findAll, existsById, save);
 * - como mock, cuando lo que importa es verificar si se llamó o no (save, deleteById).
 * El validador es el real, con un RelojFijo: es lógica en memoria, rápida y determinista,
 * así que no hace falta reemplazarlo.
 */
@ExtendWith(MockitoExtension.class)
class AvistamientoServiceTest {

    @Mock
    private AvistamientoRepository repository;

    private AvistamientoService service;

    @BeforeEach
    void crearServicio() {
        ValidadorDeAvistamiento validador = new ValidadorDeAvistamiento(new RelojFijo(LocalDate.of(2026, 10, 21)));
        service = new AvistamientoService(repository, validador);
    }

    private static Avistamiento avistamiento(String especie) {
        Avistamiento a = new Avistamiento();
        a.setEspecie(especie);
        return a;
    }

    @Test
    void crear_conDatosValidos_guardaLaEntidadConLosDatosLimpiosYLaFechaConvertida() {
        // Arrange
        AvistamientoRequest datos = new AvistamientoRequest("  colibri ", "Chingaza", "2026-09-29", "Juanita");
        when(repository.save(any(Avistamiento.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Avistamiento> guardado = ArgumentCaptor.forClass(Avistamiento.class);

        // Act
        service.crear(datos);

        // Assert
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getEspecie()).isEqualTo("colibri");
        assertThat(guardado.getValue().getFecha()).isEqualTo(LocalDate.of(2026, 9, 29));
    }

    @Test
    void crear_conDatosInvalidos_lanzaDatosInvalidosYNoGuardaNada() {
        // Arrange
        AvistamientoRequest datos = new AvistamientoRequest("ab", "Chingaza", "2026-10-22", "Juanita");

        // Act
        DatosInvalidosException error = assertThrows(DatosInvalidosException.class, () -> service.crear(datos));

        // Assert
        assertThat(error.getErrores()).containsOnlyKeys("especie", "fecha");
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_idInexistente_devuelveVacioYNoGuarda() {
        // Arrange
        AvistamientoRequest datos = new AvistamientoRequest("tucan", "Chingaza", "2026-09-28", "Juanita");
        when(repository.existsById(99L)).thenReturn(false);

        // Act
        Optional<Avistamiento> resultado = service.actualizar(99L, datos);

        // Assert
        assertThat(resultado).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_idExistente_guardaConElMismoId() {
        // Arrange
        AvistamientoRequest datos = new AvistamientoRequest("tucan", "Chingaza", "2026-09-28", "Juanita");
        when(repository.existsById(5L)).thenReturn(true);
        when(repository.save(any(Avistamiento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Optional<Avistamiento> resultado = service.actualizar(5L, datos);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(5L);
        assertThat(resultado.get().getEspecie()).isEqualTo("tucan");
    }

    @Test
    void eliminar_idInexistente_devuelveFalseYNoBorra() {
        // Arrange
        when(repository.existsById(99L)).thenReturn(false);

        // Act
        boolean eliminado = service.eliminar(99L);

        // Assert
        assertThat(eliminado).isFalse();
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void eliminar_idExistente_borraEseIdYDevuelveTrue() {
        // Arrange
        when(repository.existsById(5L)).thenReturn(true);

        // Act
        boolean eliminado = service.eliminar(5L);

        // Assert
        assertThat(eliminado).isTrue();
        verify(repository).deleteById(5L);
    }

    @Test
    void resumen_conVariasEspecies_cuentaCuantasVecesApareceCadaUna() {
        // Arrange
        when(repository.findAll()).thenReturn(List.of(
                avistamiento("colibri"), avistamiento("tucan"), avistamiento("colibri")));

        // Act
        Map<String, Long> resumen = service.resumen();

        // Assert
        assertThat(resumen).containsExactly(Map.entry("colibri", 2L), Map.entry("tucan", 1L));
    }
}
