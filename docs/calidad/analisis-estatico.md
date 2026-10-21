# Reporte de análisis estático · Game Spot Reservas

| Dato | Valor |
| --- | --- |
| Fecha de ejecución | 7 de octubre de 2026 |
| Código analizado | `backend/src/main/java` (código de producción), rama `feat/bd-supabase`, commit `ab3c081` |
| Tamaño | 981 líneas de código sin comentarios ni líneas en blanco (NLOC), 37 métodos con cuerpo |

## Herramientas

| Herramienta | Versión | Configuración | Qué revisa |
| --- | --- | --- | --- |
| PMD | 7.28.0 | `rulesets/java/quickstart.xml` | Errores probables, código innecesario, malas prácticas |
| Checkstyle | 14.3.0 | `google_checks.xml` (Google Java Style, la guía adoptada en ESTANDARES) | Formato y convenciones de estilo |
| Lizard | 1.24.1 | Umbral de complejidad ciclomática: 10 | Complejidad ciclomática y tamaño por método |

## Resumen

| Herramienta | Hallazgos | Prioridad alta | Archivo de salida |
| --- | --- | --- | --- |
| PMD | 2 | 0 | [pmd-reporte.txt](pmd-reporte.txt) |
| Checkstyle | 243 advertencias | 0 errores | [checkstyle-reporte.txt](checkstyle-reporte.txt) |
| Lizard | 0 métodos sobre el umbral | — | [lizard-complejidad.txt](lizard-complejidad.txt) |

## PMD

| Archivo | Línea | Regla | Mensaje |
| --- | --- | --- | --- |
| `controller/AuthController.java` | 11 | UnnecessaryImport | Import con comodín `org.springframework.web.bind.annotation.*` |
| `model/Usuario.java` | 3 | UnnecessaryImport | Import con comodín `jakarta.persistence.*` |

PMD se ejecutó sin el classpath de dependencias, por lo que reporta como "innecesarios" dos imports con comodín que sí se usan. No son errores de lógica, pero sí incumplen la Google Java Style Guide, que prohíbe los imports con comodín (Checkstyle los reporta como `AvoidStarImport`).

## Checkstyle

| Regla | Advertencias | Qué significa |
| --- | --- | --- |
| Indentation | 165 | Sangría de 4 espacios; Google Java Style usa 2 |
| MissingJavadocMethod | 25 | Métodos públicos sin comentario Javadoc |
| MissingJavadocType | 22 | Clases o interfaces públicas sin comentario Javadoc |
| CustomImportOrder | 19 | Orden de los imports distinto al de Google |
| LineLength | 8 | Líneas de más de 100 caracteres |
| AvoidStarImport | 2 | Imports con comodín |
| SummaryJavadoc | 1 | Primera frase del Javadoc con formato no permitido |
| EmptyLineSeparator | 1 | Falta línea en blanco entre métodos |
| **Total** | **243** | |

Por módulo: 230 advertencias están en el módulo de autenticación (login, registro, seguridad y `Usuario`) y 13 en el módulo de datos (entidades y repositorios nuevos). Las 165 de sangría, las 19 de orden de imports y las 2 de comodín se corrigen solas al aplicar el formateador de IntelliJ con el estilo de Google, que la Definition of Done exige (punto 3). Las de Javadoc requieren documentar a mano.

## Complejidad ciclomática (Lizard)

| Indicador | Valor |
| --- | --- |
| Complejidad promedio por método | 1,2 |
| Complejidad máxima | 6 (`JwtAuthFilter.doFilterInternal`) |
| Métodos con complejidad 1 | 32 |
| Métodos con complejidad 2 a 5 | 4 |
| Métodos con complejidad 6 a 10 | 1 |
| Métodos con complejidad mayor a 10 | 0 |

Lizard solo mide métodos escritos a mano. Los getters, setters y constructores que genera Lombok no aparecen.

## Cómo repetir el análisis

```bash
pmd check -d backend/src/main/java -R rulesets/java/quickstart.xml -f text
java -jar checkstyle-14.3.0-all.jar -c /google_checks.xml backend/src/main/java
lizard backend/src/main/java -l java --CCN 10
```
