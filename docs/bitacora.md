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

## 2026-10-28 — Cierre de DT-01: estilo Google con Checkstyle

- **Tarjeta:** CACS-13 (DT-01), criterio "Checkstyle reporta 0 advertencias en
  el módulo de autenticación".
- **Rama:** `feat/cerrar-dt01` (PR #12 hacia `dev`)
- **Qué se hizo:**
  - Se agregó maven-checkstyle-plugin 3.6.0 (Checkstyle 10.26.1) al `pom.xml`
    con `google_checks.xml`. `./mvnw checkstyle:check` falla ante cualquier
    advertencia y revisa solo el módulo de autenticación: `BackendApplication`,
    `config`, `controller`, `dto`, `exception`, `security`, `service`,
    `model/Usuario` y `model/Rol` (15 archivos).
  - Antes había 32 advertencias, todas de Javadoc faltante: 19
    `MissingJavadocMethod` y 13 `MissingJavadocType`. Se completó el Javadoc
    en 14 archivos, solo comentarios y sin cambios de lógica. Ahora quedan 0.
- **Pruebas:** `./mvnw test`: 57 pruebas, 0 fallos, 0 errores.
- **Pendientes:**
  - `repository/` tiene 20 advertencias de Javadoc faltante y no entra en la
    verificación. Decidir si se incluye más adelante.
  - Checkstyle guarda caché en `target/checkstyle-cachefile`; para verificar
    desde cero usar `./mvnw clean checkstyle:check`.

## 2026-10-28 — Expiración de reservas no pagadas

- **Tarjeta:** CACS-12 (HU-05).
- **Rama:** `feat/expiracion-reservas`
- **Qué se hizo:**
  - `ExpiracionReservasService.expirarVencidas()` (`@Transactional`) llama a
    `reservaRepository.expirarPendientes()` y registra en el log cuántas
    reservas expiraron (INFO si fue al menos una, DEBUG si fueron 0).
  - `ExpiracionReservasTarea` la ejecuta con `@Scheduled(fixedDelay)`: el
    intervalo se cuenta desde que termina la ejecución anterior, así no se
    solapan. `ProgramacionConfig` habilita `@EnableScheduling`.
  - Propiedades en `application.yaml`: `app.reservas.expiracion.habilitada`
    (`true`) y `app.reservas.expiracion.intervalo-ms` (`60000`). En la
    configuración de pruebas la tarea queda apagada.
  - Se comprobó una vez con la tarea encendida y un intervalo de 200 ms: se
    ejecuta sola en el hilo `scheduling-1`.
- **Pruebas:** `./mvnw test`: 61 pruebas, 0 fallos, 0 errores. Nueva clase
  `ExpiracionReservasTests` (CP-21a a CP-21d): la reserva vencida pasa a
  EXPIRADA y su bloque se puede volver a reservar, la vigente sigue en
  PENDIENTE_PAGO, la CONFIRMADA no expira aunque tenga el plazo vencido, y la
  tarea no existe cuando está apagada. `./mvnw checkstyle:check`: 0
  advertencias.
- **Pendientes:**
  - Con varias instancias del backend la tarea correría en cada una. Hoy hay
    una sola; si se escala, usar un bloqueo (por ejemplo ShedLock).

## 2026-11-04 — Instrucciones de trabajo del repositorio

- **Tarjeta:** sin tarjeta propia (mantenimiento del repositorio).
- **Rama:** `feat/instrucciones-trabajo`
- **Qué se hizo:**
  - Se agregó `INSTRUCCIONES.md` en la raíz: comandos (pruebas, una clase o
    un método, Checkstyle), arquitectura por capas, reglas del esquema y de
    las migraciones, convenciones de nombres y Javadoc, cómo escribir las
    pruebas, y el flujo Jira → rama → commits → PR → bitácora con la lista de
    lo que debe cumplirse antes de dar una tarea por terminada.
  - Complementa `ESTANDARES.md` y el README sin reemplazarlos. Aclara que la
    rama de integración se llama `dev` (en `ESTANDARES.md` aparece como
    `develop`).
- **Pruebas:** solo documentación. `./mvnw -DskipTests compile` y
  `./mvnw checkstyle:check` (0 advertencias) pasan.
- **Pendientes:**
  - `.github/copilot-instructions.md` dice que no hay verificación de estilo
    en Maven, pero ya existe `checkstyle:check`. Decidir si se actualiza o se
    elimina.

## 2026-11-04 — Panel de administración de reservas

- **Tarjeta:** CACS-6 (HU-06).
- **Rama:** `feat/panel-administracion`
- **Qué se hizo:**
  - `AdminReservasService` y `AdminReservasController` bajo
    `/api/admin/reservas`: listar las reservas de un día (hora de Bogotá) con
    filtros opcionales `equipoId` y `estado`, cancelar con motivo, marcar como
    completada y ver las reservas que expiraron con un pago aprobado.
  - Cancelar solo se permite desde `PENDIENTE_PAGO` o `CONFIRMADA` y completar
    solo desde `CONFIRMADA`; cualquier otro estado responde 409
    (`TransicionReservaInvalidaException`). Una reserva inexistente responde
    404 (`ReservaNoEncontradaException`). Sin motivo responde 400 por Bean
    Validation (`CancelacionReservaRequest`).
  - Los puntos al completar los sigue otorgando el trigger
    `otorgar_puntos_al_completar`; el servicio no los calcula.
  - `SecurityConfig` responde 401 cuando no hay token (antes Spring respondía
    403 tanto sin token como sin rol).
  - Nuevas consultas: `ReservaRepository.buscarQueInicianEntre` y
    `PagoRepository.buscarPorEstadoYEstadoDeReserva`, con `join fetch` del
    cliente y el equipo.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 72 pruebas, 0 fallos, 0 errores. Nueva clase
  `AdminReservasTests` (CP-22a a CP-22k), que llama los endpoints con MockMvc y
  tokens JWT reales: 403 para `CLIENTE` y 401 sin token, listado del día con
  filtros, cancelación que libera el bloque, 400 sin motivo, 409 en estados
  finales, puntos otorgados una sola vez, 409 al completar sin estar
  confirmada, casos de expiradas con pago y 404. `./mvnw checkstyle:check`: 0
  advertencias.
- **Pendientes:**
  - La tarjeta no dice qué HU es; se usó HU-06. Confirmar con el equipo.
  - Las pruebas crean las reservas directamente en la base porque CACS-4
    (reservas) y CACS-5 (pagos) siguen abiertas.
  - Completar no exige que la hora de la reserva ya haya pasado; decidir si se
    agrega esa regla.

## 2026-11-04 — Pago del adelanto con proveedor simulado

- **Tarjeta:** CACS-5 (HU-05).
- **Rama:** `feat/anticipos-simulados`
- **Qué se hizo:**
  - La tarjeta pasó de Cristian Amado a Cristian Silva por acuerdo del equipo
    en el Sprint 1 (Amado sigue con CACS-4 y CACS-9).
  - `ProveedorAnticipos`: interfaz del proveedor de pagos, para cambiar después
    el simulado por Wompi sin tocar la lógica. `ProveedorAnticiposSimulado` no
    mueve dinero ni pide datos de tarjeta: responde `APROBADO` o `RECHAZADO`
    según el campo `resultadoSimulado` (por defecto `APROBADO`).
  - `PagoAnticipoService` y `PagoAnticipoController`:
    `POST /api/reservas/{id}/pago`. El monto lo calcula el servidor
    (`adelantoPesos` × 100 en `montoCentavos`) y cada pago tiene una referencia
    única `GS-{reserva}-{aleatorio}`. Quien confirma la reserva es el trigger
    `confirmar_reserva_al_aprobar_pago`; el servicio vuelve a leer la reserva
    para responder su estado.
  - Respuestas: 201 con el pago (aprobado o rechazado); 403 si la reserva es de
    otro usuario (`ReservaAjenaException`); 404 si no existe; 409 si no está
    `PENDIENTE_PAGO` o si su plazo ya venció aunque la tarea de expiración no
    haya pasado todavía (`PlazoPagoVencidoException`).
  - `ReservaRepository.buscarParaPagar` bloquea la reserva (`FOR UPDATE`) para
    que dos pagos simultáneos se atiendan en orden y el segundo responda 409.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 83 pruebas, 0 fallos, 0 errores. Nueva clase
  `PagoAnticipoTests` (CP-23a a CP-23k), con MockMvc y tokens JWT reales:
  pago aprobado que confirma la reserva, aprobado por defecto sin cuerpo,
  rechazado con reintento, 409 al pagar dos veces, la base rechaza un segundo
  aprobado, 409 para `EXPIRADA`, `CANCELADA` y plazo vencido, 403 para una
  reserva ajena, un pago menor al adelanto no confirma, no se guardan datos de
  tarjeta, 401 sin token y 404. `./mvnw checkstyle:check`: 0 advertencias.
- **Pendientes:**
  - La tarjeta no dice qué HU es; se usó HU-05 (la misma de la expiración por
    no pagar). Confirmar con el equipo.
  - Las pruebas crean las reservas directamente en la base porque CACS-4
    (reservas) sigue abierta. Cuando se fusione, agregar una prueba de punta a
    punta: crear la reserva con el endpoint de CACS-4 y pagarla.
  - Cambiar el proveedor simulado por Wompi en entorno de pruebas (llaves en
    variables de entorno y webhook) queda para otra tarjeta.

## 2026-11-04 — Puntos de fidelización

- **Tarjeta:** CACS-7 (HU-07).
- **Rama:** `feat/fidelizacion`
- **Qué se hizo:**
  - La tarjeta (opcional) pasó del backlog al CACS Sprint 1: Cristian Silva
    terminó CACS-6 y CACS-5, y esta solo depende de CACS-6, ya fusionada.
  - `PuntosFidelidadService` y `PuntosFidelidadController`:
    - `GET /api/puntos`: saldo e historial de puntos del usuario autenticado,
      del movimiento más reciente al más antiguo (fecha, puntos, tipo,
      descripción y reserva).
    - `POST /api/admin/usuarios/{usuarioId}/puntos` (solo `ADMIN`): ajuste
      manual positivo o negativo. `AjustePuntosRequest` exige descripción
      (máximo 500 caracteres) y puntos distintos de cero (400 si no).
  - Un ajuste que dejaría el saldo negativo responde 409
    (`SaldoPuntosInsuficienteException`); un usuario inexistente, 404
    (`UsuarioNoEncontradoException`). El servicio bloquea al usuario
    (`FOR UPDATE`) mientras valida el saldo, y la base lo vuelve a impedir con
    el `CHECK` de `usuarios.puntos_fidelidad`.
  - Los puntos por horas jugadas los sigue otorgando el trigger
    `otorgar_puntos_al_completar`, una sola vez por reserva; el servicio no los
    calcula.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 92 pruebas, 0 fallos, 0 errores. Nueva clase
  `PuntosFidelidadTests` (CP-24a a CP-24i), con MockMvc y tokens JWT reales:
  puntos proporcionales al completar (150 minutos = 2 puntos), una sola vez
  por reserva, saldo e historial ordenados, cada cliente ve solo lo suyo,
  401 sin token, ajuste del admin, 400 sin descripción o con cero puntos, 403
  para `CLIENTE`, 404 para un usuario inexistente y 409 si el saldo quedaría
  negativo (dejarlo en 0 sí se permite). `./mvnw checkstyle:check`: 0
  advertencias.
- **Pendientes:**
  - La tarjeta no dice qué HU es; se usó HU-07. Confirmar con el equipo.
  - El historial no está paginado; si crece mucho, agregar paginación como en
    CACS-3.

## 2026-11-04 — Numeración de historias de usuario y criterio de CACS-6

- **Tarjetas:** CACS-5 (HU-03), CACS-6 (HU-06) y CACS-7 (HU-04).
- **Rama:** `feat/numeracion-hu`
- **Qué se hizo:**
  - Se resolvieron los pendientes de CACS-5, CACS-6 y CACS-7 sobre qué HU
    implementan. Se toma la numeración que ya usaba `ReglasBaseDatosTests`:
    HU-02 reservas por bloques, HU-03 pago de adelanto y HU-04 fidelización;
    HU-05 sigue siendo la expiración (CACS-12) y HU-06 el panel de
    administración (CACS-6), que no chocan con ninguna.
  - CACS-5 pasa de HU-05 a HU-03 y CACS-7 de HU-07 a HU-04 en el Javadoc, las
    pruebas y el README. Las entradas anteriores de esta bitácora se dejan como
    se escribieron; esta entrada las corrige.
  - CACS-6, criterio 4: cancelar sin motivo responde 400 (dato de entrada
    inválido, igual que el resto de validaciones de la API) y cancelar una
    reserva en estado final responde 409. Se ajustó la descripción de la
    tarjeta en Jira para que coincida con lo implementado; el código no cambia.
- **Pruebas:** `./mvnw test`: 92 pruebas, 0 fallos, 0 errores (solo cambian
  comentarios). `./mvnw checkstyle:check`: 0 advertencias.
- **Pendientes:**
  - Ninguno de los tres puntos queda abierto.

## 2026-11-04 — Errores 403 y 400 que llegaban como 401

- **Tarjeta:** CACS-14 (defecto encontrado al probar la API a mano).
- **Rama:** `feat/respuestas-de-error`
- **Qué se hizo:**
  - Al correr la API con un servidor real, un `CLIENTE` con token recibía 401
    en `/api/admin/**` (debía ser 403) y un parámetro o cuerpo inválido también
    daba 401 (debía ser 400). Spring reenvía esos errores a `/error` sin el
    token, y `SecurityConfig` exigía autenticación en esa ruta.
  - `SecurityConfig` permite `/error`. Solo devuelve la descripción del error
    (código, mensaje genérico y ruta), no datos de las rutas protegidas.
  - Las pruebas con MockMvc no lo detectaban porque MockMvc no hace el reenvío
    a `/error`. Se agregó `RespuestasErrorServidorTests`, que levanta el
    servidor en un puerto aleatorio.
  - README: cómo probar la API a mano con `./mvnw spring-boot:test-run` y un
    PostgreSQL temporal, sin tocar Supabase.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 95 pruebas, 0 fallos, 0 errores. Nueva clase
  `RespuestasErrorServidorTests` (CP-25a a CP-25c): 403 para `CLIENTE` sin
  exponer datos, 400 para fecha y enum inválidos, y 401 sin token. Antes de la
  corrección, CP-25a y CP-25b fallaban con 401. `./mvnw checkstyle:check`: 0
  advertencias.
- **Pendientes:**
  - Ninguno.

## 2026-11-04 — Módulo de reservas del cliente

- **Tarjeta:** CACS-4 (HU-02).
- **Rama:** `feat/modulo-reservas`
- **Qué se hizo:**
  - `ReservaService` y `ReservaController`: `POST /api/reservas` crea una
    reserva en `PENDIENTE_PAGO` y `GET /api/reservas/disponibilidad` muestra los
    bloques de 30 minutos de un equipo en un día. Ambos exigen token.
  - El precio sale de la tarifa por hora de la modalidad y el adelanto del
    `porcentajeAdelanto`; los dos se redondean hacia arriba al peso. El plazo de
    pago (`expiraEn`) es la hora de creación más `minutosEsperaPago`.
  - El servicio revisa antes de guardar: bloque en :00 o :30, duración múltiplo
    de 30, que no haya empezado, que quepa en el horario del día (hora de
    Bogotá), que el equipo exista y esté en servicio, y que no se cruce con otra
    reserva activa. Para que la revisión de cruces sirva con peticiones
    simultáneas, el equipo se bloquea (`SELECT ... FOR UPDATE`) mientras se crea
    la reserva; la restricción `reservas_sin_cruces` queda como respaldo y su
    violación también se responde con 409.
  - Corrección encontrada por las pruebas de CACS-9: sin el bloqueo, varias
    inserciones simultáneas chocaban en la restricción de exclusión, PostgreSQL
    abortaba algunas con un deadlock y el cliente recibía 500 en vez de 409.
  - En la disponibilidad ocupan el bloque las reservas `PENDIENTE_PAGO`,
    `CONFIRMADA` y `COMPLETADA`; un equipo fuera de servicio no tiene bloques
    libres.
  - Excepciones nuevas: `ReservaInvalidaException` (400),
    `BloqueOcupadoException` (409) y `EquipoNoEncontradoException` (404).
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 111 pruebas, 0 fallos, 0 errores. Nueva clase
  `ReservasTests` (CP-26a a CP-26p), con al menos un caso por criterio de
  aceptación y una prueba de punta a punta que crea la reserva con el endpoint
  y la paga (pendiente que dejó CACS-5). `./mvnw clean checkstyle:check`: 0
  advertencias.
- **Pendientes:**
  - Una reserva de un equipo fuera de servicio responde 400; confirmar con el
    equipo si debería ser 409.
  - Las pruebas con peticiones realmente simultáneas están en CACS-9.

## 2026-11-04 — Pruebas de reservas y pagos simultáneos

- **Tarjeta:** CACS-9 (objetivo 1: cero dobles reservas).
- **Rama:** `feat/pruebas-simultaneas` (creada desde `feat/modulo-reservas`,
  porque depende de CACS-4).
- **Qué se hizo:**
  - `PruebasSimultaneasTests` levanta el servidor en un puerto aleatorio y
    lanza peticiones HTTP reales a la vez (todas esperan a estar listas y
    salen juntas) contra el PostgreSQL de Testcontainers.
  - No usa `@Transactional`, porque cada petición corre en su propio hilo:
    crea usuarios con correos únicos, usa fechas de 2099 que no usa ninguna
    otra clase y borra sus usuarios, reservas y pagos al terminar cada prueba.
  - La primera corrida encontró un defecto en CACS-4: con inserciones
    simultáneas, PostgreSQL abortaba algunas con un deadlock en la restricción
    `reservas_sin_cruces` y el cliente recibía 500. Se corrigió en la rama de
    CACS-4 (PR #21) bloqueando el equipo mientras se crea la reserva. Sin esa
    corrección, esta clase falla.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 132 pruebas, 0 fallos, 0 errores. Nueva clase
  `PruebasSimultaneasTests` (CP-27a a CP-27e): CP-27a a CP-27d se repiten 5
  veces cada una y CP-27e corre 16 carreras de 10 reservas seguidas. La clase
  se corrió 4 veces más (84 ejecuciones) sin fallos intermitentes.
  `./mvnw clean checkstyle:check`: 0 advertencias.
- **Pendientes:**
  - Este PR se fusiona después del #21 (CACS-4).
  - El pool de conexiones es de 5 (`maximum-pool-size`); con más tráfico real,
    las peticiones por un mismo equipo esperan su turno. Revisarlo si se
    despliega con más usuarios.

## 2026-11-04 — Detalles del cierre del Sprint 1

- **Tarjeta:** CACS-20 (mantenimiento).
- **Rama:** `feat/detalles-cierre-sprint1`
- **Qué se hizo:**
  - Reservar un equipo fuera de servicio pasa de 400 a **409** con
    `EquipoFueraDeServicioException`. Sigue la regla acordada en CACS-6: 400
    cuando los datos enviados son inválidos y 409 cuando el estado del recurso
    no permite la acción. Resuelve el pendiente de la entrada de CACS-4.
  - `DatosNegocioTests` tenía dos pruebas con el código CP-18; pasan a CP-18a
    y CP-18b. Ya no se repite ningún código de caso de prueba.
  - El pool de conexiones (5, pendiente de la entrada de CACS-9) no se cambia:
    es una nota para cuando se despliegue con más usuarios, no un defecto.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 132 pruebas, 0 fallos, 0 errores. CP-26n ahora
  espera 409 y el mensaje "El equipo … está fuera de servicio".
  `./mvnw checkstyle:check`: 0 advertencias.
- **Pendientes:**
  - Revisar el tamaño del pool de conexiones antes de un despliegue real.

## 2026-11-04 — Frontend: base Angular, identidad visual e inicio de sesión

- **Tarjeta:** CACS-15 (HU-01).
- **Rama:** `feat/frontend-base`
- **Qué se hizo:**
  - Proyecto Angular 22 en `frontend/` (standalone, signals, sin zone.js,
    SCSS y Vitest), creado con `ng new`.
  - Identidad visual en `src/styles.scss`: tema oscuro con acentos neón morado
    y cian, Orbitron para títulos e Inter para texto, contraste AA, foco
    visible, botones grandes y respeto a `prefers-reduced-motion`.
  - Sesión con JWT (`core/auth/sesion.ts`) guardada en `localStorage`, que se
    cierra sola si el token venció. `tokenInterceptor` envía el token solo a la
    API y, ante un 401, cierra la sesión y lleva al ingreso; los 401 del login
    se muestran como credenciales incorrectas. `sesionGuard` y `adminGuard`
    protegen las rutas; un `CLIENTE` no entra a `/admin`.
  - Pantallas: inicio (equipos reales y horario), registro, ingreso, 404 y
    "Próximamente" para reservar (CACS-19), mis puntos (CACS-17) y el panel
    (CACS-18). Barra de navegación según el rol, con menú para celular.
  - `mensajeDeError` muestra el `mensaje` de `ApiError` en español.
    `EstadoReservaEtiqueta` muestra el estado de una reserva con texto y color,
    lista para las pantallas de las otras tarjetas.
  - Probado contra la API real (`spring-boot:test-run`): login incorrecto,
    ingreso de cliente y de admin, `/admin` bloqueado para el cliente, salir,
    y la vista en 375 px. Se corrigió el mensaje de "repite la contraseña"
    cuando el campo está vacío.
- **Pruebas:** `npx ng test --watch=false`: 30 pruebas, 0 fallos (CP-F01a a
  CP-F08c, al menos una por criterio). `npx ng build` sin errores. Prettier sin
  advertencias. El backend no cambia.
- **Pendientes:**
  - Las pruebas del frontend no corren en `./mvnw test`; se corren aparte.

## 2026-11-04 — Frontend: panel del administrador

- **Tarjeta:** CACS-18 (HU-06 y HU-04).
- **Rama:** `feat/panel-admin-frontend`
- **Qué se hizo:**
  - Backend: `GET /api/admin/usuarios?email=` (`AdminUsuariosController` y
    `AdminUsuariosService`) busca hasta 10 usuarios por parte del correo, sin
    distinguir mayúsculas, y responde `UsuarioResumenResponse` (sin datos
    sensibles). Menos de 3 caracteres responde 400; solo `ADMIN`.
  - Frontend, `/admin` con tres pestañas protegidas por `adminGuard`:
    - Reservas del día: agenda por equipo en hora de Bogotá, filtros por día,
      equipo y estado; cancelar con motivo obligatorio y completar, solo en los
      estados que lo permiten. La tarjeta se actualiza con la respuesta del
      backend, sin recargar, y un 409 muestra su mensaje sin cambiar nada.
    - Expiradas con pago: reservas vencidas con pago aprobado, con monto,
      fecha y referencia.
    - Ajustar puntos: buscar al cliente por correo, ver su saldo y registrar
      un ajuste con descripción; el saldo negativo (409) muestra el mensaje.
  - `core/formato.ts`: hora y fecha de Bogotá, pesos colombianos y "punto"
    o "puntos" según la cantidad. Botón `gs-boton--peligro` para cancelar.
  - Probado contra la API real con reservas en todos los estados: completar,
    cancelar con motivo, expiradas, ajuste de -5 sobre 1 punto (409) y de +10.
    Se corrigieron dos detalles vistos al probar: las pestañas con barra de
    desplazamiento en celular y el texto "1 puntos".
- **Pruebas:** `./mvnw test`: 137 pruebas, 0 fallos, 0 errores (nueva
  `AdminUsuariosTests`, CP-28a a CP-28e). `./mvnw checkstyle:check`: 0
  advertencias. `npx ng test --watch=false`: 48 pruebas, 0 fallos (nuevas
  CP-F10a a CP-F14d). `npx ng build` y Prettier sin errores.
- **Pendientes:**
  - El filtro por equipo usa los equipos que tienen reservas ese día; si se
    quiere ver todos los equipos, hace falta un endpoint de equipos.

## 2026-11-05 — Datos de demostración para probar en local

- **Tarjeta:** CACS-16.
- **Rama:** `feat/datos-demostracion`
- **Qué se hizo:**
  - `./mvnw spring-boot:test-run` activa ahora el perfil `demo`
    (`TestBackendApplication` y `src/test/resources/config/application-demo.yaml`).
    El perfil enciende `app.demo.datos`, apaga el registro por consola (ya no
    hace falta pasar `--app.cli.registration.enabled=false`) y deja correr la
    expiración de reservas como en el uso real.
  - `demo/DatosDemostracion` (en el código de pruebas, así que nunca llega al
    jar ni corre contra Supabase) crea al arrancar un `ADMIN`, dos clientes,
    reservas en los cinco estados entre ayer y mañana, pagos simulados
    (aprobados, uno rechazado y uno aprobado tarde sobre una reserva expirada) y
    bonos de puntos. Los estados `CONFIRMADA` y `COMPLETADA` y los puntos por
    horas jugadas los ponen los triggers de la base, como en el uso real. Si
    los datos ya existen, no los duplica.
  - Las credenciales son valores de prueba documentados en el README; las
    contraseñas se guardan con BCrypt.
  - Se probó a mano con la API en marcha: inicio de sesión del admin y de un
    cliente, saldo de puntos, panel del día y expiradas con pago.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 143 pruebas, 0 fallos, 0 errores. Nueva clase
  `DatosDemostracionTests` (CP-29a a CP-29f): con el perfil `demo` se inicia
  sesión, hay reservas en cada estado, puntos y una expirada con pago, no se
  duplican y las contraseñas van cifradas; sin el perfil no existe el cargador
  ni los usuarios. `./mvnw clean checkstyle:check`: 0 advertencias.
- **Pendientes:**
  - La reserva pendiente de demostración tiene plazo de pago hasta su hora de
    inicio (mañana), no 15 minutos, para que no expire durante una
    demostración.

## 2026-11-05 — Frontend: pagar el adelanto y mis puntos

- **Tarjeta:** CACS-17 (HU-03 y HU-04).
- **Rama:** `feat/pantallas-cliente`
- **Qué se hizo:**
  - Backend: `GET /api/reservas/{id}` (`ReservaService.obtenerPropia`) para que
    el cliente lea su reserva antes de pagar; 403 si es de otro usuario y 404
    si no existe. No había forma de mostrar el monto y la fecha límite al
    recargar la página.
  - Frontend, `/reservas/:id/pagar` (`PagarAdelanto`): equipo, horario,
    precio, adelanto destacado y lo que se paga en el local; cuenta regresiva
    del plazo (`role="timer"`, sin anuncios cada segundo); pago aprobado con la
    reserva `CONFIRMADA` y su referencia; pago rechazado con el motivo y
    reintento; plazo vencido sin botón de pago; errores 403, 404 y 409 con el
    mensaje del backend.
  - El selector "aprobar / rechazar" del proveedor simulado solo aparece con
    `ng serve` (`PROVEEDOR_SIMULADO`, que vale `isDevMode()`); en un build de
    producción el pago va sin resultado y el simulado lo aprueba.
  - Frontend, `/mis-puntos` (`MisPuntos`): saldo destacado e historial del más
    reciente al más antiguo, con tipo, descripción, fecha y el signo en el
    texto (no solo el color); estado vacío amigable.
  - `ClienteApi` agrupa las llamadas del cliente. Las rutas exigen sesión.
  - Probado a mano con `spring-boot:test-run` y `ng serve` como Laura: pago
    rechazado, reintento aprobado, mis puntos, y las dos pantallas a 360 px sin
    desplazamiento horizontal. Contraste de los colores usados entre 5,38:1 y
    15,23:1 (AA). Capturas en `docs/capturas/cacs-17/`.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 146 pruebas, 0 fallos, 0 errores (CP-30a a
  CP-30c en `ReservasTests`). `./mvnw clean checkstyle:check`: 0
  advertencias. `npx ng test --watch=false`: 61 pruebas, 0 fallos (CP-F14e,
  CP-F15a a CP-F15g, CP-F16a a CP-F16c y CP-F17). `npx ng build` sin errores.
  `npx prettier --check --end-of-line auto "src/**/*.{ts,html,scss}"` sin
  cambios.
- **Pendientes:**
  - El cliente todavía no tiene una lista de sus reservas: llega a pagar desde
    la pantalla de reservar (CACS-19) o con el enlace. La lista es el
    historial de CACS-3.
  - En Windows con `core.autocrlf=true`, `prettier --check` sin
    `--end-of-line auto` marca todos los archivos por los finales de línea
    CRLF del checkout; en el repositorio están en LF.

## 2026-11-06 — Frontend: reservar un equipo

- **Tarjeta:** CACS-19 (HU-02).
- **Rama:** `feat/reservar-frontend` (creada desde `feat/pantallas-cliente`,
  porque al reservar se pasa a la pantalla de pago de CACS-17).
- **Qué se hizo:**
  - Backend: `GET /api/catalogo` (`CatalogoController`, `CatalogoService`,
    `CatalogoResponse`) con los equipos, su modalidad y tarifa, si están
    disponibles, el porcentaje de adelanto y los minutos para pagar. Ningún
    endpoint del cliente exponía la tarifa ni el porcentaje, que la pantalla
    necesita para mostrar el precio y el adelanto antes de confirmar. El valor
    que se cobra lo sigue calculando `ReservaService`.
  - Frontend, `/reservar` (`Reservar`): catálogo en tarjetas con ícono por
    modalidad (consola o sala) y tarifa; los equipos fuera de servicio no se
    eligen. Grilla de bloques de 30 minutos del día elegido con cuatro estados
    con color y texto (libre, ocupado, ya pasó y elegido). Solo se eligen
    bloques libres y duraciones de bloques libres seguidos. El resumen muestra
    equipo, horario, precio, adelanto y el plazo de pago. Al reservar pasa a
    `/reservas/:id/pagar`; un 409 muestra el mensaje y vuelve a cargar la grilla.
  - Teclado: los bloques son botones con nombre ("4:00 p. m. a 4:30 p. m.,
    libre"), `aria-pressed` para los elegidos y foco visible.
  - "Reservar" deja de ser "Próximamente"; el componente `Proximamente` ya no
    lo usa ninguna ruta.
  - Probado a mano con los datos de demostración como Andrés: catálogo, la
    grilla de la Sala VIP con su reserva de hoy ocupada, un 409 real (Laura
    tomó el bloque por la API mientras Andrés lo tenía elegido), elegir con Tab
    y Enter, crear la reserva y pasar al pago; a 360 px sin desplazamiento
    horizontal. Contraste de los colores nuevos entre 4,78:1 y 13,01:1 (AA).
    Capturas en `docs/capturas/cacs-19/`.
  - La prueba a mano encontró que el aviso del 409 seguía visible al elegir
    otro bloque; ahora se borra (CP-F18f lo cubre).
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 149 pruebas, 0 fallos, 0 errores (CP-31a a
  CP-31c en `CatalogoTests`). `./mvnw clean checkstyle:check`: 0
  advertencias. `npx ng test --watch=false`: 71 pruebas, 0 fallos (CP-F18a a
  CP-F18j). `npx ng build` sin errores. `npx prettier --check --end-of-line
  auto "src/**/*.{ts,html,scss}"` sin cambios.
- **Pendientes:**
  - Este PR se fusiona después del #28 (CACS-17).
  - Decidir si se borra `paginas/proximamente`, que ya no usa ninguna ruta.

## 2026-11-09 — Mantenimiento: frontend en Windows, Prettier y tabla de pruebas

- **Tarjeta:** CACS-23.
- **Rama:** `feat/mantenimiento-windows`
- **Qué se hizo:**
  - `frontend/angular.json`: `cli.analytics: false`. Angular ya no pregunta
    por los datos de uso, pregunta que dejaba `ng serve` esperando.
  - `frontend/.prettierrc`: `endOfLine: "auto"`. Con `core.autocrlf=true`, Git
    deja los archivos en CRLF en Windows y `prettier --check` los marcaba
    todos; ahora pasa sin opciones extra.
  - README: en PowerShell, si `npm.ps1` está bloqueado, se usan `npm.cmd` y
    `npx.cmd` (`npm.cmd start`). Se agrega `BackendApplicationTests` a la
    tabla de pruebas del backend.
  - Probado a mano en Windows, sin `NG_CLI_ANALYTICS`: `npm.cmd start` arranca
    sin preguntar nada y `http://localhost:4200` responde 200.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 149 pruebas, 0 fallos, 0 errores.
  `./mvnw checkstyle:check`: 0 advertencias. `npx ng test --watch=false`: 71
  pruebas, 0 fallos. `npx ng build` sin errores. `npx prettier --check
  "src/**/*.{ts,html,scss}"` sin cambios y sin opciones extra.
- **Pendientes:**
  - Ninguno de esta tarjeta.

## 2026-11-09 — Pago del adelanto con Wompi (sin llaves todavía)

- **Tarjeta:** CACS-21 (HU-03).
- **Rama:** `feat/pago-wompi`
- **Qué se hizo:**
  - Cambio de alcance acordado por el equipo: Wompi solo muestra las llaves de
    Sandbox después de activar el comercio con RUT y NIT, y se decidió no
    activarlo por ahora. La integración se construyó con la API documentada de
    Wompi y se prueba contra un Wompi simulado; la prueba manual en el Sandbox
    real queda pendiente de las llaves.
  - `config/ProveedorPagos`, `PropiedadesPagos` y `PagosConfig`:
    `app.pagos.proveedor` elige entre `simulado` (por defecto) y `wompi`. Con
    `wompi` la API no arranca sin `WOMPI_LLAVE_PUBLICA`,
    `WOMPI_SECRETO_EVENTOS` y `WOMPI_SECRETO_INTEGRIDAD`. No hace falta la
    llave privada.
  - `service/PagoWompiService` y `controller/PagoWompiController`:
    `POST /api/reservas/{id}/pago/wompi` crea el pago `PENDIENTE` y devuelve
    la URL del Web Checkout con monto y referencia firmados (firma de
    integridad SHA-256); `POST /api/pagos/wompi/eventos` es el webhook,
    público y protegido con el checksum del evento (401 si no coincide);
    `GET /api/reservas/{id}/pago/wompi/{transacción}` consulta la transacción
    en Wompi al volver del checkout, para registrar el pago aunque el webhook
    no llegue; `GET /api/pagos/proveedor` le dice al frontend qué flujo usar.
  - `service/RegistroResultadoPago`: único punto donde se guarda el resultado
    de un cobro (simulado, webhook o consulta). Es idempotente: un pago
    APROBADO no vuelve a cambiar. Ahí debe crear CACS-8 la notificación de
    confirmación.
  - Si el monto o la moneda del evento no coinciden con el pago, queda en
    `ERROR` y la reserva no se confirma. Con Wompi activo, el pago simulado
    responde 409.
  - Frontend, `/reservas/:id/pagar`: con Wompi, "Pagar con Wompi" lleva al
    checkout; al volver con `?id=` consulta el resultado y muestra aprobado,
    rechazado, error o "todavía procesando" con "Consultar de nuevo". Con el
    simulado todo sigue igual.
  - Probado a mano con los datos de demostración: con el proveedor simulado
    el pago de Laura se aprueba y confirma la reserva, el checkout de Wompi
    responde 409 y el webhook con firma falsa 401.
  - Sin cambios de esquema: la tabla `pagos` ya tenía las columnas de Wompi.
- **Pruebas:** `./mvnw test`: 161 pruebas, 0 fallos, 0 errores (CP-32a a
  CP-32l en `PagoWompiTests`). `./mvnw checkstyle:check`: 0 advertencias.
  `npx ng test --watch=false`: 75 pruebas, 0 fallos (CP-F19a a CP-F19d).
  `npx ng build` sin errores. `npx prettier --check` sin cambios.
- **Pendientes:**
  - Prueba manual en el Sandbox de Wompi cuando haya llaves (activar el
    comercio), con las tarjetas de prueba 4242 4242 4242 4242 (aprobada) y
    4111 1111 1111 1111 (rechazada), y configurar la URL de eventos con un
    túnel si se quiere probar el webhook en local.
  - CACS-8 debe crear las notificaciones en `RegistroResultadoPago`.

## 2026-11-09 — Panel: equipos y tarifas

- **Tarjeta:** CACS-22 (HU-06).
- **Rama:** `feat/gestion-equipos`
- **Qué se hizo:**
  - Backend, solo `ADMIN` (`AdminEquiposController`, `AdminEquiposService`):
    `GET /api/admin/equipos` (modalidad, tarifa, estado y reservas
    `PENDIENTE_PAGO` o `CONFIRMADA` que aún no empiezan),
    `PUT /api/admin/equipos/{id}/disponibilidad`, `GET /api/admin/modalidades`
    y `PUT /api/admin/modalidades/{id}` (tarifa entre 1 y 1.000.000, y si está
    activa). El tope lo sugirió Cristian Silva en la revisión, para que un error
    al digitar no deje un precio desbordado; la pantalla lo valida también.
    Modalidad inexistente responde 404 (`ModalidadNoEncontradaException`).
  - Las reservas existentes no cambian: guardan su precio y siguen activas aunque
    el equipo salga de servicio. Las reservas nuevas de un equipo fuera de
    servicio o de una modalidad desactivada ya respondían 409 (CACS-20).
  - Frontend, pestaña "Equipos y tarifas" del panel (`Equipos`): estado en
    texto ("Disponible", "Fuera de servicio", "Modalidad desactivada"); si el
    equipo tiene reservas futuras pide confirmación y avisa que siguen activas;
    cada modalidad se edita con su tarifa y una casilla para ofrecerla.
  - La prueba a mano encontró un error de concordancia con una sola reserva
    ("Sus 1 reserva futura siguen activas"); ahora dice "Su reserva futura
    sigue activa" (CP-F20h).
  - Probado a mano con los datos de demostración como admin: fuera de servicio
    con confirmación, habilitar, tarifa del Xbox 360 a $8.000 y la reserva
    pendiente de Laura en ese equipo conserva sus $5.000; a 360 px sin
    desplazamiento horizontal. Capturas en `docs/capturas/cacs-22/`.
  - Sin cambios de esquema: no hay migración pendiente.
- **Pruebas:** `./mvnw test`: 169 pruebas, 0 fallos, 0 errores, ya con el
  código de CACS-21 traído de `dev` (CP-33a a CP-33h en `AdminEquiposTests`). `./mvnw clean checkstyle:check`: 0
  advertencias. `npx ng test --watch=false`: 83 pruebas, 0 fallos (CP-F20a a
  CP-F20h). `npx ng build` sin errores. `npx prettier --check
  "src/**/*.{ts,html,scss}"` sin cambios.
- **Pendientes:**
  - Cambiar las tarifas en Supabase desde el panel cambia los precios reales
    de las reservas nuevas: usarlo con cuidado una vez desplegado.

## 2026-11-09 — Historial de reservas y pantalla "Mis reservas"

- **Tarjeta:** CACS-3 (HU-07).
- **Rama:** `feat/historial-reservas`
- **Qué se hizo:**
  - `GET /api/reservas/mias?estado=&pagina=&tamano=` (`ReservaController`,
    `ReservaService.historialPropio`, `HistorialReservasResponse`): solo las
    reservas del usuario autenticado, de la más reciente a la más antigua,
    filtro opcional por estado y 10 por página por defecto (máximo 50).
    Página negativa, tamaño fuera de 1 a 50 o un estado que no existe
    responden 400. Usa el índice `reservas_historial_usuario_idx`.
  - `ReservaRepository`: consultas paginadas con el equipo ya cargado
    (`@EntityGraph`), para no hacer una consulta por reserva.
  - Frontend, `/mis-reservas` (`MisReservas`): tarjetas con equipo, horario,
    precio, adelanto y estado en texto; filtro por estado; "Anterior" y
    "Siguiente"; las reservas pendientes llevan a "Pagar el adelanto"; sin
    reservas, mensaje y enlace para reservar. Enlace "Mis reservas" en la
    barra del cliente.
  - Probado a mano con los datos de demostración como Laura: sus 4 reservas
    en orden, filtro por Confirmadas y por Expiradas (sin resultados, con su
    mensaje), enlace a pagar la pendiente y 360 px sin desplazamiento
    horizontal. Capturas en `docs/capturas/cacs-3/`.
  - Se trajo `dev` con CACS-22 (PR #32) y se resolvió el conflicto del README
    dejando las filas de las dos tarjetas.
  - Sin cambios de esquema.
- **Pruebas:** `./mvnw test`: 175 pruebas, 0 fallos, 0 errores (CP-34a a
  CP-34f en `HistorialReservasTests`). `./mvnw checkstyle:check`: 0
  advertencias. `npx ng test --watch=false`: 90 pruebas, 0 fallos (CP-F21a a
  CP-F21g; CP-F08b ahora incluye "Mis reservas"). `npx ng build` y
  `prettier --check` sin errores.
- **Pendientes:**
  - Ninguno de esta tarjeta.

## 2026-11-10 — Notificaciones por correo

- **Tarjeta:** CACS-8 (HU-08).
- **Rama:** `feat/notificaciones-correo`
- **Qué se hizo:**
  - `NotificacionService`: al confirmarse una reserva programa la
    `CONFIRMACION` para enviarse de inmediato y el `RECORDATORIO` una hora
    antes del inicio; si falta menos de una hora, solo la confirmación. No
    duplica si ya existe la confirmación.
  - Las notificaciones se crean en `RegistroResultadoPago.registrar(...)`, el
    punto único que se acordó con Cristian Silva en CACS-21: después de
    guardar un pago APROBADO se vuelve a leer la reserva y, si quedó
    `CONFIRMADA`, se programan. Sirve igual para el pago simulado, el webhook de
    Wompi y la consulta de la transacción. Si la reserva ya había expirado, no
    se programa nada.
  - `NotificacionesTarea` (`@Scheduled`, cada `app.notificaciones.envio.intervalo-ms`)
    envía las que ya deben salir, en lotes de 50. Si un envío falla, suma un
    intento, guarda el error y se reintenta en la siguiente ejecución. La
    consulta (`buscarPorEnviar`) no trae recordatorios de reservas que ya no
    están `CONFIRMADA`, así que el de una reserva cancelada nunca sale.
  - `EnviadorCorreoSmtp` (Gmail por SMTP, `spring-boot-starter-mail`, con
    timeouts de 10 s). Credenciales solo por `CORREO_USUARIO` y
    `CORREO_CONTRASENA_APLICACION`; el envío se enciende con
    `APP_NOTIFICACIONES_HABILITADAS=true` y está apagado en las pruebas.
  - `PruebasSimultaneasTests` borra también las notificaciones que crean sus
    pagos aprobados.
  - Correo real probado a mano con `spring-boot:test-run` y una contraseña de
    aplicación de la cuenta del equipo: se registró un cliente con esa cuenta,
    se reservó y se pagó con el proveedor simulado, y la tarea registró
    "Notificaciones enviadas: 1 de 1" a los 25 segundos. Se comprobó que
    ninguna credencial quedó en archivos ni en el historial.
  - Sin cambios de esquema: la tabla `notificaciones` ya existía.
- **Pruebas:** `./mvnw test`: 184 pruebas, 0 fallos, 0 errores, ya con CACS-3
  traído de `dev`. Nueva clase `NotificacionesTests` (CP-35a a CP-35i; se
  renumeraron porque CACS-3 usa CP-34), con el envío de correo simulado.
  `./mvnw clean checkstyle:check`: 0 advertencias.
- **Pendientes:**
  - Para producción, definir las tres variables de entorno en el servidor.
  - No hay un tope de reintentos: una dirección que siempre falla se reintenta
    cada minuto. Si pasa, agregar un máximo de intentos.
  - En PowerShell, guardar la contraseña de aplicación con comillas simples:
    con comillas dobles, un `$` se interpreta como variable.

## 2026-11-10 — SOA: proveedor de catálogo

- **Tarjeta:** CACS-25 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-catalogo`
- **Qué se hizo:**
  - Decisión del equipo: para la arquitectura SOA que pide la docente Diana
    Velásquez, el negocio se divide en catálogo (proveedor 1) y reservas
    (proveedor 2), cada uno con su base H2 en archivo, y los tres proyectos
    van en `soa/` sin tocar el sistema actual.
  - `soa/proveedor-catalogo`: aplicación Spring Boot independiente en el
    puerto 8091 con `schema.sql` (DDL), `data.sql` (DML con los 6 equipos, 3
    modalidades y el horario reales; solo inserta lo que falta, para no pisar
    cambios al reiniciar), entidades JPA y repositorios.
  - CRUD de equipos (`GET`, `POST`, `PUT`, `DELETE /api/equipos`), tarifas
    (`GET` y `PUT /api/modalidades`) y horario (`GET /api/horarios`). Las
    modificaciones exigen un JWT de rol ADMIN emitido por el proveedor de
    reservas y firmado con el mismo `JWT_SECRETO` (401 sin token o con uno
    inválido, 403 con otro rol).
- **Pruebas:** `./mvnw test` en `soa/proveedor-catalogo`: 7 pruebas, 0 fallos
  (CP-36a a CP-36g en `CatalogoTests`).
- **Pendientes:**
  - Ninguno de esta tarjeta.

## 2026-11-10 — SOA: proveedor de reservas con JWT

- **Tarjeta:** CACS-26 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-reservas`
- **Qué se hizo:**
  - `soa/proveedor-reservas`: aplicación Spring Boot independiente en el
    puerto 8092 con H2 en archivo (`./data/reservas`), `schema.sql` (DDL),
    `data.sql` (DML con tres usuarios de prueba y contraseñas en BCrypt),
    entidades JPA y repositorios.
  - `POST /api/auth/login` emite un JWT firmado con `JWT_SECRETO` (rol en el
    token). `FiltroJwt` exige el token en todo `/api/reservas` salvo la
    ocupación, que es pública.
  - Reservas: crear (bloques en :00 o :30, múltiplos de 30, en el futuro, sin
    cruces con reservas activas del mismo equipo, 409 si se cruzan), mis
    reservas, ocupación de un día, pago simulado del adelanto y cancelación
    lógica (la reserva queda en CANCELADA y su bloque se libera).
- **Pruebas:** `./mvnw test` en `soa/proveedor-reservas`: 8 pruebas, 0
  fallos (CP-37a a CP-37h en `ReservasTests`).
- **Pendientes:**
  - La detección de cruces se hace en el servicio; con H2 no hay restricción
    de exclusión como en PostgreSQL.

## 2026-11-10 — SOA: aplicación consumidora web

- **Tarjeta:** CACS-27 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-consumidor`
- **Qué se hizo:**
  - `soa/consumidor-web`: aplicación Spring Boot independiente en el puerto
    8080. Sirve una página en HTML, CSS y JavaScript (`static/`) que llama con
    Fetch a su propio backend (`/api/portal/...`).
  - El backend llama a los dos proveedores con un RestClient por proveedor
    (`ClientesConfig`), con URL configurables (`CATALOGO_URL` y
    `RESERVAS_URL`) y tiempos de espera de 2 y 5 segundos.
  - Operaciones integradas: la disponibilidad del día cruza el horario del
    catálogo con la ocupación del proveedor de reservas, y reservar toma el
    nombre y la tarifa del equipo del catálogo, valida el horario y que el
    equipo se pueda reservar, y crea la reserva en el proveedor de reservas.
  - Errores: un proveedor detenido da 503 con el nombre del servicio; los
    errores de un proveedor (400, 401, 403, 409) llegan con su código y su
    mensaje. `/api/portal/estado` dice si cada proveedor responde.
  - Probado a mano con las tres aplicaciones: sesión de Laura, reserva de
    4:00 p. m. creada con los dos proveedores, pago del adelanto, catálogo
    detenido (la página muestra el aviso y "Mis reservas" sigue funcionando) y
    la reserva conservada al reiniciar los proveedores. Capturas en
    `soa/evidencias/`.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 9 pruebas, 0 fallos
  (CP-38a a CP-38i en `PortalTests` y `ClientesHttpTests`).
- **Pendientes:**
  - Colección de Postman y documentación técnica (CACS-28).

## 2026-11-10 — SOA: colección de Postman y documentación técnica

- **Tarjeta:** CACS-28 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-documentacion`
- **Qué se hizo:**
  - `soa/postman/GameSpot-SOA.postman_collection.json`: 40 solicitudes en 5
    carpetas (autenticación, proveedor 1, proveedor 2, consumidora y proveedor
    detenido), con pruebas de código HTTP, variables para las tres URL y los
    tokens, y la fecha de mañana calculada en hora de Bogotá.
  - `soa/README.md`: arquitectura, requisitos, cómo ejecutar, cuentas de
    prueba, contrato de la API y tabla de pruebas.
  - `soa/documentacion/`: documento técnico en inglés con formato APA 7
    (Word y PDF, 21 páginas) y el diagrama SOA en PNG.
- **Pruebas:** la colección corrió completa con Newman contra las tres
  aplicaciones: 38 solicitudes y 40 comprobaciones de las carpetas 1 a 4, y
  con el catálogo detenido 2 solicitudes y 3 comprobaciones de la carpeta 5;
  0 fallos.
- **Pendientes:**
  - Fusionar en orden los PR de CACS-25, CACS-26 y CACS-27; cada uno agrega
    una entrada al final de esta bitácora.

## 2026-11-11 — SOA: portada y reserva guiada en la consumidora

- **Tarjeta:** CACS-29 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-portal-presentacion`
- **Qué se hizo:**
  - `consumidor-web/static`: la página pasa de un formulario técnico a una
    portada con la identidad de Game Spot (la misma del frontend Angular): botón
    "Haz tu reserva", tarjetas de los equipos por modalidad con precio y estado,
    pasos de uso y horario. La reserva se hace en tres pasos (consola, día y
    hora, confirmar) y antes de confirmar muestra a nombre de quién queda, el
    precio y el adelanto. El ingreso es un diálogo; "Mis reservas" son
    tarjetas con pagar y cancelar, y el panel del administrador solo aparece
    con el rol `ADMIN`. Si un proveedor no responde, se avisa cuál y el resto
    de la página sigue funcionando.
  - Fotos del local: carpeta `static/img/` y constante `FOTOS` en `app.js`;
    mientras no haya foto se muestra un ícono por modalidad.
  - `PortalController` y `PortalService`: `GET /api/portal/horarios`, que pide
    el horario al catálogo con RestClient y lo ordena de lunes a domingo.
  - `.gitignore`: se ignora `/data/`, donde quedan las bases H2 si IntelliJ
    ejecuta las aplicaciones desde la raíz del repositorio.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos
  (nueva CP-38j: horario ordenado y 503 con el catálogo detenido). Prueba a
  mano con las tres aplicaciones: reserva completa desde la portada, ingreso en
  el diálogo, pago del adelanto, panel del administrador, catálogo detenido y
  vista de 360 px sin desplazamiento horizontal.
- **Pendientes:**
  - Corregir precios y equipos con los datos que envíe el propietario.
  - Agregar las fotos del local cuando estén disponibles.

## 2026-11-11 — SOA: precios reales con tarifa de media hora y logo

- **Tarjeta:** CACS-30 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-precios-media-hora` (parte de `feat/soa-portal-presentacion`).
- **Qué se hizo:**
  - Equipos y precios corregidos por el propietario: dos Xbox 360 ($4.000 la
    hora, $2.500 la media hora), dos Xbox Series S ($5.000 y $3.000) y la sala
    VIP con Xbox Series X ($10.000 y $6.000).
  - `proveedor-catalogo`: columna `tarifa_media_hora_pesos` (DDL en
    `schema.sql`, con `alter table ... add column if not exists` para las
    bases que ya existían) y corrección única en `data.sql`: solo actúa
    mientras falte la tarifa de media hora, así después no pisa los cambios
    hechos por la API. La tarifa nueva va en la entidad, los DTO y
    `PUT /api/modalidades/{id}`.
  - `proveedor-reservas`: el precio es cada hora completa a la tarifa por hora
    más la tarifa de media hora si sobra media hora; el adelanto sigue siendo
    el 50 %.
  - `consumidor-web`: pasa la tarifa de media hora del catálogo al proveedor de
    reservas y la página muestra ambos precios; logo de Game Spot en la barra,
    en la portada y como ícono, con los colores del logo (dorado y azul cielo).
  - Postman: cuerpos con la tarifa de media hora; el `PUT` de tarifa usa los
    precios reales del Xbox 360 para no dejar datos de prueba en la base.
- **Pruebas:** `./mvnw test`: catálogo 8 pruebas (nueva CP-36h: corrección de
  una base anterior), reservas 9 (nueva CP-37i: 30, 60 y 150 minutos) y
  consumidora 10; 0 fallos. Newman: carpetas 1 a 4, 38 solicitudes y 40
  comprobaciones; carpeta 5 con el catálogo detenido, 2 y 3; 0 fallos. Prueba a
  mano: la base existente quedó con 5 equipos y los precios nuevos, una reserva
  de 90 minutos de Series S costó $8.000 y la página se ve bien a 360 px.
- **Pendientes:**
  - Actualizar el documento técnico en inglés con la tarifa de media hora
    antes de la entrega final del 18 de noviembre.
  - Fotos del local cuando estén disponibles.

## 2026-11-11 — SOA: ilustraciones de consolas y decoración animada

- **Tarjeta:** CACS-31 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-decoracion-gamer`
- **Qué se hizo:**
  - `static/img/consola-*.svg`: ilustraciones propias de la Xbox 360, la
    Xbox Series S y la sala VIP con Series X, que reemplazan los emojis de las
    tarjetas de equipos. Si se agrega una foto en `FOTOS`, la foto tiene
    prioridad.
  - Decoración original con estilo de videojuego: cuadrícula neón que avanza
    en la portada, monedas, estrellas, corazones y controles pixelados que
    flotan detrás del contenido, una nave pixelada que cruza la portada, una
    pandilla de personajes de gelatina en el pie de página y uno que se asoma
    sobre la tarjeta de reserva. No se usan personajes de juegos existentes
    porque tienen derechos de autor.
  - Todo es decorativo (`aria-hidden`, sin clics) y se queda quieto con
    `prefers-reduced-motion`.
  - `app.js`: si el token venció (dura 2 horas), la página cierra la sesión y
    pide ingresar de nuevo, en vez de quedarse con la sesión abierta y un
    error en "Mis reservas".
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano en el navegador a 1280 px y a 360 px (sin desplazamiento
  horizontal) y con un token vencido.
- **Pendientes:**
  - Fotos reales del local y de las consolas cuando estén disponibles.

## 2026-11-11 — SOA: la página con el material real del local

- **Tarjeta:** CACS-32 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-material-local`
- **Qué se hizo:**
  - Se revisó la carpeta de publicidad del local: catálogos de juegos,
    tarifas, torneos, promociones, avisos de servicio y videos. No se abrieron
    los archivos de finanzas.
  - Portada con un video real del local de fondo (sin sonido, en bucle) y los
    lemas del local ("Tu punto de diversión", "Juega · Disfruta · Repite").
  - Franja con lo que ofrece el local: consolas Xbox, sala VIP, torneos,
    ambiente gamer y snacks.
  - Tarjetas de equipos con su nivel (Clásicos, Nueva Gen, Élite), el ahorro
    de pagar la hora completa y un enlace a sus juegos.
  - Sección "Juegos disponibles" con el catálogo real de cada equipo por
    categoría y número de jugadores (`static/datos/juegos.json`); el número de
    juegos también aparece al elegir la consola en la reserva.
  - Sección "Conoce el local" con 4 videos cortos del sitio (8,5 MB en
    total), que solo se reproducen mientras están en pantalla.
  - Torneos (Halo: Reach y KOF 2002) y promociones del local, pasados a texto;
    no se publican las piezas con personajes de terceros.
  - "Visítanos" con la dirección, un botón de WhatsApp con mensaje listo y un
    enlace para llegar en Google Maps.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 px y a 360 px (sin desplazamiento horizontal); los
  catálogos coinciden en número de juegos con los de la publicidad.
- **Pendientes:**
  - Confirmar el horario: la publicidad dice "todos los días de 3:00 p. m. a
    9:00 p. m." y el catálogo del sistema tiene L-V 2:00-10:00 p. m. y S-D
    3:00-9:00 p. m. (resuelto en CACS-33).
  - Confirmar que los torneos y promociones siguen vigentes.

## 2026-11-11 — SOA: horario real del local

- **Tarjeta:** CACS-33 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-horario-real` (parte de `feat/soa-material-local`).
- **Qué se hizo:**
  - El equipo confirmó que el horario real es todos los días de 3:00 a
    9:00 p. m., como dice la publicidad. `proveedor-catalogo/data.sql` queda
    con los 7 días de 15:00 a 21:00; el `merge` también corrige las bases que
    ya existen al arrancar.
  - `CatalogoTests` CP-36g verifica que los 7 días abren a las 15:00 y cierran
    a las 21:00.
- **Pruebas:** `./mvnw test` en `soa/proveedor-catalogo`: 8 pruebas, 0 fallos.
  Con la base existente, `/api/portal/horarios` devolvió 15:00-21:00 para los
  7 días y la disponibilidad de mañana mostró 12 bloques (3:00 a 9:00 p. m.).
  Newman, carpetas 1 a 4: 38 solicitudes y 40 comprobaciones, 0 fallos.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: torneos como historial del local

- **Tarjeta:** CACS-34 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-torneos-realizados`
- **Qué se hizo:**
  - El equipo confirmó que los torneos de Halo: Reach y KOF 2002 ya se
    jugaron. La sección pasa a "Torneos realizados": cada torneo se marca como
    finalizado, los premios quedan en pasado y se quitan los llamados a
    inscribirse.
  - Nueva tarjeta "Próximamente" con un botón de WhatsApp para preguntar por
    el próximo torneo.
  - Las promociones quedan en su propia sección.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano en el navegador.
- **Pendientes:**
  - Confirmar que las promociones siguen vigentes (resuelto en CACS-35).

## 2026-11-11 — SOA: promoción vigente "Reto 10 segundos"

- **Tarjeta:** CACS-35 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-reto-10-segundos`
- **Qué se hizo:**
  - El equipo confirmó que no hay torneos próximos y que solo está vigente el
    Reto 10 segundos de los fines de semana: quien detiene el cronómetro en
    10 segundos exactos paga la cuenta a mitad de precio.
  - Se quitaron las promociones que ya no aplican y la tarjeta del próximo
    torneo. Los torneos realizados siguen como historial.
  - Sección "Promoción vigente" con el reto (sábados y domingos), un aviso en
    la portada y el enlace "Reto 10 s" en el menú.
  - Cronómetro de práctica en la página, marcado como práctica: dice cuánto
    faltó o sobró para los 10,00 s. No da descuentos; el reto válido se hace en
    el local.
  - No se publicaron los videos del reto porque aparecen menores de edad.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano del cronómetro a 1280 px y a 360 px.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: videos sin pantalla negra y carátulas de juegos

- **Tarjeta:** CACS-36 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-videos-caratulas`
- **Qué se hizo:**
  - Causa de las pantallas negras: los videos (H.264) se servían bien, pero no
    tenían imagen de portada y solo se reproducían solos si el sistema no
    tiene las animaciones desactivadas.
  - Cada video tiene ahora su portada (`static/video/*.png`, un cuadro del
    mismo video sacado con Edge sin ventana) y un botón ▶ visible para
    reproducir o pausar; mientras se reproduce, el botón aparece al pasar el
    mouse.
  - El clip "A jugar" se cambió por uno donde solo se ve la pantalla del juego,
    porque el anterior mostraba a un joven que parece menor de edad.
  - "Juegos disponibles": muro de carátulas animado de fondo y miniatura junto
    a cada juego que tiene carátula (22 carátulas de la carpeta del local,
    `static/img/caratulas/`). Se usan por decisión del equipo, sabiendo que
    pertenecen a los estudios de cada juego.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano: portadas visibles con los videos en pausa, reproducción con el
  botón, carátulas a 1280 px y a 360 px sin desplazamiento horizontal.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: títulos centrados y secciones a todo el ancho

- **Tarjeta:** CACS-37 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-titulos-centrados` (parte de `feat/soa-videos-caratulas`).
- **Qué se hizo:**
  - Títulos de sección centrados, en mayúsculas, con la letra Russo One y un
    subrayado neón dorado y azul; sus textos de apoyo también centrados.
  - Videos de "Conoce el local" en 4 columnas que llenan el ancho en
    computador (2 en celular), sin el espacio vacío de la derecha.
  - Tarjetas de equipos, torneos, reservas y categorías de juegos con
    `auto-fit` para que ocupen todo el ancho; pestañas de juegos, pasos de la
    reserva y "Visítanos" centrados.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 px y a 360 px, sin desplazamiento horizontal.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: portada sin video y logo más grande

- **Tarjeta:** CACS-38 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-portada-logo` (parte de `feat/soa-titulos-centrados`).
- **Qué se hizo:**
  - Se quitó el video de fondo de la portada; los videos siguen en "Conoce el
    local".
  - El logo de la portada pasa de 380 a 500 px en computador y ahora también
    se ve en el celular, arriba y centrado; el título se ajusta para quedar en
    dos líneas.
  - `application.yaml` de la consumidora: `Cache-Control: no-cache` para los
    archivos estáticos. El navegador revalida en cada visita y siempre muestra
    la última versión, sin necesidad de Ctrl + F5.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 px y a 360 px, sin desplazamiento horizontal; los
  estilos responden con `Cache-Control: no-cache`.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: pandilla de gelatina animada en la portada

- **Tarjeta:** CACS-39 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-pandilla-portada` (parte de `feat/soa-portada-logo`).
- **Qué se hizo:**
  - En lugar de recrear personajes de Gang Beasts, que son del estudio Boneloaf,
    se usó la pandilla de gelatina propia como mascota del local.
  - Escena en la portada con cuatro personajes (verde, amarillo, azul y rojo)
    sobre un ring neón: el verde salta desde arriba y cae con un "¡PUM!", el
    amarillo y el azul se empujan y el rojo se tambalea, se cae y se levanta.
  - En computador va debajo del logo, en la columna derecha, visible sin bajar;
    en el celular va debajo del contenido de la portada.
  - Decorativo (`aria-hidden`, sin clics) y quieto con "reducir movimiento".
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 x 760 px (escena completa visible) y a 360 px, sin
  desplazamiento horizontal.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: logo visible sobre la pandilla y videos con reproducción automática

- **Tarjeta:** CACS-40 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-logo-videos`
- **Qué se hizo:**
  - El salto del muñeco verde baja de 95 a 60 px y el logo queda por encima de
    la escena (`z-index: 3`): el texto "TU ESPACIO GAMER" se ve completo
    durante toda la animación.
  - Los videos de "Conoce el local" se reproducen solos y sin sonido mientras
    están en pantalla, también con "reducir movimiento" (en el PC del equipo
    quedaban quietos por esa opción de Windows). Si el visitante pausa uno con
    su botón, ese video no vuelve a arrancar solo.
  - Se quitó el código que pausaba el video de fondo de la portada, que ya no
    existe desde CACS-38.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 px, 960 px y 375 px: el muñeco verde no alcanza el
  texto del logo en el punto más alto del salto; los cuatro videos arrancan al
  llegar a la sección, se pausan al salir y el que se pausa a mano sigue
  pausado al volver.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: la pandilla de la portada como botones Y, X, A y B

- **Tarjeta:** CACS-41 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-pandilla-botones`
- **Qué se hizo:**
  - Los cuatro personajes de la portada ahora son botones de acción de un
    control, con cara, brazos y pies: A verde, Y amarillo, X azul y B rojo.
    Son diseño propio (solo letras y colores, sin logotipos de marcas).
  - Se conservan las animaciones de CACS-39: la A salta y cae con "¡PUM!", la
    Y y la X se empujan y la B se cae y se levanta. Siguen siendo decorativos
    (`aria-hidden`) y quedan quietos con "reducir movimiento".
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 px y 375 px: las cuatro letras se leen, las
  animaciones corren, el salto de la A no alcanza el texto del logo y no hay
  desplazamiento horizontal.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: botones Y, X, A y B negros con la letra de color

- **Tarjeta:** CACS-42 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-botones-negros`
- **Qué se hizo:**
  - Siguiendo la referencia que compartió el equipo, los cuatro botones de la
    portada pasan a ser negros brillantes con la letra grande de su color y un
    aro del mismo color para que se distingan del fondo oscuro. Brazos y pies
    también son negros con borde de color.
  - Colores más cercanos a la referencia: A verde lima, B rojo anaranjado,
    X azul cian y Y amarillo.
  - Las animaciones de CACS-39 no cambian.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 px y 375 px: las letras se leen, los botones se
  distinguen del fondo y no hay desplazamiento horizontal.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: gato animado en el Reto 10 segundos

- **Tarjeta:** CACS-43 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-gato-reto`
- **Qué se hizo:**
  - La imagen del gato blanco frente al paisaje de bloques al atardecer, que
    compartió el equipo, quedó en "Promoción vigente": el gato "vigila" el
    cronómetro del Reto 10 segundos (`static/img/gato-reto.jpg`).
  - Animación: acercamiento lento del paisaje, brillo del sol que pulsa y
    nubes de bloques que cruzan el cielo. Marco con el degradado del atardecer
    hacia el dorado y el morado neón de la página.
  - Globo de diálogo que cambia con el cronómetro ("¿Te atreves con los
    10,00?", "No parpadeo…" y un comentario según el resultado) y un brinco al
    detenerlo. El globo es decorativo; el resultado se sigue anunciando en
    `reto-resultado`.
  - En computador va a la izquierda del texto y del cronómetro; en el celular,
    arriba. Quieto con "reducir movimiento".
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 px y 375 px: la imagen carga, el globo reacciona al
  iniciar y detener el cronómetro y no hay desplazamiento horizontal.
- **Pendientes:** la imagen la aportó el equipo; si se publica la página,
  confirmar que se puede usar (el paisaje imita el estilo de un videojuego).

## 2026-11-11 — SOA: título de la portada con voz propia y nueva distribución

- **Tarjeta:** CACS-44 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-titulo-portada`
- **Qué se hizo:**
  - El título genérico ("Reserva tu turno y llega a jugar", con la etiqueta
    "Tu punto de diversión" y el lema "Juega · Disfruta · Repite") cambió por
    uno con la voz del barrio: "El parche GAMER de San Jorge", en tres líneas
    de distinto tamaño, con "GAMER" grande en dorado y un subrayado a mano.
  - Arriba, un aviso de horario con un punto verde que late ("Abierto todos
    los días · 3 a 9 p. m."); junto al título, una calcomanía inclinada
    ("¿Quién paga la revancha?").
  - Texto y botones en tono conversacional ("Apartar mi turno", "Ver
    consolas y precios"). Las cifras pasan a una tira tipo boleta con cortes
    punteados (tres columnas en el celular), y el aviso del Reto 10 segundos
    queda debajo en su propia línea.
  - Se quitaron los estilos que ya no se usan (`portada__etiqueta`,
    `portada__lema` y el color de los `span` del título). Los ids que usa
    `app.js` no cambian.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280, 375 y 360 px: las cifras se llenan desde los
  servicios, el título no toca el logo ni la pandilla y no hay desplazamiento
  horizontal.
- **Pendientes:** ninguno.

## 2026-11-11 — SOA: título "Salón GAMER de San Jorge" sin calcomanía

- **Tarjeta:** CACS-45 (entrega final de Desarrollo Orientado a Servicios).
- **Rama:** `feat/soa-salon-gamer`
- **Qué se hizo:**
  - El título de la portada pasa de "El parche GAMER de San Jorge" a "Salón
    GAMER de San Jorge", a pedido del equipo.
  - Se quitó la calcomanía "¿Quién paga la revancha?" con sus estilos y su
    animación. El resto de la portada de CACS-44 no cambia.
- **Pruebas:** `./mvnw test` en `soa/consumidor-web`: 10 pruebas, 0 fallos.
  Prueba a mano a 1280 px y 360 px, sin desplazamiento horizontal.
- **Pendientes:** ninguno.
