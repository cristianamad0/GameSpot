# Game Spot SOA

Arquitectura orientada a servicios de Game Spot para el proyecto final de
Desarrollo Orientado a Servicios (docente Diana Velásquez). Son tres
aplicaciones Spring Boot independientes, cada una con su propio `pom.xml`,
puerto y base de datos, que se comunican por HTTP con JSON.

| Aplicación | Carpeta | Puerto | Capacidad | Datos que administra |
| --- | --- | --- | --- | --- |
| Proveedor 1 | `proveedor-catalogo` | 8091 | Catálogo: equipos, modalidades con su tarifa por hora y por media hora, y horario de atención | H2 en `proveedor-catalogo/data/catalogo` |
| Proveedor 2 | `proveedor-reservas` | 8092 | Reservas por bloques de 30 minutos y autenticación JWT | H2 en `proveedor-reservas/data/reservas` |
| Consumidora | `consumidor-web` | 8080 | Interfaz web (HTML, CSS, JavaScript con Fetch) y backend que llama a los dos proveedores con RestClient | No guarda datos propios |

```
Navegador (HTML · CSS · JavaScript · Fetch)
        │ HTTP + JSON
        ▼
Consumidora web :8080  (Controller · Service · RestClient)
        │ RestClient · HTTP + JSON           │ RestClient · HTTP + JSON
        ▼                                     ▼
Proveedor catálogo :8091               Proveedor reservas :8092
(Controller · Service · DTO ·          (Controller · Service · DTO ·
 Repository · H2)                       Repository · H2 · emite el JWT)
```

La consumidora nunca escribe en las bases de los proveedores: todo pasa por sus
API. El sistema anterior (`backend/` y `frontend/`) sigue intacto.

## Requisitos

- JDK 21. No hace falta Docker ni Supabase: cada proveedor crea su base H2 en
  disco al arrancar.
- La variable de entorno **`JWT_SECRETO`** (al menos 32 caracteres) **con el
  mismo valor en los dos proveedores**: el de reservas firma los tokens y el
  de catálogo los valida. No se escribe en ningún archivo del repositorio.
  Para generar uno:

  ```bash
  openssl rand -base64 48
  ```

  En IntelliJ: Run > Edit Configurations > Environment variables, en las dos
  configuraciones de los proveedores.

## Cómo ejecutar

1. Arrancar el proveedor de catálogo (puerto 8091), desde `soa/proveedor-catalogo`:

   ```bash
   ./mvnw spring-boot:run
   ```

2. Arrancar el proveedor de reservas (puerto 8092), desde `soa/proveedor-reservas`:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Arrancar la consumidora (puerto 8080), desde `soa/consumidor-web`:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Abrir `http://localhost:8080` en el navegador. La portada muestra los
   equipos, precios y horario del catálogo; el botón "Haz tu reserva" lleva a
   la reserva en tres pasos (consola, día y hora, confirmar).

Fotos del local: copiarlas a `consumidor-web/src/main/resources/static/img/`
y poner el nombre del archivo en la constante `FOTOS` al inicio de
`static/app.js`. Mientras no haya foto, cada modalidad muestra su
ilustración (`static/img/consola-<modalidad>.svg`, dibujos propios). La
decoración animada (cuadrícula, objetos flotantes, nave y personajes) es
original y se queda quieta si el sistema tiene activado "reducir movimiento".

Contenido del local (CACS-32): el catálogo de juegos de cada equipo está en
`static/datos/juegos.json` (tomado de los catálogos de publicidad del local) y
los videos cortos del local en `static/video/`. Para actualizar un catálogo se
edita ese JSON; el nombre de cada equipo debe ser igual al del proveedor de
catálogo.

En IntelliJ, cada configuración de ejecución debe tener como *Working
directory* la carpeta de su proyecto (por ejemplo `soa/proveedor-catalogo`);
si no, la base H2 se crea en otra carpeta.

En PowerShell usar `.\mvnw.cmd` en lugar de `./mvnw`. Las URL de los
proveedores se pueden cambiar con `CATALOGO_URL` y `RESERVAS_URL` al arrancar
la consumidora.

### Cuentas de prueba

Son datos de demostración cargados por `proveedor-reservas/src/main/resources/data.sql`
(las contraseñas se guardan con BCrypt), no credenciales reales.

| Rol | Correo | Contraseña |
| --- | --- | --- |
| `ADMIN` | `admin@gamespot.demo` | `AdminDemo123` |
| `CLIENTE` | `laura@gamespot.demo` | `ClienteDemo123` |
| `CLIENTE` | `andres@gamespot.demo` | `ClienteDemo123` |

## Contrato de la API

### Proveedor 1: catálogo (8091)

| Método | Ruta | JWT | Respuestas |
| --- | --- | --- | --- |
| GET | `/api/equipos` | No | 200 lista de equipos |
| GET | `/api/equipos/{id}` | No | 200 equipo; 404 si no existe |
| POST | `/api/equipos` | ADMIN | 201 equipo creado; 400 datos inválidos; 404 modalidad inexistente; 409 nombre repetido |
| PUT | `/api/equipos/{id}` | ADMIN | 200 equipo actualizado; 400; 404; 409 |
| DELETE | `/api/equipos/{id}` | ADMIN | 204; 404 |
| GET | `/api/modalidades` | No | 200 modalidades con su tarifa por hora y por media hora |
| PUT | `/api/modalidades/{id}` | ADMIN | 200 modalidad; 400 si falta una tarifa o está fuera de 1 a 1.000.000; 404 |
| GET | `/api/horarios` | No | 200 horario de los 7 días |

Sin token o con un token inválido, las rutas con JWT responden 401; con un
token de `CLIENTE`, 403.

### Proveedor 2: reservas (8092)

| Método | Ruta | JWT | Respuestas |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | No | 200 `{token, tipo, expiraEn, nombre, email, rol}`; 400; 401 credenciales incorrectas |
| POST | `/api/reservas` | Sí | 201 reserva en `PENDIENTE_PAGO`; 400 bloque inválido; 409 bloque ocupado |
| GET | `/api/reservas/mias` | Sí | 200 reservas propias, de la más reciente a la más antigua |
| GET | `/api/reservas/{id}` | Sí | 200; 403 si es de otro usuario; 404 |
| GET | `/api/reservas/ocupacion?equipoId=&fecha=` | No | 200 intervalos ocupados del día (sin datos del cliente) |
| PATCH | `/api/reservas/{id}/pago` | Sí | 200 reserva `CONFIRMADA`; 409 si no estaba pendiente |
| PATCH | `/api/reservas/{id}/cancelacion` | Sí | 200 reserva `CANCELADA` (cancelación lógica); 409 si ya estaba cancelada |

### Consumidora (8080)

| Método | Ruta | Proveedores | Respuestas |
| --- | --- | --- | --- |
| GET | `/api/portal/estado` | Los dos | 200 `{catalogo, reservas}` con `DISPONIBLE` o `NO_DISPONIBLE` |
| GET | `/api/portal/equipos` | Catálogo | 200; 503 si el catálogo no responde |
| GET | `/api/portal/horarios` | Catálogo | 200 horario de lunes a domingo; 503 si el catálogo no responde |
| GET | `/api/portal/equipos/{id}/disponibilidad?fecha=` | Los dos | 200 bloques de 30 minutos con su estado |
| POST | `/api/portal/login` | Reservas | 200 token; 401 |
| POST | `/api/portal/reservas` | Los dos | 201; 400 fuera del horario; 401 sin sesión; 409 equipo fuera de servicio o bloque ocupado; 503 |
| GET | `/api/portal/reservas/mias` | Reservas | 200 |
| PATCH | `/api/portal/reservas/{id}/pago` y `/cancelacion` | Reservas | 200; 409 |
| PATCH | `/api/portal/equipos/{id}/estado` | Catálogo (ADMIN) | 200; 401; 403 |

Todas las respuestas de error tienen la misma forma:
`{timestamp, status, mensaje, detalles}`.

## Pruebas

Cada proyecto tiene sus pruebas automatizadas (`./mvnw test` en su carpeta):

| Proyecto | Clase | Casos |
| --- | --- | --- |
| `proveedor-catalogo` | `CatalogoTests`: listado con los datos de `data.sql`, consulta y 404, CRUD completo, validaciones (400 y 409), JWT (401 sin token o inválido, 403 cliente), tarifas, horario y corrección de una base anterior | CP-36a a CP-36h |
| `proveedor-reservas` | `ReservasTests`: login y 401, precio y adelanto, bloques inválidos (400), cruces (409), reservas propias y ocupación pública, pago, cancelación lógica y 403, 401 sin token, precio con tarifa de media hora | CP-37a a CP-37i |
| `consumidor-web` | `PortalTests` y `ClientesHttpTests`: integración de los dos proveedores, validaciones, proveedor detenido (503), página web, horario, rutas y token del RestClient | CP-38a a CP-38j |

La colección de Postman está en `postman/GameSpot-SOA.postman_collection.json`
(importar en Postman y ejecutar las carpetas en orden). Las capturas de la
prueba con las tres aplicaciones están en `evidencias/`.
