# Instrucciones de trabajo en GameSpot

Reglas prácticas para trabajar en el repositorio. Complementan
[ESTANDARES.md](ESTANDARES.md) (acuerdos del equipo, DoR, DoD y revisión) y el
[README.md](README.md) (cómo ejecutar). Si algo aquí contradice esos
documentos, mandan ellos y este archivo se corrige.

## El proyecto

Backend de reservas por intervalos para Game Spot (sala de videojuegos en
Villavicencio): reservas en bloques de 30 minutos, pago de adelanto con Wompi
(entorno de pruebas), puntos de fidelidad, notificaciones y panel de
administración. Proyecto del curso Gerencia de Software (USTA).

- Java 21, Spring Boot 4 (MVC, Data JPA, Security, Validation), JWT (jjwt
  0.11.5) sin sesiones, Lombok.
- PostgreSQL en Supabase. Pruebas con JUnit 5 y Testcontainers.
- Backend en `backend/`. Frontend en `frontend/` (Angular 22, Node 24), que
  llama a la API en `http://localhost:8080`; el CORS ya admite
  `http://localhost:4200`.
- Frontend: código y textos en español, componentes standalone con signals,
  colores y tipografía desde las variables de `frontend/src/styles.scss`
  (tema oscuro de Game Spot, contraste AA, estados con texto, usable desde
  360 px). Antes de un PR: `npx ng test --watch=false`, `npx ng build` y
  `npx prettier --check "src/**/*.{ts,html,scss}"`. Los casos de prueba del
  frontend usan el prefijo `CP-F` (`CP-F01a`, ...).

## Comandos

Se ejecutan desde `backend/`. En PowerShell usar `.\mvnw.cmd` en lugar de
`./mvnw`.

| Para qué | Comando |
| --- | --- |
| Compilar sin pruebas | `./mvnw -DskipTests compile` |
| Todas las pruebas (requiere Docker encendido) | `./mvnw test` |
| Una clase de prueba | `./mvnw -Dtest=ExpiracionReservasTests test` |
| Un método de prueba | `./mvnw -Dtest=AuthServiceTests#nombreDelMetodo test` |
| Estilo Google (falla con cualquier advertencia) | `./mvnw checkstyle:check` |
| Estilo desde cero (sin caché) | `./mvnw clean checkstyle:check` |
| Ejecutar la API | `./mvnw spring-boot:run` (necesita `JWT_SECRETO` y `SUPABASE_DB_CONTRASENA`) |

`./mvnw test` genera la cobertura en `backend/target/site/jacoco/index.html`.
Antes de abrir un PR deben pasar **las pruebas y Checkstyle**.

## Arquitectura

Paquete base `com.usta.gamespot`, por capas:

| Paquete | Contenido |
| --- | --- |
| `controller/` | Endpoints REST bajo `/api/...`. Delgados: validan y delegan al servicio. |
| `service/` | Lógica de negocio y transacciones (`@Transactional`). |
| `repository/` | Repositorios de Spring Data. Consultas nativas o JPQL cuando hace falta (ej. `expirarPendientes()`). |
| `model/` | Entidades JPA (Lombok) y enums. Los estados se guardan como texto en MAYÚSCULA (`@Enumerated(EnumType.STRING)`). |
| `dto/` | Entrada y salida como `record`, con Bean Validation. |
| `exception/` | Excepciones propias y `GlobalExceptionHandler` (`@RestControllerAdvice`) que responde con `ApiError`. |
| `security/` | `JwtService` y `JwtAuthFilter`. |
| `config/` | `SecurityConfig` (JWT sin estado, CORS, `/api/admin/**` solo `ADMIN`) y `ProgramacionConfig` (`@EnableScheduling`). |
| `tarea/` | Tareas programadas (`@Scheduled`), activables por propiedad. |

Reglas:

- La lógica va en `service/`, nunca en el controlador ni en la entidad.
- No introducir sesiones del lado del servidor; todo es JWT sin estado.
- Las propiedades propias van en `application.yaml` bajo `app.*`. Si una
  tarea o integración no debe correr en pruebas, se apaga en
  `backend/src/test/resources/config/application.yaml`.

## Base de datos

- `backend/src/main/resources/db/esquema.sql` es la **única fuente de verdad**.
  Hibernate solo valida (`ddl-auto: validate`): una entidad que no coincide
  con el esquema impide arrancar la aplicación y las pruebas.
- Las pruebas cargan ese mismo `esquema.sql` en el contenedor de PostgreSQL.
- Todo cambio de esquema se hace en **dos lugares**:
  1. `esquema.sql`, para que una base nueva quede completa.
  2. Una migración nueva numerada `V{n}_descripcion.sql` (la siguiente es
     `V3_...`), dentro de `begin; ... commit;`, para la base de Supabase que ya
     tiene datos. Se ejecuta a mano en el SQL Editor el día en que se fusiona
     el PR, y se anota como pendiente en la bitácora.
- Nunca editar una migración que ya se aplicó en Supabase.
- La base garantiza reglas del negocio por sí misma; no quitarlas ni
  duplicarlas de forma que se contradigan:
  - bloques alineados a :00 o :30 y duración múltiplo de 30 minutos;
  - ningún equipo con dos reservas activas que se crucen (restricción de
    exclusión, válida también con peticiones simultáneas);
  - una reserva CONFIRMADA o COMPLETADA siempre tiene adelanto pagado;
  - ciclo de vida: `PENDIENTE_PAGO → CONFIRMADA → COMPLETADA`,
    `PENDIENTE_PAGO → EXPIRADA`, `PENDIENTE_PAGO | CONFIRMADA → CANCELADA`.
- El servicio debe traducir las violaciones de estas restricciones a errores
  claros para el usuario (por ejemplo, 409 cuando el bloque ya está ocupado).

## Convenciones de código

- Todo en español: clases, métodos, variables, comentarios y mensajes. Sin
  mezclar idiomas en un mismo nombre (salvo lo que impone el framework).
- La unidad va en el nombre: `duracionMinutos`, `precioTotalPesos`,
  `montoCentavos`. Nunca `monto` o `duracion` a secas.
- Booleanos como pregunta: `estaDisponible`, `tieneAnticipoPagado`.
- Tablas en `snake_case` plural; clases en `PascalCase` singular.
- Google Java Style (2 espacios, formateador de IntelliJ al guardar).
- Checkstyle revisa `BackendApplication`, `config`, `controller`, `dto`,
  `exception`, `security`, `service`, `model/Usuario` y `model/Rol`. Toda
  clase y método público nuevo en esos paquetes necesita **Javadoc**. Si se
  crea un paquete nuevo (ej. `tarea/`), evaluar agregarlo a los `includes` del
  plugin en `pom.xml`.
- El Javadoc de la clase nombra la tarjeta y la historia que implementa:
  `/** CACS-12 (HU-05): libera las reservas que no se pagaron dentro del plazo. */`
- Inyección por constructor con `@RequiredArgsConstructor` y campos
  `private final`.
- Ninguna credencial, llave de Wompi o secreto en el código ni en archivos
  versionados: siempre variables de entorno (`JWT_SECRETO`,
  `SUPABASE_DB_CONTRASENA`, ...). Esto bloquea un PR.

## Pruebas

- Al menos una prueba automatizada por criterio de aceptación (DoD).
- Cada caso se identifica con su código de caso de prueba (`CP-21a`, ...),
  continuando la numeración existente. Revisar el último CP usado antes de
  crear nuevos.
- Pruebas de integración con este encabezado:

  ```java
  @Import(TestcontainersConfiguration.class)
  @SpringBootTest
  @TestPropertySource(properties = "app.cli.registration.enabled=false")
  @Transactional
  ```

  `@Transactional` revierte los datos de cada prueba.
- Usar `@DisplayName` en español y fechas fijas (`Instant.parse(...)`) en vez
  de la hora actual. La zona del negocio es Bogotá (UTC-5).
- Validaciones de DTO sin Spring ni Docker, como
  `dto/RegistroRequestValidacionTest`.
- Al agregar una clase de pruebas, actualizar la tabla de pruebas del README.

## Flujo de trabajo

### Jira

Proyecto **CACS** en `cristiansantiagosilvamunoz.atlassian.net`. Cada cambio
corresponde a una tarjeta; la historia de usuario va entre paréntesis
(`CACS-12 (HU-05)`). Al terminar, la tarjeta se pasa a "Listo".

**Todo movimiento de una tarjeta se justifica.** Cada vez que una tarjeta
cambia de estado, de sprint, de responsable o de prioridad, se deja en ese
mismo momento un comentario en la tarjeta que diga:

- **Qué cambió:** de dónde a dónde (por ejemplo, "Por hacer → En curso").
- **Por qué:** el motivo concreto.
- **Evidencia:** lo que respalda el cambio, cuando aplica: rama, PR, resultado
  de las pruebas o la decisión del equipo.

Ejemplos:

- `Por hacer → En curso: se empieza en la rama feat/modulo-reservas; cumple la DoR (criterios y estimación registrados).`
- `En curso → Listo: PR #15 aprobado por Cristian Amado y fusionado en dev; ./mvnw test 70 pruebas, 0 fallos.`
- `Sprint 2 → Sprint 3: no alcanzó a terminarse porque depende de CACS-5 (pagos), que sigue en curso.`

Un movimiento sin comentario se considera incompleto: quien lo note lo
señala y quien movió la tarjeta agrega la justificación.

### Ramas

- `main`: solo código que funciona. `dev`: integración (en ESTANDARES.md
  aparece como `develop`; el nombre real es `dev`).
- Cada tarjeta en `feat/nombre-corto`, creada desde `dev` actualizado.
- Nunca hacer push directo a `dev` ni a `main`, ni `push --force`.

### Commits

- Conventional Commits en español e imperativo, con la tarjeta al final:
  `feat(reservas): programar la expiración de reservas no pagadas (CACS-12)`.
- Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `style`. Un cambio con
  sentido propio por commit; lo habitual es separar código, pruebas y
  bitácora (`feat(...)`, `test(...)`, `docs(bitacora): ...`).
- Los cambios solo de formato van en un commit `style(...)` aparte.
- El único autor es el integrante: sin líneas `Co-Authored-By` ni firmas
  adicionales en commits ni PRs.

### Pull requests

- Siempre hacia `dev`. Lo revisa el integrante que no es el autor: Cristian
  Silva (CSSM01) y Cristian Amado (cristianamad0) se revisan entre sí.
- Título: `CACS-n: descripción`. Cuerpo con estas secciones:

  ```markdown
  ## Qué cierra
  CACS-n (HU-xx): qué queda resuelto para el usuario.

  ## Cambios
  - `paquete/Clase`: qué cambió y por qué.

  ## Pruebas
  - `./mvnw test`: N pruebas, 0 fallos, 0 errores.
  - `ClaseTests` (CP-xx a CP-yy): qué cubre cada caso.
  - `./mvnw checkstyle:check`: 0 advertencias.

  ## Nota
  Riesgos, pasos manuales (ej. migración en Supabase) o trabajo futuro.
  ```

### Bitácora

Cada tarea terminada agrega una entrada **al final** de
[docs/bitacora.md](docs/bitacora.md), con el formato existente:

```markdown
## AAAA-MM-DD — Título

- **Tarjeta:** CACS-n (HU-xx).
- **Rama:** `feat/...`
- **Qué se hizo:**
  - ...
- **Pruebas:** `./mvnw test`: N pruebas, 0 fallos, 0 errores. ...
- **Pendientes:**
  - ...
```

## Antes de dar algo por terminado

1. Cumple los criterios de aceptación de la tarjeta.
2. Hay pruebas por cada criterio y `./mvnw test` pasa completo.
3. `./mvnw checkstyle:check` da 0 advertencias.
4. Si cambió el esquema: `esquema.sql` y la migración `V{n}` están al día.
5. README (tabla de pruebas, estado de módulos) y bitácora actualizados.
6. Commits con el formato acordado, en una rama `feat/...`, y PR hacia `dev`.
