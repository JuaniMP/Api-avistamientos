# API REST - Registro de avistamientos de aves

Taller de Ingeniería de Software 2 (Sesión 17). API REST para registrar avistamientos de aves, hecha con Spring Boot y base de datos H2 embebida.

## Requisitos

- Java 21 o superior (`java -version` para verificar)
- No hace falta instalar Maven ni ninguna base de datos: el proyecto trae el Maven Wrapper y H2 se crea sola en la carpeta `data/`.

## Cómo ejecutarla

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/JuaniMP/Api-avistamientos.git
   cd Api-avistamientos
   ```
2. Levantar la API:
   - Windows: `mvnw.cmd spring-boot:run`
   - Linux / Mac: `./mvnw spring-boot:run`

La API queda corriendo en `http://localhost:8080`.

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

## Ejemplos con curl

> Los ejemplos usan comillas simples, así que funcionan en Git Bash, Linux o Mac. El `-i` muestra el código de estado de la respuesta.

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

## Uso de IA

Usé Claude (modelo Opus 5.5, de Anthropic) como apoyo para entender los conceptos y guiarme paso a paso en la construcción del proyecto.
