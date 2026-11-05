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
