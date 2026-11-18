# Reporte de análisis estático · 18 de noviembre de 2026

Segunda medición del backend, para la entrega final. La primera, del 7 de
octubre, está en [analisis-estatico.md](analisis-estatico.md).

| Dato | Valor |
| --- | --- |
| Fecha de ejecución | 18 de noviembre de 2026 |
| Código analizado | `backend/src/main/java` (código de producción), rama `dev`, commit `7706427` (último cambio del backend en `dev`) |
| Tamaño | 2914 líneas de código sin comentarios ni líneas en blanco (NLOC), 149 métodos con cuerpo |

## Herramientas

| Herramienta | Versión | Configuración | Qué revisa |
| --- | --- | --- | --- |
| Lizard | 1.24.1 | Umbral de complejidad ciclomática: 10 | Complejidad ciclomática y tamaño por método |
| Checkstyle | 10.26.1 (maven-checkstyle-plugin 3.6.0) | `google_checks.xml` y los paquetes que revisa el `pom.xml`; falla ante cualquier advertencia | Formato y convenciones de estilo |
| PMD | 7.17.0 (maven-pmd-plugin 3.28.0) | Reglas por defecto del plugin (`maven-pmd-plugin-default.xml`) | Errores probables, código innecesario, malas prácticas |

## Resumen

| Herramienta | Hallazgos | Archivo de salida |
| --- | --- | --- |
| Lizard | 0 métodos sobre el umbral | [lizard-complejidad-2026-11-18.txt](lizard-complejidad-2026-11-18.txt) |
| Checkstyle | 0 advertencias en 81 archivos | [checkstyle-resultado-2026-11-18.xml](checkstyle-resultado-2026-11-18.xml) |
| PMD | 0 | [pmd-resultado-2026-11-18.xml](pmd-resultado-2026-11-18.xml) |

## Complejidad ciclomática (Lizard)

| Indicador | 7/10/2026 | 18/11/2026 |
| --- | --- | --- |
| Líneas de código (NLOC) | 981 | 2914 |
| Métodos con cuerpo | 37 | 149 |
| Complejidad promedio por método | 1,2 | 1,6 |
| Complejidad máxima | 6 (`JwtAuthFilter.doFilterInternal`) | 9 (`PagoWompiService.verificarChecksum`) |
| Métodos con complejidad 1 | 32 | 107 |
| Métodos con complejidad 2 a 5 | 4 | 37 |
| Métodos con complejidad 6 a 10 | 1 | 5 |
| Métodos con complejidad mayor a 10 | 0 | 0 |

Los métodos de complejidad 6 a 10 son:

| Método | Complejidad |
| --- | --- |
| `PagoWompiService.verificarChecksum` | 9 |
| `ReservaService.crear` | 6 |
| `JwtAuthFilter.doFilterInternal` | 6 |
| `PagoWompiService.estadoDe` | 6 |
| `PropiedadesPagos.Wompi.estaCompleta` | 6 |

La complejidad sigue dentro del umbral de 10, pero va al alza: el máximo pasó de
6 a 9 con la validación de la firma de los eventos de Wompi. Si ese método
crece, conviene partirlo.

## Checkstyle

0 advertencias desde el 28 de octubre (de 243 el 7 de octubre). El build falla
ante cualquier advertencia (`violationSeverity` en `warning`), así que el valor
se mantiene en cada PR.

No es una comparación exacta: el 7 de octubre se usó Checkstyle 14.3.0 por línea
de comandos sobre todo el código; hoy se usa el plugin de Maven sobre los
paquetes que lista el `pom.xml` (no incluye `repository`, `tarea` ni las
entidades de `model` distintas de `Usuario` y `Rol`).

## PMD

0 hallazgos. Tampoco se compara de frente con los 2 del 7 de octubre: aquella vez
se usó el conjunto `quickstart.xml` sin el classpath de dependencias, y hoy las
reglas por defecto del plugin de Maven.

## Cómo repetir el análisis

Desde `backend/`:

```bash
lizard src/main/java -l java --CCN 10
./mvnw checkstyle:check
./mvnw pmd:pmd
```

Checkstyle deja su resultado en `target/checkstyle-result.xml` y PMD en
`target/pmd.xml`.
