package co.edu.bosque.avistamientos.config;

import co.edu.bosque.avistamientos.application.port.in.GestionarAvistamientos;
import co.edu.bosque.avistamientos.application.port.out.Reloj;
import co.edu.bosque.avistamientos.application.port.out.RepositorioDeAvistamientos;
import co.edu.bosque.avistamientos.application.service.AvistamientoService;
import co.edu.bosque.avistamientos.application.service.ValidadorDeAvistamiento;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma la aplicación: conecta los adaptadores con los puertos.
 * Las clases de application y domain no tienen anotaciones de Spring; es aquí, afuera
 * del hexágono, donde se crean y se les pasa lo que necesitan. Spring encuentra solos
 * los adaptadores (RelojDelSistema, AdaptadorDePersistencia) porque tienen @Component.
 */
@Configuration
public class ConfiguracionAplicacion {

    @Bean
    ValidadorDeAvistamiento validadorDeAvistamiento(Reloj reloj) {
        return new ValidadorDeAvistamiento(reloj);
    }

    @Bean
    GestionarAvistamientos gestionarAvistamientos(RepositorioDeAvistamientos repositorio,
                                                  ValidadorDeAvistamiento validador) {
        return new AvistamientoService(repositorio, validador);
    }
}
