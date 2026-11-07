package com.usta.gamespot;

import org.springframework.boot.SpringApplication;

/**
 * Arranque local con ./mvnw spring-boot:test-run: la API usa un PostgreSQL temporal de
 * Testcontainers y el perfil demo, que carga los datos de demostración (CACS-16).
 */
public class TestBackendApplication {

  public static void main(String[] args) {
    SpringApplication.from(BackendApplication::main)
        .with(TestcontainersConfiguration.class)
        .withAdditionalProfiles("demo")
        .run(args);
  }
}
