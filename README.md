# API REST - Registro de avistamientos de aves

Taller de Ingeniería de Software 2 (Sesión 17). API REST para registrar avistamientos de aves, hecha con Spring Boot y base de datos H2 embebida.

## Requisitos

- Java 21 o superior (`java -version` para verificar)
- Conexión a internet la primera vez que se ejecuta (para descargar las librerías)
- No hace falta instalar Maven ni ninguna base de datos: el proyecto trae el Maven Wrapper y H2 se crea sola en la carpeta `data/`.

## Cómo ejecutarla

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/JuaniMP/Api-avistamientos.git
   cd Api-avistamientos
   ```
2. Levantar la API. El comando cambia según la terminal:

   | Terminal | Comando |
   |---|---|
   | CMD (Windows) | `mvnw.cmd spring-boot:run` |
   | PowerShell (Windows) | `.\mvnw.cmd spring-boot:run` |
   | Git Bash, Linux o Mac | `./mvnw spring-boot:run` |

La primera vez tarda un poco porque descarga Maven y las librerías. Cuando en la consola salga `Started AvistamientosApplication`, la API queda corriendo en `http://localhost:8080`.

Para detenerla: `Ctrl + C`.

## Estructura del proyecto

```
src/main/java/co/edu/bosque/avistamientos/
├── AvistamientosApplication.java           Punto de entrada: arranca la aplicación
├── entity/Avistamiento.java                El modelo: campos, reglas de validación y tabla en la base
├── repository/AvistamientoRepository.java  Habla con la base de datos (guardar, buscar, borrar)
├── controller/AvistamientoController.java  Los endpoints: recibe las peticiones HTTP y responde en JSON
└── exception/ManejadorErrores.java         Convierte los errores de validación en respuestas 400 con mensajes claros

src/main/resources/application.properties   Configuración de la base H2 (guardada en archivo)
src/test/java/...                           Pruebas de integración de todos los endpoints
src/test/resources/application.properties   Base H2 en memoria, solo para las pruebas
docs/openapi.yaml                           Contrato OpenAPI de la API (copia del que genera Swagger)
docs/avistamientos.postman_collection.json  Colección de Postman con todas las peticiones
```

## Modelo

Cada avistamiento tiene:

| Campo | Tipo | Reglas |
|---|---|---|
| id | número | Lo asigna el sistema (si se envía en el POST, se ignora) |
| especie | texto | Obligatorio. Solo letras, espacios, guiones o apóstrofes, y debe empezar con letra. Máximo 100 caracteres |
| lugar | texto | Obligatorio. Máximo 150 caracteres |
| fecha | fecha | Obligatoria. Formato `AAAA-MM-DD`, debe ser una fecha real y no puede ser futura |
| observador | texto | Obligatorio. Solo letras, espacios, puntos, guiones o apóstrofes. Máximo 100 caracteres |

## Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| GET | /avistamientos | 200 con la lista |
| GET | /avistamientos/{id} | 200 con el avistamiento, o 404 si no existe |
| GET | /avistamientos/resumen | 200 con cuántos avistamientos hay por especie |
| POST | /avistamientos | 201 con el creado, o 400 si los datos no son válidos |
| PUT | /avistamientos/{id} | 200 con el actualizado, 404 si no existe, o 400 si los datos no son válidos |
| DELETE | /avistamientos/{id} | 204, o 404 si no existe |

## Documentación con Swagger (OpenAPI)

La API tiene su contrato OpenAPI, generado automáticamente a partir del código con [springdoc-openapi](https://springdoc.org/). Con la API corriendo:

| Qué | Dirección |
|---|---|
| **Swagger UI**: página para ver y probar todos los endpoints desde el navegador | `http://localhost:8080/swagger-ui.html` |
| Contrato OpenAPI en JSON | `http://localhost:8080/v3/api-docs` |
| Contrato OpenAPI en YAML | `http://localhost:8080/v3/api-docs.yaml` |

En Swagger UI cada endpoint muestra qué recibe, qué responde y con qué códigos (200, 201, 204, 400, 404). Para probar uno: abrirlo → **Try it out** → **Execute**. Los campos ya vienen con datos de ejemplo válidos.

Además hay una copia del contrato en `docs/openapi.yaml`, para poder leerlo sin levantar la API (por ejemplo, pegándolo en [editor.swagger.io](https://editor.swagger.io/)).

## Ejemplos con curl

> Los ejemplos usan comillas simples, así que funcionan en Git Bash, Linux o Mac. En Windows lo más fácil es correrlos desde Git Bash (también se pueden probar con la colección de Postman, ver más abajo). El `-i` muestra el código de estado de la respuesta.

**Listar todos**
```bash
curl -i http://localhost:8080/avistamientos
```

**Ver uno**
```bash
curl -i http://localhost:8080/avistamientos/1
```

**Resumen por especie**
```bash
curl -i http://localhost:8080/avistamientos/resumen
```
Respuesta de ejemplo:
```json
{"colibrí": 2, "tucán": 1}
```

**Registrar uno**
```bash
curl -i -X POST http://localhost:8080/avistamientos \
  -H "Content-Type: application/json" \
  -d '{"especie":"colibri","lugar":"Humedal La Conejera","fecha":"2026-09-29","observador":"Juanita"}'
```

**Actualizar uno**
```bash
curl -i -X PUT http://localhost:8080/avistamientos/1 \
  -H "Content-Type: application/json" \
  -d '{"especie":"tucan","lugar":"Parque Nacional Chingaza","fecha":"2026-09-28","observador":"Juanita"}'
```

**Eliminar uno**
```bash
curl -i -X DELETE http://localhost:8080/avistamientos/1
```

## Errores

Cuando los datos no son válidos, la API responde **400** con un JSON que dice qué campo falló y por qué.

**Datos que no cumplen las reglas** (por ejemplo, una especie con símbolos y una fecha futura):
```bash
curl -i -X POST http://localhost:8080/avistamientos \
  -H "Content-Type: application/json" \
  -d '{"especie":"------","lugar":"Humedal La Conejera","fecha":"2030-01-01","observador":"Juanita"}'
```
Respuesta:
```json
{
  "especie": "la especie solo puede tener letras, espacios, guiones o apóstrofes, y debe empezar con letra",
  "fecha": "la fecha no puede ser futura"
}
```

**JSON mal escrito o fecha que no se puede leer** (por ejemplo, `"fecha": "ayer"` o `"2026-13-45"`):
```json
{"error": "El JSON no es válido o algún campo tiene un formato incorrecto (la fecha debe ser AAAA-MM-DD)"}
```

Si el `id` no existe (en GET, PUT o DELETE), la respuesta es **404** sin cuerpo.

## Colección de Postman

En la carpeta `docs/` está el archivo `avistamientos.postman_collection.json`, con todas las peticiones listas para probar la API desde Postman sin escribir nada.

**Cómo abrirla:**
1. Levantar la API (ver "Cómo ejecutarla").
2. En Postman: botón **Import** → arrastrar el archivo `docs/avistamientos.postman_collection.json`.
3. Aparece la colección **API Avistamientos** en el panel de la izquierda.

**Qué tiene:** 10 peticiones numeradas, cada una con el código que debe responder entre paréntesis:

| # | Petición | Respuesta esperada |
|---|---|---|
| 1 | Registrar avistamiento | 201 |
| 2 | Listar avistamientos | 200 |
| 3 | Ver avistamiento 1 | 200 |
| 4 | Ver avistamiento inexistente (id 99) | 404 |
| 5 | Actualizar avistamiento 1 | 200 |
| 6 | Resumen por especie | 200 |
| 7 | Registrar con especie inválida (`------`) | 400 |
| 8 | Registrar con fecha que no existe (`2026-13-45`) | 400 |
| 9 | Eliminar avistamiento 1 | 204 |
| 10 | Eliminar avistamiento ya borrado | 404 |

Con la base vacía (sin carpeta `data/`), se pueden correr en orden de la 1 a la 10, una por una o todas juntas con **Run collection**. La dirección de la API está en la variable `baseUrl` de la colección (`http://localhost:8080`), por si se levanta en otro puerto.

## Decisiones de diseño

- **H2 guardada en archivo (`data/`)**: los datos no se pierden al reiniciar la API, y no hay que instalar ni configurar ningún motor de base de datos. La tabla la crea Spring sola a partir de la clase `Avistamiento` (`ddl-auto=update`).
- **El `id` lo pone la base**: en el POST se ignora cualquier `id` que venga en el body, para que nadie pueda sobrescribir un avistamiento existente al "crear" uno nuevo.
- **PUT reemplaza el avistamiento completo**: por eso valida todos los campos y responde 400 si falta alguno.
- **DELETE responde 204**: la operación salió bien, pero no hay nada que devolver en el cuerpo.
- **La URL indica el recurso y el método HTTP indica la acción**: todas las rutas usan `/avistamientos` (en plural) y lo que cambia es GET, POST, PUT o DELETE.
- **La fecha es `LocalDate` y no texto**: así Java comprueba que sea una fecha real (rechaza `2026-13-45`) y se puede validar que no sea futura. En el JSON se sigue viendo como `"2026-09-29"`.
- **Las validaciones van en la entidad, no en el controller**: las reglas quedan en un solo lugar y sirven igual para POST y PUT. El `@Valid` del controller es el que las activa.
- **Manejo de errores en una clase aparte** (`ManejadorErrores`, con `@RestControllerAdvice`): los errores también se responden en JSON y dicen qué corregir, sin llenar el controller de `try/catch`.
- **El contrato OpenAPI se genera desde el código (code-first)**: como la API ya estaba hecha, springdoc lee el controller, la entidad y sus validaciones y arma el contrato solo. Las anotaciones `@Operation`, `@ApiResponse` y `@Schema` solo lo documentan (resúmenes, códigos posibles y ejemplos); no cambian cómo funciona la API.
- **El resumen usa un `TreeMap`**: cuenta los avistamientos por especie y los devuelve ordenados alfabéticamente.

## Pruebas

El proyecto trae 13 pruebas de integración (en `src/test/java`) que llaman a cada endpoint y revisan que responda el código correcto (200, 201, 204, 400 y 404), incluyendo las validaciones y el resumen. Además está la prueba que trae Spring por defecto, que revisa que la aplicación arranque. Usan una base H2 en memoria, así que no tocan los datos de `data/`.

Para correrlas:

| Terminal | Comando |
|---|---|
| CMD (Windows) | `mvnw.cmd test` |
| PowerShell (Windows) | `.\mvnw.cmd test` |
| Git Bash, Linux o Mac | `./mvnw test` |

## Verificación en un computador limpio

Para comprobar que la API funciona siguiendo solo este README, se probó en **GitHub Codespaces** (un Linux nuevo, sin nada del proyecto instalado, con Java 25): se clonó el repositorio, se levantó con `./mvnw spring-boot:run` y se probaron los endpoints con curl.

Resultado de los curl:
```
GET  /avistamientos           ->  HTTP/1.1 200   []
POST /avistamientos           ->  HTTP/1.1 201   {"especie":"colibri","fecha":"2026-09-29","id":1,"lugar":"Humedal La Conejera","observador":"Juanita"}
GET  /avistamientos/resumen   ->  HTTP/1.1 200   {"colibri":1}
```

Resultado de `./mvnw test`:
```
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

También se probó en Windows con Java 26, desde IntelliJ IDEA y Postman.

## Problemas comunes

- **`Port 8080 was already in use`**: ya hay otra instancia de la API (u otro programa) usando el puerto. Detener la otra instancia, o levantar esta en otro puerto: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
- **`java: command not found` o error de `JAVA_HOME`**: falta instalar Java 21 o superior, o no está en el PATH. Verificar con `java -version`.
- **`./mvnw: Permission denied`** (Linux o Mac): darle permiso de ejecución con `chmod +x mvnw`.
- **Se quieren borrar todos los datos**: detener la API y borrar la carpeta `data/`. Se vuelve a crear vacía al levantar la API.

## Uso de IA

Usé Claude (modelo Opus 5.5, de Anthropic) como apoyo para entender los conceptos y guiarme paso a paso en la construcción del proyecto.
