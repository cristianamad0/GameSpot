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

## 2026-10-27 — Mantenimiento: cobertura con JaCoCo

- **Tarjeta:** sin tarjeta propia (mantenimiento del repositorio).
- **Rama:** `feat/mantenimiento-calidad`
- **Qué se hizo:**
  - Se agregó jacoco-maven-plugin 0.8.13 al `pom.xml`. `prepare-agent` queda
    en su fase por defecto (`initialize`) para que el agente esté activo antes
    de las pruebas, y `report` corre en la fase `test`. `./mvnw test` genera
    `backend/target/site/jacoco/index.html`.
  - Revisión de ramas remotas: `feat/plan-calidad` y `feat/corregir-defectos`
    ya están integradas en `dev` y se pueden borrar. No se borró ninguna.
- **Pruebas:** `./mvnw clean test`: 43 pruebas, 0 fallos, 0 errores. Cobertura
  de líneas: 60,3 % (instrucciones: 62,2 %). La meta de 70 % o más se trabaja
  en la Tarea 5, con las pruebas de los endpoints.
- **Pendientes:**
  - Decidir si se borran las ramas ya integradas.
