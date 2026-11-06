# GameSpot

Plataforma web de reservas por intervalos para Game Spot, con pago de adelanto y
panel de administración. Proyecto del curso Gerencia de Software (Universidad
Santo Tomás, sede Villavicencio).

## Estado actual

| Módulo | Estado |
| --- | --- |
| Registro e inicio de sesión con JWT (HU-01) | Implementado: `POST /api/auth/registro` y `POST /api/auth/login` |
| Esquema de base de datos (reservas, pagos, puntos, notificaciones, horarios) | Implementado en Supabase con sus reglas (bloques de 30 min, sin cruces, adelanto, puntos) |
| Entidades y repositorios JPA | Implementados |
| Expiración de reservas no pagadas (HU-05) | Implementado: tarea programada cada minuto |
| Panel de administración de reservas (HU-06) | Implementado: `GET /api/admin/reservas`, `POST /api/admin/reservas/{id}/cancelar`, `POST /api/admin/reservas/{id}/completar` y `GET /api/admin/reservas/expiradas-con-pago` (solo `ADMIN`) |
| Pago del adelanto con proveedor simulado (HU-03) | Implementado: `POST /api/reservas/{id}/pago` (solo el dueño de la reserva); el proveedor simulado se cambiará por Wompi en pruebas |
| Puntos de fidelización (HU-04) | Implementado: `GET /api/puntos` (saldo e historial del cliente) y `POST /api/admin/usuarios/{usuarioId}/puntos` (ajuste manual, solo `ADMIN`) |
| Reservas por bloques de 30 minutos (HU-02) | Implementado: `POST /api/reservas` (crea la reserva en `PENDIENTE_PAGO` con precio, adelanto y plazo de pago) y `GET /api/reservas/disponibilidad?equipoId=&fecha=` (bloques libres y ocupados del día) |
| Frontend: base, identidad visual e inicio de sesión (HU-01) | Implementado en `frontend/` (Angular 22): inicio, registro, ingreso, sesión con JWT, guardas por rol y tema oscuro de Game Spot. Reservar muestra "Próximamente" hasta CACS-19 |
| Frontend: pagar el adelanto y mis puntos (HU-03 y HU-04) | Implementado en `/reservas/:id/pagar` (monto, cuenta regresiva del plazo, aprobado o rechazado y reintento; selector del proveedor simulado solo con `ng serve`) y `/mis-puntos` (saldo e historial). Backend: `GET /api/reservas/{id}` para que el cliente lea su reserva (solo el dueño) |
| Frontend: panel del administrador (HU-06 y HU-04) | Implementado en `/admin`: reservas del día por equipo con filtros, cancelar con motivo y completar; expiradas con pago aprobado; ajuste de puntos buscando al cliente por correo (`GET /api/admin/usuarios?email=`, solo `ADMIN`) |

## Tecnologías

- Java 21 y Spring Boot 4 (Spring MVC, Spring Data JPA, Spring Security, Bean Validation)
- JWT (jjwt) con sesiones sin estado
- PostgreSQL en Supabase
- Lombok
- Pruebas: JUnit 5 y Testcontainers (PostgreSQL)
- Frontend: Angular 22 (componentes standalone, signals, sin zone.js), SCSS y
  pruebas con Vitest. Necesita Node 24.

## Estructura

```
GameSpot/
├── backend/                      Proyecto Maven de Spring Boot
│   └── src/
│       ├── main/java/com/usta/gamespot/
│       │   ├── config/           Seguridad y CORS
│       │   ├── controller/       Endpoints REST
│       │   ├── dto/              Objetos de entrada y salida (records)
│       │   ├── exception/        Excepciones y manejador global de errores
│       │   ├── model/            Entidades JPA y enumeraciones
│       │   ├── repository/       Repositorios de Spring Data
│       │   ├── security/         Generación y validación de JWT
│       │   └── service/          Lógica de negocio
│       ├── main/resources/
│       │   ├── application.yaml
│       │   └── db/               esquema.sql, borrar_esquema.sql y migraciones (V2...)
│       └── test/java/...         Pruebas de integración y unitarias
├── frontend/                     Aplicación web en Angular
│   └── src/
│       ├── styles.scss           Identidad visual: colores, tipografía, botones y formularios
│       └── app/
│           ├── core/             Sesión (JWT), interceptor, guardas, errores y modelos
│           ├── compartido/       Componentes reutilizables (estado de una reserva)
│           ├── diseno/           Barra de navegación
│           └── paginas/          Una carpeta por pantalla
├── docs/calidad/                 Reporte de análisis estático (PMD, Checkstyle, Lizard)
└── ESTANDARES.md                 Acuerdos del equipo, DoR, DoD y revisión de código
```

## Base de datos

PostgreSQL en Supabase. El esquema completo está en
`backend/src/main/resources/db/esquema.sql` y Spring Boot solo lo valida
(`ddl-auto: validate`); no crea ni modifica tablas.

**Crear o recrear el esquema en Supabase** (SQL Editor > New query):

1. Si ya existen tablas y todavía no hay datos reales, ejecutar
   `backend/src/main/resources/db/borrar_esquema.sql`.
2. Ejecutar `backend/src/main/resources/db/esquema.sql`.

**Aplicar un cambio sobre un esquema que ya tiene datos.** Los cambios
posteriores van en scripts de migración numerados en la misma carpeta
(`V2_datos_negocio.sql`, ...). Cada uno se ejecuta una sola vez en el SQL
Editor, el mismo día en que se fusiona su PR, y va dentro de una transacción.
`esquema.sql` ya incluye todos los cambios, así que una base nueva no
necesita las migraciones.

## Ejecutar el backend

Requisitos: JDK 21.

1. Definir la variable de entorno `SUPABASE_DB_CONTRASENA` con la contraseña de
   la base de datos (en IntelliJ: Run > Edit Configurations > Environment
   variables). Si la red no tiene IPv6, definir también `SUPABASE_DB_URL` y
   `SUPABASE_DB_USUARIO` con los datos del "Session pooler" (botón Connect del
   proyecto en Supabase).
2. Definir la variable de entorno `JWT_SECRETO` con el secreto que firma los
   tokens de sesión. Debe tener al menos 32 caracteres (jjwt rechaza los más
   cortos) y no se escribe en ningún archivo del repositorio. Para generar uno:

   ```bash
   openssl rand -base64 48
   ```

   Sin esta variable la aplicación no arranca. Las pruebas no la necesitan:
   usan el valor de `backend/src/test/resources/config/application.yaml`, que
   solo sirve para pruebas.
3. Desde `backend/`:

   ```bash
   ./mvnw spring-boot:run
   ```

La API queda en `http://localhost:8080` y acepta peticiones CORS desde
`http://localhost:4200`.

**Probar la API a mano sin tocar Supabase.** Con Docker encendido, desde
`backend/`:

```bash
./mvnw spring-boot:test-run
```

Levanta la API en `http://localhost:8080` con un PostgreSQL temporal que tiene
el mismo `esquema.sql` y se borra al detenerla (Ctrl+C). No necesita
`JWT_SECRETO` ni `SUPABASE_DB_CONTRASENA`. Las peticiones se hacen con
Postman, el HTTP Client de IntelliJ, `curl` o el frontend.

Este arranque activa el perfil `demo` (`TestBackendApplication` y
`src/test/resources/config/application-demo.yaml`), que carga datos de
demostración (CACS-16). Son valores de prueba, no credenciales reales:

| Rol | Correo | Contraseña |
| --- | --- | --- |
| `ADMIN` | `admin@gamespot.demo` | `AdminDemo123` |
| `CLIENTE` | `laura@gamespot.demo` | `ClienteDemo123` |
| `CLIENTE` | `andres@gamespot.demo` | `ClienteDemo123` |

Las fechas se calculan al arrancar (hora de Bogotá):

- **Laura:** una reserva `COMPLETADA` ayer (2 horas, con sus puntos) y, para
  mañana, una `CONFIRMADA`, una `PENDIENTE_PAGO` con un pago rechazado (se
  puede pagar hasta la hora de inicio) y una `CANCELADA`. Saldo: 12 puntos.
- **Andrés:** una reserva `CONFIRMADA` hoy a las 20:00 y una `EXPIRADA` ayer
  cuyo pago se aprobó tarde (aparece en "expiradas con pago" del panel).
  Saldo: 5 puntos.

El cargador está en el código de pruebas (`demo/DatosDemostracion`), así que
nunca llega al jar ni corre con la configuración de Supabase; además solo se
activa con `app.demo.datos=true`. Si se arranca dos veces sobre la misma base,
no duplica los datos.

## Ejecutar el frontend

Requisitos: Node 24. Con la API corriendo en `http://localhost:8080` (por
ejemplo con `spring-boot:test-run`), desde `frontend/`:

```bash
npm ci
```

```bash
npm start
```

La aplicación queda en `http://localhost:4200`. La primera vez, `npm ci`
instala las dependencias de `package-lock.json`.

## Pruebas

Desde `backend/`:

```bash
./mvnw test
```

Requieren Docker en ejecución: Testcontainers levanta un PostgreSQL con el
mismo `esquema.sql`, así que no se toca la base de datos de Supabase.

| Clase | Qué cubre | Casos |
| --- | --- | --- |
| `dto/RegistroRequestValidacionTest` | Validaciones del formulario de registro (no necesita Docker) | CP-02 a CP-04 |
| `AuthServiceTests` | Registro e inicio de sesión | CP-01, CP-05 a CP-07 |
| `ReglasBaseDatosTests` | Reglas de reservas, pagos y puntos en la base de datos | CP-08 a CP-16 |
| `DatosNegocioTests` | Equipos, horario de atención y configuración reales del negocio | CP-17 a CP-20 |
| `ExpiracionReservasTests` | Expiración de las reservas no pagadas a tiempo | CP-21a a CP-21d |
| `AdminReservasTests` | Panel de administración: acceso solo `ADMIN`, listar, cancelar y completar reservas | CP-22a a CP-22k |
| `PagoAnticipoTests` | Pago del adelanto: aprobado, rechazado y reintento, un solo pago aprobado, reservas no pagables o ajenas, monto menor y sin datos de tarjeta | CP-23a a CP-23k |
| `PuntosFidelidadTests` | Puntos de fidelización: puntos al completar una sola vez, saldo e historial propios, ajustes del admin con descripción y sin saldo negativo | CP-24a a CP-24i |
| `RespuestasErrorServidorTests` | Códigos de error con un servidor real (403, 400 y 401), que MockMvc no reproduce | CP-25a a CP-25c |
| `AdminUsuariosTests` | Búsqueda de usuarios por parte del correo: sin distinguir mayúsculas, máximo 10, lista vacía, 400 con menos de 3 caracteres, solo `ADMIN` | CP-28a a CP-28e |
| `ReservasTests` | Reservas del cliente: precio y adelanto, bloques de :00 o :30 y múltiplos de 30 minutos, horario de cada día, bloques ya pasados, cruces (409), disponibilidad por estado, equipo fuera de servicio (409), 401 sin token y reserva pagada de punta a punta; el cliente lee su reserva (403 ajena, 404 inexistente) | CP-26a a CP-26p, CP-30a a CP-30c |
| `PruebasSimultaneasTests` | Concurrencia real con servidor y PostgreSQL: 10 reservas simultáneas del mismo bloque, bloques que se cruzan en parte, bloques contiguos y otros equipos, pagos simultáneos de una reserva y 16 carreras seguidas sin resultados intermitentes. Sin `@Transactional`: borra lo que crea | CP-27a a CP-27e |
| `DatosDemostracionTests` | Datos de demostración: con el perfil `demo` se inicia sesión como admin y cliente, hay reservas en los cinco estados entre ayer y mañana, puntos y una expirada con pago, no se duplican y las contraseñas van cifradas; sin el perfil no se cargan | CP-29a a CP-29f |

Pruebas del frontend, desde `frontend/` (no necesitan Docker ni la API):

```bash
npx ng test --watch=false
```

| Archivo | Qué cubre | Casos |
| --- | --- | --- |
| `core/auth/sesion.spec.ts` | Guardar, recuperar y cerrar la sesión; token vencido | CP-F01a a CP-F01d |
| `core/auth/token.interceptor.spec.ts` | Token en las peticiones a la API, nunca a otros sitios; 401 cierra la sesión | CP-F02a a CP-F02d |
| `core/auth/guardas.spec.ts` | Rutas protegidas y `/admin` solo para `ADMIN` | CP-F03a a CP-F03d |
| `core/errores/mensaje-error.spec.ts` | Mensajes de error en español desde `ApiError` | CP-F04a a CP-F04c |
| `paginas/ingresar/ingresar.spec.ts` | Inicio de sesión, credenciales incorrectas y formulario vacío | CP-F05a a CP-F05c |
| `paginas/registro/registro.spec.ts` | Registro, validaciones y correo ya registrado | CP-F06a a CP-F06d |
| `compartido/estado-reserva/estado-reserva.spec.ts` | Estados de reserva con texto, no solo color | CP-F07 |
| `app.spec.ts` | Enlaces de la barra según el rol | CP-F08a a CP-F08c |
| `paginas/admin/reservas-dia/reservas-dia.spec.ts` | Panel: reservas del día por equipo, filtros, cancelar con motivo, completar, acciones según el estado, 409 y cambio de día | CP-F10a a CP-F10g |
| `paginas/admin/expiradas/expiradas.spec.ts` | Panel: reservas expiradas con pago aprobado | CP-F11a y CP-F11b |
| `paginas/admin/ajuste-puntos/ajuste-puntos.spec.ts` | Panel: buscar al cliente, ajustar puntos, 409 por saldo negativo y validaciones | CP-F12a a CP-F12d |
| `paginas/admin/admin.spec.ts` | Todas las rutas del panel protegidas por `adminGuard` | CP-F13 |
| `core/formato.spec.ts` | Hora y fecha de Bogotá, pesos, singular o plural de puntos y cuenta regresiva | CP-F14a a CP-F14e |
| `paginas/pagar-adelanto/pagar-adelanto.spec.ts` | Pagar el adelanto: aprobado y confirmada, rechazado con reintento, plazo vencido, 403 y 404 al cargar, 409 al pagar, sin selector fuera de la demostración y reserva ya confirmada | CP-F15a a CP-F15g |
| `paginas/mis-puntos/mis-puntos.spec.ts` | Mis puntos: saldo, historial del más reciente al más antiguo con signo, estado vacío y error | CP-F16a a CP-F16c |
| `paginas/cliente/rutas-cliente.spec.ts` | Mis puntos y pagar el adelanto exigen sesión | CP-F17 |

## Calidad de código

El reporte de análisis estático y la forma de repetirlo están en
[docs/calidad/analisis-estatico.md](docs/calidad/analisis-estatico.md).

## Forma de trabajo

Los acuerdos completos están en [ESTANDARES.md](ESTANDARES.md). En resumen:

- **Ramas:** `main` solo recibe código que funciona; `dev` integra el trabajo
  de todos; cada historia de usuario se trabaja en `feat/nombre-corto`, que se
  borra al integrarse.
- **Flujo:** `feat/...` → pull request a `dev` (revisado por el otro
  integrante) → pull request de `dev` a `main`.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/),
  con el formato `tipo(alcance): descripción en imperativo`.
- **Estilo:** Google Java Style aplicado con el formateador de IntelliJ al
  guardar. El código y los comentarios van en español.

## Equipo

- Cristian Farid Amado Peña
- Cristian Santiago Silva Muñoz
