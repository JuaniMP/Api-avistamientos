# Justificación de las pruebas: API de avistamientos

Taller de pruebas unitarias y de integración. Ingeniería de Software 2, Sesión 22 (21 de octubre de 2026).
Juanita Mejía Pineda.

El repositorio tiene 47 pruebas: 30 unitarias (23 del validador y 7 del servicio) y 17 de integración (16 de los endpoints y la que revisa que la aplicación arranque). Los comandos para correrlas y para la cobertura están en el README.

## 1. Qué cambió en el código

La API del 29 de septiembre tenía las validaciones como anotaciones en la entidad (`@NotBlank`, `@Size`, `@PastOrPresent`) y las activaba el `@Valid` del controlador. Funcionaba, pero la regla no se podía probar sola: solo existía si Spring estaba corriendo. Además, `@PastOrPresent` lee el reloj del sistema por dentro, así que no había forma de controlar qué día es "hoy" en una prueba. Cambié cuatro cosas:

- **La regla pasó a su propia clase, `ValidadorDeAvistamiento`.** Recibe los datos y devuelve un mapa campo → mensaje. No sabe nada de HTTP ni de la base de datos, entonces se crea con `new` y se prueba en milisegundos. Quité las anotaciones de la entidad para que la regla esté en un solo lugar: si las dejaba, el `@Valid` respondía 400 antes de que el validador se ejecutara y las pruebas de integración nunca pasarían por él.
- **El "hoy" lo da una interfaz `Reloj`.** En producción se inyecta `RelojDelSistema` (`LocalDate.now()`) y en las pruebas un `RelojFijo`. Esto es la táctica de testabilidad de limitar el no determinismo, y funciona por inversión de dependencias: el validador depende de la abstracción y no de la clase concreta.
- **Agregué una capa de servicio, `AvistamientoService`.** Antes el controlador hablaba directo con el repositorio. Ahora el controlador solo traduce HTTP y el servicio valida, convierte y guarda. El servicio recibe el repositorio por el constructor, así que en una prueba unitaria le puedo pasar un doble.
- **La fecha llega como texto (`AvistamientoRequest`).** Si llega como `LocalDate`, Jackson rechaza `"2026-13-45"` antes de validar y el error no dice qué campo falló, que es justo lo que pide la regla. Ahora el validador revisa el formato y responde `{"fecha": "la fecha no existe en el calendario"}`.

Con esto la API ganó **controlabilidad**: puedo fijar la entrada, el día y lo que responde el repositorio. También ganó **observabilidad**: el validador devuelve exactamente qué campo falló, en vez de lanzar una excepción de Spring que tendría que interpretar.

## 2. Tabla de casos (Parte A)

Apliqué **partición de equivalencia**: dividir la entrada en clases que el sistema trata igual y probar un valor de cada una. También apliqué **valores límite**: probar justo en el borde de cada clase y a ambos lados, porque ahí es donde se cuela un `<` en vez de un `<=`. En las pruebas, "hoy" es el 21 de octubre de 2026 (lo fija el `RelojFijo`).

**Especie** (regla: entre 3 y 50 caracteres). Las clases son: vacía, muy corta (1 a 2), válida (3 a 50), muy larga (51 o más) y con caracteres no permitidos.

| Valor | Tipo de caso | Resultado esperado | Por qué ese valor |
|---|---|---|---|
| `""` | Inválido (clase vacía) | Error: "la especie es obligatoria" | Es la clase "no hay dato", que tiene su propio mensaje |
| `"ab"` (2) | Límite inválido | Error: "entre 3 y 50 caracteres" | El último valor de la clase "muy corta". Si alguien escribe `< 2` en vez de `< 3`, solo este caso lo detecta |
| `"abc"` (3) | Límite válido | Sin errores | El primer valor válido: es el otro lado del mismo borde |
| `"colibri"` (7) | Representativo válido | Sin errores | Un valor del medio de la clase válida (está en la prueba de "todo válido") |
| 50 letras | Límite válido | Sin errores | El último valor válido |
| 51 letras | Límite inválido | Error: "entre 3 y 50 caracteres" | El primer valor de la clase "muy larga" |
| `"   ab   "` | Límite inválido | Error: "entre 3 y 50 caracteres" | Tiene 8 caracteres pero solo 2 letras. Comprueba que los espacios de los lados no cuentan; sin el `trim()` pasaría como válida |
| `"------"` | Inválido (formato) | Error: "solo puede tener letras…" | Tiene una longitud válida (6) pero caracteres que no son letras. Es otra clase, aunque la longitud esté bien |

**Fecha** (regla: formato AAAA-MM-DD y no posterior a hoy). Las clases son: sin fecha, formato incorrecto, formato correcto pero inexistente, pasada o de hoy (válida) y futura.

| Valor | Tipo de caso | Resultado esperado | Por qué ese valor |
|---|---|---|---|
| `"2026-09-29"` | Representativo válido | Sin errores | Una fecha cualquiera del pasado |
| `"2026-10-21"` (hoy) | Límite válido | Sin errores | La regla dice "no posterior a hoy", entonces hoy sí vale. Si alguien usa `!isBefore` en vez de `isAfter`, solo este caso falla |
| `"2026-10-22"` (mañana) | Límite inválido | Error: "no puede ser posterior a hoy" | El primer día de la clase futura |
| `"21/10/2026"` | Inválido (formato) | Error: "debe tener el formato AAAA-MM-DD" | Es una fecha real, pero en el formato que usamos en Colombia, que es justo el error más probable de un usuario |
| `"2026-02-30"` | Inválido (no existe) | Error: "no existe en el calendario" | Tiene el formato correcto pero el día no existe (2026 no es bisiesto). Lo elegí en vez de `2026-13-45` porque un parser permisivo no lo rechaza: lo convierte en silencio en otra fecha (el 28 de febrero o el 2 de marzo, según el modo). Un mes 13, en cambio, lo rechaza cualquiera. El `2026-13-45` quedó en la prueba de integración |
| `null` | Inválido (clase vacía) | Error: "la fecha es obligatoria" | Para el caso en que el campo no viene en el JSON |

**Lugar y observador.** La regla del taller solo pide que no estén vacíos, pero el taller también dice que si uno ya tiene validaciones propias las puede usar. Yo ya tenía de septiembre un largo máximo para los dos (150 y 100) y un formato para el observador, así que también los particioné y probé sus límites:

| Campo | Valor | Tipo de caso | Resultado esperado | Por qué ese valor |
|---|---|---|---|---|
| Lugar | `"   "` | Inválido (vacío) | Error: "el lugar es obligatorio" | Solo espacios: `isEmpty()` diría que no está vacío, pero `isBlank()` sí lo detecta. Es el caso vacío más engañoso |
| Lugar | `"Humedal La Conejera"` | Representativo válido | Sin errores | Un lugar normal (está en la prueba de "todo válido") |
| Lugar | 150 letras | Límite válido | Sin errores | El último valor válido |
| Lugar | 151 letras | Límite inválido | Error: "no puede tener más de 150 caracteres" | El primer valor de la clase "muy largo" |
| Observador | `""` | Inválido (vacío) | Error: "el observador es obligatorio" | La clase "no hay dato" |
| Observador | `"Juanita"` | Representativo válido | Sin errores | Un nombre normal (está en la prueba de "todo válido") |
| Observador | `"María-José O'Neil Jr."` | Representativo válido | Sin errores | Usa todos los caracteres especiales permitidos (tilde, guion, apóstrofe, punto). Si alguien "simplifica" la expresión regular, este caso lo detecta |
| Observador | 100 letras | Límite válido | Sin errores | El último valor válido |
| Observador | 101 letras | Límite inválido | Error: "no puede tener más de 100 caracteres" | El primer valor de la clase "muy largo" |
| Observador | `"Juanita123"` | Inválido (formato) | Error: "solo puede tener letras…" | Largo válido, pero con números. Es otra clase, igual que `"------"` en la especie |

Por último, una prueba con los cuatro campos mal a la vez revisa que el validador reporte **todos** los errores y no se quede en el primero.

En total son 23 pruebas unitarias del validador. Incluyen los seis límites que pide el taller (especie de 2, 3, 50 y 51; fecha de hoy y de mañana) y los de mis validaciones propias.

## 3. Por qué cada nivel

**Los límites del validador son unitarios.** La regla es lógica pura: entra un `AvistamientoRequest` y sale un mapa. No hay base de datos ni HTTP de por medio, así que probarla a través de la API sería más lento y, cuando fallara, no sabría si el problema es la regla, el JSON o el controlador. Como unitarias son rápidas e independientes, y cada una verifica un solo comportamiento, que es lo que pide FIRST.

**El servicio también se prueba con unitarias, con un doble del repositorio.** Lo que quiero comprobar ahí son decisiones del servicio: que si los datos son inválidos **no guarde nada**, que si el id no existe no borre nada, que el PUT guarde con el mismo id y que limpie los espacios y convierta la fecha a `LocalDate` antes de guardar. Nada de eso necesita una base real.

**El POST tiene pruebas de integración aunque el validador ya esté probado.** La prueba unitaria del validador no puede detectar fallas en las **fronteras** entre componentes:

- que el JSON se mapee bien al `AvistamientoRequest` (si un campo se llamara distinto, llegaría `null`);
- que el controlador realmente llame al servicio y el servicio al validador;
- que `ManejadorErrores` convierta la `DatosInvalidosException` en un **400** con el JSON campo → mensaje. Lo comprobé: borré ese método y las 30 pruebas unitarias siguieron en verde, mientras que 5 de integración fallaron, porque la excepción se escapaba y la API habría respondido 500;
- que un JSON mal escrito también dé 400;
- que el avistamiento quede **guardado de verdad**: después del POST hago un GET al id nuevo y debe devolverlo.

Por eso las pruebas de integración revisan el cuerpo y el efecto, no solo el código: después del PUT, que en la base esté el dato nuevo; después de un PUT inválido, que siga el dato viejo; después del DELETE, que el GET responda 404. Hay al menos un caso de éxito y uno de error por endpoint. Cada prueba prepara sus propios datos y el `@BeforeEach` vacía la base H2 en memoria, así que no dependen del orden.

Algunos casos están en los dos niveles, como la fecha de mañana, la de hoy y la especie de 2. No es repetir lo mismo: la unitaria prueba la regla y la de integración prueba que esa regla está **conectada** y que el reloj inyectado en el contexto de Spring es el que se usa.

**¿Las pruebas detectan defectos de verdad?** Una prueba que pasa no demuestra que el código esté bien (primer principio). Para revisar que mis pruebas no pasaran "por pasar", metí a propósito ocho defectos típicos en el validador, uno a la vez, y corrí la suite:

| Defecto introducido | Pruebas que fallaron |
|---|---|
| Mínimo de la especie en 2 en vez de 3 | 7, entre ellas `especieDe2Caracteres` (unitaria y de integración) |
| Máximo de la especie en 51 en vez de 50 | 4, entre ellas `especieDe51Caracteres` |
| `!isBefore(hoy)` en vez de `isAfter(hoy)` (rechaza también hoy) | 2: `fechaDeHoy` unitaria y de integración |
| Quitar el `trim()` de la especie | 2: `especieDe2LetrasRodeadaDeEspacios` y la del servicio que revisa los datos limpios |
| Máximo del lugar en 151 en vez de 150 | 1: `lugarDe151Caracteres` |
| Máximo del observador en 99 en vez de 100 | 2: `observadorDe100Caracteres` y `observadorDe101Caracteres` |
| Expresión regular del observador que acepta cualquier cosa | 1: `observadorConNumeros` |
| Expresión regular del observador sin punto, guion ni apóstrofe | 1: `observadorConPuntoGuionYApostrofe` |

Cada defecto lo detectó justo la prueba de su valor límite o de su clase. Es lo mismo que el ejemplo de clase del envío gratis: solo el valor que está en el borde detecta el `>` en vez de `>=`.

## 4. Los dobles

| Doble | Tipo | Dónde | Por qué ese tipo |
|---|---|---|---|
| `RelojFijo` | **Stub** | Pruebas del validador, del servicio y de integración | Solo necesito que responda una fecha preparada de antemano (21/10/2026). No verifico cómo lo llaman, entonces es un stub y no un mock |
| `AvistamientoRepository` con Mockito | **Stub** y **mock** | `AvistamientoServiceTest` | Con `when(...).thenReturn(...)` lo uso como stub (qué responde `existsById` o `findAll`). Con `verify(repository, never()).save(...)` lo uso como mock, porque lo que importa es una interacción: que **no** se llamó a guardar |

**¿Qué habría pasado sin el `RelojFijo`?** La prueba de "fecha de mañana" tendría que usar `LocalDate.now().plusDays(1)`, o una fecha escrita a mano como `2026-10-22`. La escrita a mano pasa hoy y falla mañana, porque el 22 se vuelve "hoy" y ya es válida. Con `now()`, el resultado depende de la hora a la que se corre: cerca de la medianoche, `now()` en la prueba y `now()` en el validador pueden caer en días distintos. En los dos casos la prueba deja de ser **repetible** (la R de FIRST). En integración reemplacé el reloj real por el `RelojFijo` con un `@TestConfiguration` y `@Primary`, para que la frontera "hoy / mañana" también se pruebe por HTTP con una fecha fija.

**¿Por qué un mock y no un fake?** Un fake (un repositorio en memoria con un `HashMap`) también servía. Lo descarté porque esas pruebas son sobre lo que el servicio **hace o no hace** con el repositorio, y con el mock eso se dice directo (`never().save`). Con un fake tendría que implementar los métodos de `JpaRepository` que uso y después revisar el estado. Sin ningún doble, el servicio necesitaría una base de datos y la prueba ya sería de integración: más lenta y sin aislar.

No usé **dummy** porque ningún método recibe un parámetro que no se use. Tampoco usé **spy**, porque el `verify` de Mockito ya me da lo que daría un spy (revisar las llamadas después). En las pruebas de integración **no** reemplacé la base por un doble: lo que quiero probar ahí es justo la colaboración real con JPA, y H2 en memoria es rápida y se vacía antes de cada prueba. La API no habla con ningún servicio externo, así que no hizo falta un servidor simulado.

## 5. Lo que no probé

- **Getters y setters de la entidad y el record.** No tienen lógica. Probarlos sería probar Java y llenaría la cobertura sin detectar nada. Igual se ejecutan indirectamente en todas las pruebas de integración.
- **`RelojDelSistema.hoy()`.** Es una línea: `return LocalDate.now()`. Para probarla tendría que compararla contra `LocalDate.now()`, que es probar la librería y además mete otra vez el no determinismo que quise sacar.
- **`AvistamientosApplication.main`.** Solo arranca Spring. Que la aplicación arranca ya lo verifica `contextLoads` y, de hecho, todas las pruebas de integración.
- **Los métodos del repositorio (`save`, `findById`, …).** Son de Spring Data JPA, no míos. Las pruebas de integración los usan con una base real, y eso me basta.
- **Los límites del lugar y el observador por HTTP.** Están probados en las unitarias del validador, pero no repetí cada uno en las de integración. Ya hay pruebas de integración que demuestran que cualquier error del validador sale como 400 con su campo (especie, fecha y la de "incompleto", que cubre lugar y observador). Repetir cada límite por HTTP sería más lento y no detectaría nada nuevo.
- **La generación del contrato OpenAPI y Swagger UI.** Son documentación generada por springdoc. La revisé abriéndola, y una prueba automática de eso sería frágil y no protege la regla de negocio.

## 6. Lectura de la cobertura

Corrí `mvnw.cmd verify` (JaCoCo 0.8.15). Resultado: 47 pruebas, 0 fallas, y el reporte quedó en `target/site/jacoco/index.html`. Este es el resumen en texto (`jacoco.csv`):

| Clase | Líneas | Ramas | Instrucciones |
|---|---|---|---|
| `AvistamientoService` | 34/34 | 8/8 | 100 % |
| `ValidadorDeAvistamiento` | 47/49 | 29/30 | 96,2 % |
| `AvistamientoController` | 12/12 | 2/2 | 100 % |
| `ManejadorErrores` | 3/3 | — | 100 % |
| `DatosInvalidosException` | 4/4 | — | 100 % |
| `Avistamiento` (entidad) | 11/11 | — | 100 % |
| `AvistamientoRequest` | 1/1 | — | 100 % |
| `RelojDelSistema` | 1/2 | — | 60 % |
| `AvistamientosApplication` | 1/3 | — | 37,5 % |
| **Total** | **114/119 (95,8 %)** | **39/40 (97,5 %)** | **96,9 %** |

Miré dos criterios de cobertura: **líneas** (qué sentencias se ejecutaron) y **ramas** (si cada `if` se probó por el lado verdadero y por el falso). El de ramas es más exigente: una línea con un `if` puede salir "cubierta" aunque solo se haya probado uno de sus dos caminos.

**Cómo me sirvió el reporte.** La primera vez que lo corrí, con 41 pruebas, el validador estaba en 26 de 30 ramas. Las 4 ramas sin cubrir eran justo las de mis validaciones propias: largo máximo del lugar, largo máximo del observador y formato del observador. Al verlo en rojo me di cuenta de que, si alguien dañaba la expresión regular del observador, **ninguna prueba lo iba a detectar**, y esa regla sí la ve el usuario (un 400 con un mensaje). Como el taller permite usar las validaciones propias, agregué sus casos a la tabla de la sección 2 (6 pruebas más). Ahora el validador tiene 29 de 30 ramas y comprobé que esas pruebas detectan los defectos (sección 3).

Lo que sigue sin cubrir, y por qué lo dejo así:

- **La rama `datos == null` del validador (3 líneas).** Por HTTP no se puede llegar ahí: si el body es `null` o no viene, Spring lanza `HttpMessageNotReadableException` antes de llegar al validador, y la API ya responde 400 (lo probé con curl). La dejé como protección por si otro código llama al validador directamente. Una prueba unitaria sería de tres líneas, pero no protege ningún comportamiento que un usuario de la API pueda ver, así que **no la escribí**.
- **`RelojDelSistema.hoy()` (la línea `return LocalDate.now()`) no se ejecutó nunca.** Eso es bueno: confirma que ninguna prueba depende del reloj real, porque todas usan el `RelojFijo`. **No vale la pena** probarla, por lo que expliqué en la sección 5.
- **`AvistamientosApplication.main`.** `@SpringBootTest` arranca el contexto sin pasar por `main`. Son dos líneas que solo llaman a Spring. **No vale la pena.**

**Lo que concluyo.** El 97,5 % de ramas no significa que la API esté 97,5 % bien probada. JaCoCo cuenta líneas **ejecutadas**, no **verificadas**: una prueba sin ningún `assert` subiría la cobertura igual. Por ejemplo, `ManejadorErrores` sale al 100 %, pero lo que de verdad demuestra que funciona son los `assert` sobre el 400 y el mensaje, y el experimento de la sección 3 donde lo borré y fallaron 5 pruebas. Lo útil del reporte fue mostrarme **dónde no había ninguna prueba** y obligarme a decidir, caso por caso, si eso era un riesgo o no. En el caso del observador, sí lo era.
