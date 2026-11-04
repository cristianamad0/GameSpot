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
| Servicios y endpoints de reservas, pagos, fidelización y administración | Pendientes |

## Tecnologías

- Java 21 y Spring Boot 4 (Spring MVC, Spring Data JPA, Spring Security, Bean Validation)
- JWT (jjwt) con sesiones sin estado
- PostgreSQL en Supabase
- Lombok
- Pruebas: JUnit 5 y Testcontainers (PostgreSQL)

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
