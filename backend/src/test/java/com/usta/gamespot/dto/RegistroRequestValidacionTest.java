package com.usta.gamespot.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas unitarias de las validaciones del formulario de registro (HU-01, CA-01.2). No levantan
 * Spring ni base de datos: usan el mismo validador que aplica @Valid en el controlador.
 */
class RegistroRequestValidacionTest {

  private static final String NOMBRE_VALIDO = "Cliente Prueba";
  private static final String CORREO_VALIDO = "cliente@gamespot.co";
  private static final String CONTRASENA_VALIDA = "clave-segura-123";

  private static ValidatorFactory fabrica;
  private static Validator validador;

  @BeforeAll
  static void crearValidador() {
    fabrica = Validation.buildDefaultValidatorFactory();
    validador = fabrica.getValidator();
  }

  @AfterAll
  static void cerrarValidador() {
    fabrica.close();
  }

  private boolean campoEsValido(RegistroRequest solicitud, String campo) {
    Set<ConstraintViolation<RegistroRequest>> violaciones = validador.validate(solicitud);
    return violaciones.stream().noneMatch(v -> v.getPropertyPath().toString().equals(campo));
  }

  @ParameterizedTest(name = "contraseña de {0} caracteres -> válida={1}")
  @DisplayName("CP-02 Longitud mínima de la contraseña (valores límite)")
  @CsvSource({"0, false", "7, false", "8, true", "9, true"})
  void longitudDeLaContrasena(int longitudCaracteres, boolean esValida) {
    RegistroRequest solicitud =
        new RegistroRequest(NOMBRE_VALIDO, CORREO_VALIDO, "x".repeat(longitudCaracteres));

    assertEquals(esValida, campoEsValido(solicitud, "password"));
  }

  @ParameterizedTest(name = "nombre de {0} caracteres -> válido={1}")
  @DisplayName("CP-03 Longitud del nombre (valores límite)")
  @CsvSource({"1, false", "2, true", "120, true", "121, false"})
  void longitudDelNombre(int longitudCaracteres, boolean esValido) {
    RegistroRequest solicitud =
        new RegistroRequest("x".repeat(longitudCaracteres), CORREO_VALIDO, CONTRASENA_VALIDA);

    assertEquals(esValido, campoEsValido(solicitud, "nombre"));
  }

  @ParameterizedTest(name = "correo \"{0}\" -> válido={1}")
  @DisplayName("CP-04 Formato del correo (partición de equivalencias)")
  @CsvSource({
    "cliente@gamespot.co, true",
    "kevin.ospina@usta.edu.co, true",
    "cliente.gamespot.co, false",
    "@gamespot.co, false",
    "cliente@, false",
    "'', false"
  })
  void formatoDelCorreo(String correo, boolean esValido) {
    RegistroRequest solicitud = new RegistroRequest(NOMBRE_VALIDO, correo, CONTRASENA_VALIDA);

    assertEquals(esValido, campoEsValido(solicitud, "email"));
  }
}
