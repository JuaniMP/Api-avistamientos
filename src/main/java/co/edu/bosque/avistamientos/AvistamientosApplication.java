package co.edu.bosque.avistamientos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
		title = "API de avistamientos de aves",
		version = "1.0",
		description = "Registro de avistamientos de aves."))
@SpringBootApplication
public class AvistamientosApplication {

	public static void main(String[] args) {
		SpringApplication.run(AvistamientosApplication.class, args);
	}

}
