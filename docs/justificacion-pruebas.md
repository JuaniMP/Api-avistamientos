# Justificación de las pruebas: API de avistamientos

Ingeniería de Software 2, Sesión 22 (21 de octubre de 2026). Juanita Mejía Pineda.

Quedaron 47 pruebas: 30 unitarias (23 del validador y 7 del servicio) y 17 de integración. Los comandos para correrlas y sacar la cobertura están en el README.

## 1. Qué cambió en el código

Antes las validaciones eran anotaciones en la entidad y las activaba el `@Valid` del controller, entonces la regla solo existía con Spring corriendo, y `@PastOrPresent` usaba el reloj del sistema por dentro. Cambié cuatro cosas:

- **`ValidadorDeAvistamiento`:** la regla en su propia clase. Recibe los datos y devuelve un mapa campo → mensaje, sin saber nada de HTTP ni de la base. Le quité las anotaciones a la entidad para que la regla esté en un solo lugar.
- **Interfaz `Reloj`:** en producción se inyecta `RelojDelSistema` (`LocalDate.now()`) y en las pruebas un `RelojFijo`. Es la táctica de limitar el no determinismo, y funciona por inversión de dependencias.
- **`AvistamientoService`:** el controller solo se encarga de HTTP y el servicio valida y guarda. Recibe el repositorio por el constructor, así que en las pruebas le puedo pasar un doble.
- **La fecha llega como texto (`AvistamientoRequest`):** si llegara como `LocalDate`, Jackson rechazaría `"2026-13-45"` antes de validar y el error no diría qué campo falló.

Así la API ganó controlabilidad (fijo la entrada, el día y lo que responde el repositorio) y observabilidad (el validador dice qué campo falló).

## 2. Tabla de casos (Parte A)

Usé partición de equivalencia (dividir la entrada en clases que el sistema trata igual y probar un valor de cada una) y valores límite (probar justo en cada borde, a los dos lados, porque ahí se cuela un `<` en vez de `<=`). En las pruebas "hoy" es el 21/10/2026, lo que dice el `RelojFijo`.

**Especie** (3 a 50 caracteres). Clases: vacía, muy corta, válida, muy larga y con caracteres no permitidos.

| Valor | Tipo de caso | Resultado esperado | Por qué ese valor |
|---|---|---|---|
| `""` | Inválido | Error "obligatoria" | La clase de "no hay dato" |
| `"ab"` (2) | Límite inválido | Error "entre 3 y 50" | Si alguien pone `< 2` en vez de `< 3`, solo este caso lo detecta |
| `"abc"` (3) | Límite válido | OK | El otro lado del mismo borde |
| `"colibri"` | Representativo | OK | Un valor del medio de la clase válida |
| 50 letras | Límite válido | OK | El último válido |
| 51 letras | Límite inválido | Error "entre 3 y 50" | El primero de la clase muy larga |
| `"   ab   "` | Límite inválido | Error "entre 3 y 50" | Solo 2 letras: los espacios de los lados no cuentan |
| `"------"` | Inválido | Error "solo letras…" | Largo válido pero no son letras: es otra clase |

**Fecha** (AAAA-MM-DD y no posterior a hoy). Clases: sin fecha, formato malo, formato bien pero inexistente, válida (pasada o hoy) y futura.

| Valor | Tipo de caso | Resultado esperado | Por qué ese valor |
|---|---|---|---|
| `2026-09-29` | Representativo | OK | Una fecha del pasado |
| `2026-10-21` (hoy) | Límite válido | OK | "No posterior a hoy" incluye hoy. Si alguien usa `!isBefore` en vez de `isAfter`, solo este caso falla |
| `2026-10-22` (mañana) | Límite inválido | Error "posterior a hoy" | El primer día de la clase futura |
| `21/10/2026` | Inválido | Error "formato AAAA-MM-DD" | Como escribimos la fecha en Colombia, el error más probable |
| `2026-02-30` | Inválido | Error "no existe" | Un parser permisivo no lo rechaza, lo cambia por otra fecha sin avisar. El `2026-13-45` quedó en integración |
| `null` | Inválido | Error "obligatoria" | El campo no viene en el JSON |

**Lugar y observador.** La regla solo pide que no estén vacíos, pero el taller deja usar validaciones propias y yo ya tenía largos máximos (150 y 100) y un formato para el observador:

| Campo | Valor | Tipo de caso | Resultado esperado |
|---|---|---|---|
| Lugar | `"   "` | Inválido | Error "obligatorio" (`isBlank()` lo detecta, `isEmpty()` no) |
| Lugar | 150 / 151 letras | Límite válido / inválido | OK / Error "más de 150" |
| Observador | `""` | Inválido | Error "obligatorio" |
| Observador | 100 / 101 letras | Límite válido / inválido | OK / Error "más de 100" |
| Observador | `"María-José O'Neil Jr."` | Representativo | OK (usa todos los caracteres especiales permitidos) |
| Observador | `"Juanita123"` | Inválido | Error "solo letras…" |

Además, una prueba con los cuatro campos mal revisa que salgan todos los errores y no solo el primero.

## 3. Por qué cada nivel

**Los límites del validador son unitarios** porque la regla es lógica pura: entra un `AvistamientoRequest` y sale un mapa. Por HTTP serían más lentos y, si fallaran, no sabría si el problema es la regla, el JSON o el controller. Así cumplen FIRST: rápidas, independientes y repetibles.

**El servicio también es unitario, con un doble del repositorio.** Reviso decisiones del servicio (si los datos están mal no guarda, si el id no existe no borra, el PUT guarda con el mismo id) y nada de eso necesita una base real.

**El POST tiene integración aunque el validador ya esté probado**, porque la unitaria no ve las fronteras entre componentes: que el JSON se mapee bien, que el controller sí llame al validador, que `ManejadorErrores` convierta el error en un 400 y que el dato quede guardado de verdad (después del POST hago un GET al id nuevo). Lo comprobé borrando ese manejador: las 30 unitarias siguieron pasando y fallaron 5 de integración.

Por eso las de integración revisan el cuerpo y el efecto: después del PUT, que esté el dato nuevo; después de un PUT inválido, que siga el viejo; después del DELETE, que el GET dé 404. Hay éxito y error en cada endpoint donde aplica, y el `@BeforeEach` vacía la H2 en memoria para que no dependan del orden.

Para revisar que no pasaran "por pasar", metí a propósito ocho defectos en el validador (por ejemplo mínimo 2 en vez de 3, `!isBefore` en vez de `isAfter`, quitar el `trim()`) y cada uno lo detectó la prueba de su límite o de su clase.

## 4. Los dobles

| Doble | Tipo | Por qué ese tipo |
|---|---|---|
| `RelojFijo` | Stub | Solo devuelve una fecha preparada (21/10/2026). No reviso cómo lo llaman, por eso no es mock |
| `AvistamientoRepository` con Mockito | Stub y mock | Con `when(...).thenReturn(...)` decido qué responde (stub). Con `verify(repository, never()).save(...)` reviso que no se guardó (mock) |

**Sin el `RelojFijo`**, la prueba de "mañana" usaría `LocalDate.now()` o una fecha escrita a mano. La escrita a mano falla al otro día, porque el 22 se vuelve "hoy", y con `now()` el resultado depende de la hora (cerca de medianoche puede caer en otro día). La prueba dejaría de ser repetible. En integración también lo puse, con `@TestConfiguration` y `@Primary`, para probar hoy y mañana por HTTP con fecha fija.

**Mock y no fake:** un repositorio en memoria también servía, pero lo que reviso es lo que el servicio hace o no hace con el repositorio, y eso con el mock se escribe directo. Sin doble, el servicio necesitaría una base y la prueba ya sería de integración. No usé dummy porque ningún método recibe parámetros que no se usen, ni spy porque el `verify` ya me da eso. En integración no reemplacé la base porque ahí lo que quiero probar es justo la colaboración con JPA.

## 5. Lo que no probé

- **Getters y setters:** no tienen lógica, probarlos solo subiría la cobertura. Igual se ejecutan en las de integración.
- **`RelojDelSistema.hoy()`:** es `return LocalDate.now()`. Probarla sería probar Java y meter otra vez el no determinismo.
- **`main` de la aplicación:** solo arranca Spring, y eso ya lo revisa `contextLoads`.
- **Los métodos del repositorio:** la interfaz la creé yo, pero está vacía (extiende `JpaRepository`). El código de `save` o `findById` lo genera Spring Data, entonces probarlos sería probar Spring. Igual se usan en integración con una base real.
- **Los límites del lugar y del observador por HTTP:** ya están en las unitarias, y la integración ya muestra que cualquier error del validador sale como 400 con su campo. Repetirlos no detectaría nada nuevo.

## 6. Lectura de la cobertura

`mvnw.cmd verify` con JaCoCo: 47 pruebas, 0 fallas. El criterio de cobertura más exigente es el de ramas (que cada `if` se pruebe por el lado verdadero y por el falso), así que miré ramas además de líneas:

| Clase | Líneas | Ramas |
|---|---|---|
| `ValidadorDeAvistamiento` | 47/49 | 29/30 |
| Servicio, controller, manejador de errores, entidad y DTO | todas | todas |
| `RelojDelSistema` y `AvistamientosApplication` | 2/5 | — |
| **Total** | **114/119 (95,8 %)** | **39/40 (97,5 %)** |

**Para qué me sirvió.** La primera vez, con 41 pruebas, el validador iba en 26 de 30 ramas, y las que faltaban eran las de mis validaciones propias. Ahí vi que si alguien dañaba la expresión regular del observador nadie se iba a dar cuenta, y por eso agregué esos 6 casos.

**Lo que sigue sin cubrir:** la rama `datos == null` del validador, a la que por HTTP no se llega porque Spring responde 400 antes si falta el body (una prueba ahí no protege nada que el usuario vea); `RelojDelSistema.hoy()`, y que nunca se ejecute confirma que ninguna prueba depende del reloj real; y el `main`, que `@SpringBootTest` no usa.

**Conclusión.** JaCoCo cuenta líneas ejecutadas, no verificadas: una prueba sin `assert` también sube el porcentaje. Lo que muestra que las pruebas sirven son los `assert` y el experimento de la sección 3; lo útil del reporte fue ver dónde no había pruebas y decidir si eso era un riesgo.

**Captura del reporte** (`target/site/jacoco/index.html`). JaCoCo redondea hacia abajo: su 96 % y 97 % son el 96,9 % de instrucciones y el 97,5 % de ramas.

<img width="1917" height="550" alt="image" src="https://github.com/user-attachments/assets/7ec9a7e3-fd25-452a-a15e-cb432b23082b" />
