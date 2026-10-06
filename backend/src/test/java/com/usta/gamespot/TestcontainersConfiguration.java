package com.usta.gamespot;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  /**
   * Postgres de pruebas con el mismo esquema de Supabase: el contenedor ejecuta db/esquema.sql
   * al arrancar, así que las pruebas validan las entidades contra las tablas reales.
   */
  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("db/esquema.sql"),
            "/docker-entrypoint-initdb.d/01_esquema.sql");
  }
}
