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

| Campo | Tipo | Descripción |
|---|---|---|
| id | número | Lo asigna el sistema |
| especie | texto | Nombre de la especie (obligatorio) |
| lugar | texto | Dónde se observó (obligatorio) |
| fecha | texto | Formato AAAA-MM-DD (obligatorio) |
| observador | texto | Quién lo registró (obligatorio) |

## Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| GET | /avistamientos | 200 con la lista |
| GET | /avistamientos/{id} | 200 con el avistamiento, o 404 |
| GET | /avistamientos/resumen | 200 con cuántos avistamientos hay por especie |
| POST | /avistamientos | 201 con el creado, o 400 si faltan datos |
| PUT | /avistamientos/{id} | 200 con el actualizado, 404 si no existe, o 400 si faltan datos |
| DELETE | /avistamientos/{id} | 204, o 404 si no existe |

## Ejemplos con curl

> Los ejemplos usan comillas simples, así que funcionan en Git Bash, Linux o Mac.

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
## Uso de IA

Usé Claude (modelo Opus 5.5, de Anthropic) como apoyo para entender los conceptos y guiarme paso a paso en la construcción del proyecto.