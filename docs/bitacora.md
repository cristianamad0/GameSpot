# Bitácora de desarrollo

Registro de cada tarea terminada: fecha, tarjeta de Jira, rama, qué se hizo,
pruebas y pendientes. La entrada más reciente va al final.

## 2026-10-21 — CACS-13: corregir defectos D-01, D-02 y D-03

- **Rama:** `feat/corregir-defectos`
- **Qué se hizo:**
  - D-03: el correo se normaliza (`trim` y minúsculas con `Locale.ROOT`) al
    registrar, al iniciar sesión y en `UsuarioDetailsService`. El mismo correo
    con otras mayúsculas ya no crea una segunda cuenta.
  - D-01: el secreto JWT se lee de la variable de entorno `JWT_SECRETO`. Las
    pruebas usan un valor propio en `src/test/resources/config/application.yaml`.
    La variable está documentada en el README.
  - D-02: estilo Google aplicado al módulo de autenticación con
    google-java-format 1.28.0, sin cambiar la lógica (commit `style(auth)`
    aparte).
- **Pruebas:** `./mvnw test`: 43 pruebas, 0 fallos, 0 errores. CP-06
  (`noPermiteCorreoRepetidoConOtrasMayusculas`) ya pasa. Se agregaron dos
  pruebas: el correo se guarda normalizado y el inicio de sesión acepta el
  correo con otras mayúsculas.
- **Pendientes:**
  - Generar un secreto JWT nuevo (el anterior quedó en el historial público) y
    definir `JWT_SECRETO` en el entorno de cada integrante.
  - Revisar en Supabase si hay correos repetidos por mayúsculas o guardados sin
    normalizar, y corregirlos antes de desplegar (consultas en la descripción
    del PR).

## 2026-10-27 — Datos reales del negocio

- **Tarjeta:** sin tarjeta propia (datos entregados por el propietario el 8/10/2026).
- **Rama:** `feat/datos-negocio`
- **Qué se hizo:**
  - `configuracion_negocio.valor_adelanto_pesos` pasa a `porcentaje_adelanto`
    (de 1 a 100) en `esquema.sql` y en la entidad `ConfiguracionNegocio`
    (campo `porcentajeAdelanto`).
  - Datos iniciales reales: 6 equipos (`Xbox 360 #1`, `Xbox Series S #1` a
    `#4` y `Sala VIP Xbox Series X`), 7 horarios (lunes a viernes de 14:00 a
    22:00; sábado y domingo de 15:00 a 21:00) y la configuración `(50, 1, 15)`.
  - `db/V2_datos_negocio.sql` aplica el mismo cambio sobre el esquema que ya
    existe en Supabase, dentro de `begin; ... commit;`. Se comprobó en dos
    PostgreSQL 17 temporales: el esquema anterior más V2 queda igual al
    `esquema.sql` nuevo (mismo DDL, mismas restricciones y mismos datos).
- **Pruebas:** `./mvnw clean test`: 57 pruebas, 0 fallos, 0 errores. Nueva
  clase `DatosNegocioTests` (CP-17 a CP-20): equipos, horario de cada día,
  configuración y valores límite del porcentaje (0, 1, 100 y 101).
- **Pendientes:**
  - Ejecutar `V2_datos_negocio.sql` en Supabase el mismo día en que se fusione
    el PR; después, Cristian Amado hace `git pull`.
  - La Tarea 5 (Proveedor 1) no empieza hasta que V2 esté aplicado en Supabase.
