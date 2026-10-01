# Estándares del Equipo: Acuerdos, Revisión y Definición de Terminado

**Proyecto Game Spot · Taller 4**

Cristian Farid Amado Peña
Cristian Santiago Silva Muñoz

Ingeniería de Sistemas, Universidad Santo Tomás, sede Villavicencio
Gerencia de Software
Docente: Stefany Gómez Riveros
30/09/2026

---

## Introducción

Este documento reúne los acuerdos de trabajo del equipo conformado por Cristian Silva y Cristian Amado para el desarrollo de Game Spot. Su propósito es que cualquier integrante pueda continuar el trabajo de otro sin perder tiempo ni romper nada, y que todos usen la misma idea de cuándo una tarea está lista para empezar y cuándo está terminada. Rige desde su publicación hasta el cierre del semestre.

### Contexto técnico del equipo

**Tabla 1.** Tecnologías y herramientas reales del equipo

| Elemento | Definición del equipo |
|---|---|
| Lenguaje de programación | Java (versión 21) |
| Framework backend | Spring Boot |
| Pagos | SDK de Wompi (entorno de pruebas) |
| Formateador automático | Formateador integrado de IntelliJ IDEA con el estilo de Google, activado al guardar |
| Repositorio en GitHub | https://github.com/cristianamad0/GameSpot.git |
| Interlocutor principal del proyecto | Carlos Silva |

## Guía de Estilo y Nombres

### Guía oficial adoptada

El equipo adopta la Google Java Style Guide y la aplica de forma automática con el formateador integrado de IntelliJ IDEA, configurado con el estilo de Google y activado para formatear al guardar cada archivo (Settings > Tools > Actions on Save). La configuración de estilo está guardada en el repositorio (`.editorconfig` y `.idea/codeStyles/`), de modo que ambos integrantes aplican las mismas reglas.

### Idioma del código

El código (variables, funciones, clases) se escribe en español y los comentarios en español. No se mezclan idiomas dentro de un mismo nombre.

### Reglas propias de nombres

**Regla 1: La unidad va dentro del nombre**

- Correcto: `duracionMinutos`, `montoCentavos`, `montoPesos`. Incorrecto: `duracion`, `monto`. Esto evita confundir pesos con centavos al integrar Wompi y minutos con horas al calcular reservas y puntos.

**Regla 2: Los booleanos se leen como pregunta de sí o no**

- Correcto: `estaDisponible`, `tieneAnticipoPagado`. Incorrecto: `flag`, `estado`.

**Regla 3: Las tablas van en snake_case plural y las clases en PascalCase singular**

- Correcto: tabla `reservas`, clase `Reserva`. Incorrecto: tabla `Reserva`, clase `reservas`.

## Convención de Commits y Ramas

### Formato del mensaje

Los mensajes siguen el formato `tipo(alcance): descripción en imperativo`, según la especificación de Conventional Commits (2023). Cada commit contiene un solo cambio con sentido propio.

**Tabla 2.** Tipos de commit permitidos y ejemplos de Game Spot

| Tipo | Cuándo se usa | Ejemplo |
|---|---|---|
| feat | Funcionalidad nueva para el usuario | `feat(reservas): agregar barra de reserva rápida` |
| fix | Corrección de un error | `fix(reservas): corregir bloque libre que no permitía reservar` |
| docs | Solo documentación | `docs(reservas): documentar cómo hacer una reserva` |
| refactor | Cambio interno sin alterar el comportamiento | `refactor(reservas): simplificar el cálculo de disponibilidad` |
| test | Agregar o corregir pruebas | `test(reservas): agregar prueba de reservas simultáneas` |
| style | Formato sin cambio de lógica | `style(pagos): aplicar formateador a la clase de pagos` |

### Esquema de ramas

**Tabla 3.** Ramas del repositorio

| Rama | Uso y regla |
|---|---|
| main | Solo código que funciona. Nadie programa directamente aquí. |
| develop | Integración del trabajo de todos. |
| feat/nombre-corto | Una rama por historia de usuario; se borra al integrarse. |

## Definition of Ready

Una historia de usuario solo entra al sprint si cumple todas las condiciones de la Tabla 4. Se aplica durante la planeación del sprint.

**Tabla 4.** Condiciones de la Definition of Ready (entre cuatro y seis)

| N.º | Condición | Cómo se comprueba |
|---|---|---|
| 1 | La historia de usuario está redactada de forma clara y describe quién la necesita, qué necesita y para qué. | La historia está escrita en formato de usuario y puede ser entendida por cualquier integrante del equipo. |
| 2 | La historia tiene criterios de aceptación claros, verificables y relacionados con el resultado esperado. | Los criterios pueden comprobarse mediante una prueba y permiten determinar si la historia está cumplida. |
| 3 | La historia tiene una prioridad definida de acuerdo con el alcance y las necesidades del proyecto. | La historia aparece ordenada en el backlog y su prioridad está registrada. |
| 4 | La historia está estimada en story points y tiene un tamaño adecuado para ser desarrollada dentro del Sprint | El equipo registra la estimación y ambos integrantes validan que la historia sea abordable. |
| 5 | Las dependencias, reglas de negocio y recursos necesarios para desarrollar la historia están identificados | Se revisa que no existan dependencias desconocidas y que la información necesaria esté disponible. |
| 6 | La historia tiene definida la evidencia que permitirá demostrar que fue completada. | Existe una prueba, captura, resultado funcional o registro que otra persona pueda revisar. |

## Definition of Done

Una historia se considera terminada solo si cumple todos los puntos de la Tabla 5. Cada punto debe poder ser comprobado por un tercero abriendo el repositorio, sin preguntarle a quien programó. La lista es igual para todas las historias y no se negocia caso por caso.

**Tabla 5.** Condiciones de la Definition of Done (entre cinco y ocho)

| N.º | Condición | Evidencia verificable |
|---|---|---|
| 1 | Cumple los criterios de aceptación de la historia, verificados por el otro integrante. | La tarjeta tiene los criterios marcados y un comentario del verificador. |
| 2 | Tiene al menos una prueba automatizada por criterio de aceptación, y todas pasan. | El resultado de las pruebas aparece sin fallos en el pull request. |
| 3 | El código no tiene cambios pendientes al ejecutar Reformat Code de IntelliJ. | El revisor lo ejecuta sobre los archivos del pull request y no aparecen cambios. |
| 4 | Fue revisada y aprobada por un integrante distinto del autor. | El pull request tiene la aprobación del otro integrante. |
| 5 | Está integrada a develop sin conflictos. | El pull request aparece fusionado en develop. |
| 6 | Los commits siguen la convención acordada. | El historial de la rama muestra mensajes con formato tipo(alcance): descripción. |

## Política de Revisión de Código

**Tabla 6.** Reglas de la revisión de código del equipo

| Punto | Acuerdo del equipo |
|---|---|
| Quién revisa | El integrante que no es el autor del cambio. Cristian Silva revisa lo que escribe Cristian Amado, y Cristian Amado revisa lo que escribe Cristian Silva. La regla no cambia con la rotación de roles de la semana 14. |
| Plazo de revisión | Máximo 24 horas hábiles desde que se abre el pull request. En semana de parciales, el integrante lo avisa con anticipación en el tablero y el plazo pasa a 48 horas hábiles. |
| Qué bloquea la integración | Error de lógica (por ejemplo, permite una doble reserva o confirma un bloque sin adelanto pagado); falta de una prueba exigida por la Definition of Done; credencial o llave de Wompi en el código; código que no compila o no pasa las pruebas; incumplimiento de un criterio de aceptación de la historia. |
| Qué no bloquea | Preferencias personales de estilo que ya resuelve el formateador y sugerencias de mejora futura, que se anotan como una tarjeta nueva en el tablero. |
| Cómo se comenta | Sobre el código, no sobre la persona, y con una propuesta de solución. Cada comentario indica si bloquea o es sugerencia. |

## Aceptación

Cada integrante declara haber leído el documento completo y acepta cumplirlo.

**Tabla 7.** Aceptación de los estándares por parte del equipo

| Nombre completo | Declaración | Fecha |
|---|---|---|
| Cristian Farid Amado Peña | Conozco y acepto estos estándares | 30/09/2026 |
| Cristian Santiago Silva Muñoz | Conozco y acepto estos estándares | 30/09/2026 |

**Código de sesión:** LLANO-14

## Referencias

Conventional Commits. (2023). *Conventional Commits 1.0.0*. https://www.conventionalcommits.org/es/v1.0.0/

## DECLARACION DE IA

Usamos la IA para la creacion del formato mas no para su complexion ya que decidimos optar su uso de esta manera para que la informacion se presente de manera organizada con nuestros conocimientos anexados en los espacios pedidos.


