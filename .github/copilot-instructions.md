# Instrucciones de Copilot para GameSpot

## Visión general del proyecto

Este repositorio contiene un backend en Java/Spring Boot para la plataforma de reservas de GameSpot. El código de la aplicación vive en `backend/` y está organizado como un monolito usando Spring MVC + Spring Data JPA + Spring Security.

Las capas principales de la arquitectura son:

- `backend/src/main/java/com/usta/gamespot/controller/`: controladores REST como `AuthController`
- `backend/src/main/java/com/usta/gamespot/service/`: lógica de negocio como `AuthService`
- `backend/src/main/java/com/usta/gamespot/repository/`: repositorios de Spring Data
- `backend/src/main/java/com/usta/gamespot/model/`: entidades JPA (`Usuario`, `Rol`)
- `backend/src/main/java/com/usta/gamespot/dto/`: DTOs de entrada y salida
- `backend/src/main/java/com/usta/gamespot/config/`: configuración de seguridad y de la aplicación
- `backend/src/main/java/com/usta/gamespot/security/`: generación y validación de JWT
- `backend/src/main/java/com/usta/gamespot/exception/`: excepciones de la aplicación y manejo global de errores

La app usa autenticación con JWT, sesiones sin estado y PostgreSQL como base de datos. Las pruebas usan Testcontainers con PostgreSQL, configurados en `backend/src/test/java/com/usta/gamespot/TestcontainersConfiguration.java`.

## Compilar, probar y validar

Ejecuta los comandos desde el directorio `backend` salvo que se indique lo contrario.

- Iniciar la aplicación:
  - `./mvnw spring-boot:run`
- Ejecutar toda la suite de pruebas:
  - `./mvnw test`
- Ejecutar una clase de prueba individual:
  - `./mvnw -Dtest=BackendApplicationTests test`
- Ejecutar un método de prueba individual:
  - `./mvnw -Dtest=BackendApplicationTests#contextLoads test`
- Compilar sin ejecutar pruebas:
  - `./mvnw -DskipTests compile`

No hay una tarea de lint dedicada definida en este repositorio. El proyecto depende de la compilación y pruebas con Maven, y además de las reglas de formato del equipo en `ESTANDARES.md` (Google Java Style e IntelliJ reformat-on-save).

## Notas de arquitectura de alto nivel

- La app es un único servicio backend, no un sistema distribuido. La mayor parte de la lógica de negocio está centrada en los controladores y servicios de Spring MVC.
- La seguridad se configura en `SecurityConfig` con manejo de JWT sin estado y CORS permitiendo `http://localhost:4200`.
- `AuthController` expone los endpoints de registro e inicio de sesión bajo `/api/auth`; las rutas de administración se esperan en `/api/admin/**` y requieren rol `ADMIN`.
- `JwtService` crea y valida tokens usando `app.jwt.secret` y `app.jwt.expiration-ms` desde la configuración de la aplicación.
- La nomenclatura de entidades sigue convenciones en español, y los nombres de tablas se mantienen en snake_case plural (por ejemplo `usuarios` y `password_hash`).
- La validación se hace con Jakarta Bean Validation, y las excepciones están centralizadas con `@RestControllerAdvice` en el paquete de exceptions.

## Convenciones clave específicas de este repositorio

- Usa identificadores y comentarios en español. No mezcles nombres en español e inglés en la misma clase o método, salvo que lo exija la API o el framework.
- Sigue los estándares del equipo en `ESTANDARES.md`:
  - El código Java usa Google Java Style mediante el formateo automático de IntelliJ al guardar
  - Los booleanos se nombran como preguntas (`estaDisponible`, `tieneAnticipoPagado`)
  - Los nombres de clase son PascalCase en singular; los nombres de tablas son snake_case en plural
- Las convenciones preferidas ya visibles en el código:
  - Las entidades usan Lombok (`@Getter`, `@Setter`, `@Builder`, etc.)
  - Los DTOs se implementan como records (`LoginRequest`, `RegistroRequest`, `AuthResponse`)
  - El nombrado de repositorios sigue convenciones de Spring Data (`findByEmail`, `existsByEmail`)
- Mantén los cambios relacionados con seguridad alineados con el patrón actual de JWT en `security/` y `config/SecurityConfig.java`; no introduzcas sesiones del lado del servidor en esta aplicación.
- Cuando modifiques autenticación o el modelo de usuario, valida tanto la semántica de la entidad como el flujo generado de JWT/token, porque registro e inicio de sesión devuelven tanto el token como los datos del usuario.

## Configuración y entorno

- La configuración principal de Spring está en `backend/src/main/resources/application.yaml`.
- El proyecto espera PostgreSQL y la configuración de JWT antes de ejecutar localmente.
- Para las pruebas, Testcontainers levanta automáticamente un contenedor de PostgreSQL; no hace falta una base de datos local extra para ejecutar `./mvnw test`.

## Estilo de trabajo para este código

- Prefiere cambios pequeños y alineados con el paquete correcto; la mayor parte de la lógica nueva debe ir en la capa adecuada: `controller`, `service` o `repository`.
- Evita refactorizaciones amplias o migraciones de frameworks sin una necesidad clara.
- Conserva el nombre en español y la estructura por capas al implementar nuevas funcionalidades.

## MCP Servers

Si en el futuro se requiere un cliente web o pruebas automatizadas UI, se puede considerar configurar MCPs relevantes como Playwright para front-end o herramientas de acceso a infraestructura, según el tipo de trabajo que se haga en el proyecto.

Este repositorio no exige un MCP específico por defecto; la configuración se debe decidir por necesidad real de la tarea.
