# Justificación de las pruebas: API de avistamientos

Taller de pruebas unitarias y de integración. Ingeniería de Software 2, Sesión 22 (21 de octubre de 2026).
Juanita Mejía Pineda.

En total quedaron 47 pruebas: 30 unitarias (23 del validador y 7 del servicio) y 17 de integración (16 de los endpoints y la que revisa que la app arranque). Los comandos para correrlas y para sacar la cobertura están en el README.

## 1. Qué cambió en el código

En la versión del 29 de septiembre las validaciones eran anotaciones en la entidad (`@NotBlank`, `@Size`, `@PastOrPresent`) y el `@Valid` del controller era el que las activaba. Funcionaba, pero la regla no se podía probar sola, solo existía con Spring corriendo. Además `@PastOrPresent` usa el reloj del sistema por dentro, entonces no había forma de decirle a una prueba qué día es "hoy". Por eso cambié cuatro cosas:

- **Saqué la regla a su propia clase, `ValidadorDeAvistamiento`.** Recibe los datos y devuelve un mapa con el campo que falló y el mensaje. No sabe nada de HTTP ni de la base, así que la puedo crear con `new` y probarla en milisegundos. Le quité las anotaciones a la entidad para que la regla quedara en un solo lugar. Si las dejaba, el `@Valid` respondía 400 antes de que el validador se ejecutara y las pruebas de integración nunca iban a pasar por él.
- **El "hoy" ahora lo da una interfaz `Reloj`.** En producción se inyecta `RelojDelSistema` (que hace `LocalDate.now()`) y en las pruebas un `RelojFijo`. Es la táctica de limitar el no determinismo que vimos en testabilidad, y funciona por inversión de dependencias: el validador depende de la interfaz y no de la clase concreta.
- **Agregué una capa de servicio, `AvistamientoService`.** Antes el controller hablaba directo con el repositorio. Ahora el controller solo se encarga de lo de HTTP y el servicio valida, convierte y guarda. Como el servicio recibe el repositorio por el constructor, en una prueba unitaria le puedo pasar un doble.
- **La fecha ahora llega como texto (`AvistamientoRequest`).** Si llega como `LocalDate`, Jackson rechaza algo como `"2026-13-45"` antes de que se valide y el error no dice qué campo falló, que es justo lo que pide la regla. Ahora el validador revisa el formato y responde `{"fecha": "la fecha no existe en el calendario"}`.

Con estos cambios la API ganó controlabilidad, porque puedo fijar la entrada, el día y lo que responde el repositorio. También ganó observabilidad, porque el validador dice exactamente qué campo falló en vez de lanzar una excepción de Spring que toca interpretar.

## 2. Tabla de casos (Parte A)

Para escoger los casos usé partición de equivalencia, o sea, dividir la entrada en clases que el sistema trata igual y probar un valor de cada una. También usé valores límite: probar justo en el borde de cada clase y a los dos lados, porque ahí es donde se cuela un `<` en vez de un `<=`. En las pruebas "hoy" es el 21 de octubre de 2026, que es lo que dice el `RelojFijo`.

**Especie** (regla: entre 3 y 50 caracteres). Las clases son: vacía, muy corta (1 a 2), válida (3 a 50), muy larga (51 o más) y con caracteres que no se permiten.

| Valor | Tipo de caso | Resultado esperado | Por qué ese valor |
|---|---|---|---|
| `""` | Inválido (vacía) | Error: "la especie es obligatoria" | Es la clase de "no hay dato", que tiene su propio mensaje |
| `"ab"` (2) | Límite inválido | Error: "entre 3 y 50 caracteres" | Es el último valor de la clase muy corta. Si alguien pone `< 2` en vez de `< 3`, solo este caso lo detecta |
| `"abc"` (3) | Límite válido | Sin errores | El primer valor válido, o sea el otro lado del mismo borde |
| `"colibri"` (7) | Representativo válido | Sin errores | Un valor del medio de la clase válida (está en la prueba de "todo válido") |
| 50 letras | Límite válido | Sin errores | El último valor válido |
| 51 letras | Límite inválido | Error: "entre 3 y 50 caracteres" | El primer valor de la clase muy larga |
| `"   ab   "` | Límite inválido | Error: "entre 3 y 50 caracteres" | Tiene 8 caracteres pero solo 2 letras. Sirve para ver que los espacios de los lados no cuentan; sin el `trim()` pasaría como válida |
| `"------"` | Inválido (formato) | Error: "solo puede tener letras…" | El largo es válido (6) pero no son letras. Es otra clase aunque el largo esté bien |

**Fecha** (regla: formato AAAA-MM-DD y que no sea posterior a hoy). Las clases son: sin fecha, formato incorrecto, formato bien pero la fecha no existe, pasada o de hoy (válida) y futura.

| Valor | Tipo de caso | Resultado esperado | Por qué ese valor |
|---|---|---|---|
| `"2026-09-29"` | Representativo válido | Sin errores | Una fecha cualquiera del pasado |
| `"2026-10-21"` (hoy) | Límite válido | Sin errores | La regla dice "no posterior a hoy", entonces hoy sí vale. Si alguien usa `!isBefore` en vez de `isAfter`, solo este caso falla |
| `"2026-10-22"` (mañana) | Límite inválido | Error: "no puede ser posterior a hoy" | El primer día de la clase futura |
| `"21/10/2026"` | Inválido (formato) | Error: "debe tener el formato AAAA-MM-DD" | Es una fecha real pero escrita como la escribimos en Colombia, que es el error más probable de alguien usando la API |
| `"2026-02-30"` | Inválido (no existe) | Error: "no existe en el calendario" | El formato está bien pero el día no existe (2026 no es bisiesto). Lo escogí en vez de `2026-13-45` porque un parser permisivo no lo rechaza, sino que lo cambia sin avisar por otra fecha (el 28 de febrero o el 2 de marzo, depende del modo). Un mes 13 lo rechaza cualquiera. El `2026-13-45` quedó en la prueba de integración |
| `null` | Inválido (vacía) | Error: "la fecha es obligatoria" | Para cuando el campo no viene en el JSON |

**Lugar y observador.** La regla del taller solo pide que no estén vacíos, pero el taller también dice que si uno ya tenía validaciones propias las puede usar. Yo ya tenía de septiembre un largo máximo para los dos (150 y 100) y un formato para el observador, entonces también los partí en clases y probé sus límites:

| Campo | Valor | Tipo de caso | Resultado esperado | Por qué ese valor |
|---|---|---|---|---|
| Lugar | `"   "` | Inválido (vacío) | Error: "el lugar es obligatorio" | Solo espacios. `isEmpty()` diría que no está vacío pero `isBlank()` sí lo detecta, por eso es el caso vacío más engañoso |
| Lugar | `"Humedal La Conejera"` | Representativo válido | Sin errores | Un lugar normal (está en la prueba de "todo válido") |
| Lugar | 150 letras | Límite válido | Sin errores | El último valor válido |
| Lugar | 151 letras | Límite inválido | Error: "no puede tener más de 150 caracteres" | El primer valor de la clase muy larga |
| Observador | `""` | Inválido (vacío) | Error: "el observador es obligatorio" | La clase de "no hay dato" |
| Observador | `"Juanita"` | Representativo válido | Sin errores | Un nombre normal (está en la prueba de "todo válido") |
| Observador | `"María-José O'Neil Jr."` | Representativo válido | Sin errores | Usa todos los caracteres especiales que se permiten (tilde, guion, apóstrofe y punto). Si alguien "simplifica" la expresión regular, este caso lo detecta |
| Observador | 100 letras | Límite válido | Sin errores | El último valor válido |
| Observador | 101 letras | Límite inválido | Error: "no puede tener más de 100 caracteres" | El primer valor de la clase muy larga |
| Observador | `"Juanita123"` | Inválido (formato) | Error: "solo puede tener letras…" | El largo es válido pero tiene números. Es otra clase, igual que `"------"` en la especie |

Además hay una prueba con los cuatro campos mal al tiempo, para revisar que el validador devuelva todos los errores y no se quede solo con el primero.

En total son 23 pruebas unitarias del validador. Ahí están los seis límites que pide el taller (especie de 2, 3, 50 y 51; fecha de hoy y de mañana) y los de mis validaciones propias.

## 3. Por qué cada nivel

**Los límites del validador son pruebas unitarias.** La regla es lógica pura: entra un `AvistamientoRequest` y sale un mapa. No hay base de datos ni HTTP de por medio, entonces probarla a través de la API sería más lento y, si fallaba, no sabría si el problema es la regla, el JSON o el controller. Como unitarias son rápidas, independientes y cada una revisa una sola cosa, que es lo que pide FIRST.

**El servicio también tiene pruebas unitarias, con un doble del repositorio.** Ahí lo que quiero revisar son decisiones del servicio: que si los datos están mal no guarde nada, que si el id no existe no borre nada, que el PUT guarde con el mismo id y que antes de guardar limpie los espacios y convierta la fecha a `LocalDate`. Para nada de eso se necesita una base real.

**El POST tiene pruebas de integración aunque el validador ya esté probado.** La unitaria del validador no puede ver lo que pasa en las fronteras entre componentes:

- que el JSON se mapee bien al `AvistamientoRequest` (si un campo se llamara diferente, llegaría `null`);
- que el controller sí llame al servicio y el servicio al validador;
- que `ManejadorErrores` convierta la `DatosInvalidosException` en un 400 con el JSON de campo y mensaje. Esto lo comprobé: borré ese método y las 30 unitarias siguieron pasando, pero fallaron 5 de integración, porque la excepción se escapaba y la API habría respondido 500;
- que un JSON mal escrito también dé 400;
- que el avistamiento sí quede guardado: después del POST hago un GET al id nuevo y lo tiene que devolver.

Por eso las de integración revisan el cuerpo y el efecto, no solo el código de estado. Después del PUT reviso que en la base esté el dato nuevo, después de un PUT inválido que siga el viejo, y después del DELETE que el GET responda 404. Hay por lo menos un caso de éxito y uno de error por endpoint. Cada prueba arma sus propios datos y el `@BeforeEach` vacía la base H2 en memoria, entonces no dependen del orden en que corran.

Hay casos que están en los dos niveles, como la fecha de hoy, la de mañana y la especie de 2. No es repetir lo mismo: la unitaria prueba la regla y la de integración prueba que la regla sí está conectada y que se usa el reloj que se inyecta en el contexto de Spring.

**¿Las pruebas sí detectan defectos?** Que una prueba pase no demuestra que el código esté bien (es el primer principio). Para revisar que mis pruebas no pasaran "por pasar", metí a propósito ocho defectos típicos en el validador, uno a la vez, y corrí todo:

| Defecto que metí | Pruebas que fallaron |
|---|---|
| Mínimo de la especie en 2 en vez de 3 | 7, entre ellas `especieDe2Caracteres` (unitaria y de integración) |
| Máximo de la especie en 51 en vez de 50 | 4, entre ellas `especieDe51Caracteres` |
| `!isBefore(hoy)` en vez de `isAfter(hoy)` (también rechaza hoy) | 2: `fechaDeHoy` unitaria y de integración |
| Quitar el `trim()` de la especie | 2: `especieDe2LetrasRodeadaDeEspacios` y la del servicio que revisa los datos limpios |
| Máximo del lugar en 151 en vez de 150 | 1: `lugarDe151Caracteres` |
| Máximo del observador en 99 en vez de 100 | 2: `observadorDe100Caracteres` y `observadorDe101Caracteres` |
| Expresión regular del observador que acepta cualquier cosa | 1: `observadorConNumeros` |
| Expresión regular del observador sin punto, guion ni apóstrofe | 1: `observadorConPuntoGuionYApostrofe` |

Cada defecto lo agarró justo la prueba de su límite o de su clase. Es lo mismo del ejemplo del envío gratis que vimos en clase: solo el valor que está en el borde detecta el `>` en vez de `>=`.

## 4. Los dobles

| Doble | Tipo | Dónde | Por qué ese tipo |
|---|---|---|---|
| `RelojFijo` | Stub | Pruebas del validador, del servicio y de integración | Solo necesito que devuelva una fecha preparada (21/10/2026). No reviso cómo lo llaman, por eso es un stub y no un mock |
| `AvistamientoRepository` con Mockito | Stub y mock | `AvistamientoServiceTest` | Con `when(...).thenReturn(...)` lo uso como stub, para decidir qué responde `existsById` o `findAll`. Con `verify(repository, never()).save(...)` lo uso como mock, porque ahí lo que importa es una interacción: que no se llamó a guardar |

**¿Qué pasaba sin el `RelojFijo`?** La prueba de "fecha de mañana" tendría que usar `LocalDate.now().plusDays(1)` o una fecha escrita a mano como `2026-10-22`. La escrita a mano pasa hoy y falla mañana, porque el 22 se vuelve "hoy" y ya es válida. Con `now()` el resultado depende de la hora: cerca de la medianoche el `now()` de la prueba y el del validador pueden caer en días distintos. En los dos casos la prueba deja de ser repetible (la R de FIRST). En las de integración también cambié el reloj real por el `RelojFijo`, con un `@TestConfiguration` y `@Primary`, para que la frontera entre hoy y mañana también se pruebe por HTTP con una fecha fija.

**¿Por qué un mock y no un fake?** Un fake (un repositorio en memoria con un `HashMap`) también hubiera servido. No lo usé porque esas pruebas son sobre lo que el servicio hace o no hace con el repositorio, y con el mock eso se escribe directo (`never().save`). Con un fake me tocaba implementar los métodos de `JpaRepository` que uso y después revisar el estado. Y sin ningún doble, el servicio necesitaría una base de datos y la prueba ya sería de integración, más lenta y sin aislar.

No usé dummy porque ningún método recibe un parámetro que no se use. Tampoco spy, porque el `verify` de Mockito ya me da lo mismo que daría un spy (revisar las llamadas después). En las de integración no cambié la base por un doble, porque justo lo que quiero probar ahí es la colaboración real con JPA, y H2 en memoria es rápida y se vacía antes de cada prueba. La API no habla con ningún servicio externo, entonces no hizo falta un servidor simulado.

## 5. Lo que no probé

- **Getters y setters de la entidad y del record.** No tienen lógica. Probarlos sería probar Java y solo subiría la cobertura sin detectar nada. Igual se ejecutan en todas las pruebas de integración.
- **`RelojDelSistema.hoy()`.** Es una sola línea: `return LocalDate.now()`. Para probarla tendría que compararla contra `LocalDate.now()`, que es probar la librería, y además vuelve a meter el no determinismo que quería sacar.
- **`AvistamientosApplication.main`.** Solo arranca Spring. Que la app arranca ya lo revisa `contextLoads` y, en la práctica, todas las pruebas de integración.
- **Los métodos del repositorio (`save`, `findById`, etc.).** Son de Spring Data JPA, no los hice yo. Las pruebas de integración los usan con una base real y con eso me basta.
- **Los límites del lugar y del observador por HTTP.** Están probados en las unitarias del validador, pero no los repetí uno por uno en integración. Ya hay pruebas de integración que muestran que cualquier error del validador sale como 400 con su campo (la de especie, la de fecha y la de "incompleto", que cubre lugar y observador). Repetir cada límite por HTTP sería más lento y no detectaría nada nuevo.
- **La generación del contrato OpenAPI y Swagger UI.** Es documentación que genera springdoc. La revisé abriéndola, y una prueba automática de eso sería frágil y no protege la regla de negocio.

## 6. Lectura de la cobertura

Corrí `mvnw.cmd verify` (JaCoCo 0.8.15). Salieron 47 pruebas y 0 fallas, y el reporte quedó en `target/site/jacoco/index.html`. Este es el resumen en texto (`jacoco.csv`):

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

Miré dos criterios de cobertura: líneas (qué sentencias se ejecutaron) y ramas (si cada `if` se probó por el lado verdadero y por el falso). El de ramas es más exigente, porque una línea con un `if` puede salir cubierta aunque solo se haya probado uno de los dos caminos.

**Para qué me sirvió el reporte.** La primera vez que lo corrí, con 41 pruebas, el validador iba en 26 de 30 ramas. Las 4 que faltaban eran justo las de mis validaciones propias: el largo máximo del lugar, el del observador y el formato del observador. Al verlas en rojo me di cuenta de que si alguien dañaba la expresión regular del observador, ninguna prueba lo iba a notar, y esa regla sí la ve el usuario (le sale un 400 con un mensaje). Como el taller deja usar las validaciones propias, agregué esos casos a la tabla de la sección 2 (6 pruebas más). Ahora el validador está en 29 de 30 ramas, y en la sección 3 está cómo comprobé que esas pruebas sí detectan los defectos.

Lo que sigue sin cubrir y por qué lo dejé así:

- **La rama `datos == null` del validador (3 líneas).** Por HTTP no se llega ahí: si el body es `null` o no viene, Spring lanza `HttpMessageNotReadableException` antes de llegar al validador, y la API ya responde 400 (lo probé con curl). La dejé como protección por si otro código llama al validador directo. La prueba unitaria serían tres líneas, pero no protege nada que un usuario de la API pueda ver, entonces no la escribí.
- **`RelojDelSistema.hoy()` no se ejecutó nunca.** Eso está bien, porque confirma que ninguna prueba depende del reloj real: todas usan el `RelojFijo`. No vale la pena probarla, por lo que expliqué en la sección 5.
- **`AvistamientosApplication.main`.** `@SpringBootTest` arranca el contexto sin pasar por el `main`. Son dos líneas que solo llaman a Spring, no vale la pena.

**Conclusión.** Que haya 97,5 % de ramas no quiere decir que la API esté 97,5 % bien probada. JaCoCo cuenta líneas ejecutadas, no verificadas: una prueba sin ningún `assert` también sube la cobertura. Por ejemplo, `ManejadorErrores` sale en 100 %, pero lo que de verdad muestra que funciona son los `assert` sobre el 400 y el mensaje, y el experimento de la sección 3 donde lo borré y fallaron 5 pruebas. Para mí lo útil del reporte fue ver dónde no había ninguna prueba y decidir caso por caso si eso era un riesgo o no. En el caso del observador sí lo era.
