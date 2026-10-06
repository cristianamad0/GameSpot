# GameSpot
Plataforma web de reservas por intervalos para Game Spot, con pago de adelanto y panel de administración

## Base de datos

PostgreSQL en Supabase. El esquema completo está en
`backend/src/main/resources/db/esquema.sql` y Spring Boot solo lo valida
(`ddl-auto: validate`); no crea ni modifica tablas.

**Crear o recrear el esquema en Supabase** (SQL Editor > New query):

1. Si ya existen tablas y todavía no hay datos reales, ejecutar
   `backend/src/main/resources/db/borrar_esquema.sql`.
2. Ejecutar `backend/src/main/resources/db/esquema.sql`.

**Ejecutar el backend:** definir la variable de entorno
`SUPABASE_DB_CONTRASENA` con la contraseña de la base de datos (en IntelliJ:
Run > Edit Configurations > Environment variables). Si la red no tiene IPv6,
definir también `SUPABASE_DB_URL` y `SUPABASE_DB_USUARIO` con los datos del
"Session pooler" (botón Connect del proyecto en Supabase).

**Pruebas:** `./mvnw test` desde `backend/`. Requieren Docker: Testcontainers
levanta un Postgres con el mismo `esquema.sql`.
