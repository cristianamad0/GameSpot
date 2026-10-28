package com.usta.gamespot;

import com.usta.gamespot.dto.RegistroRequest;
import com.usta.gamespot.service.AuthService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Punto de entrada de la aplicación GameSpot. */
@SpringBootApplication
public class BackendApplication {

  private static final BufferedReader READER = new BufferedReader(new InputStreamReader(System.in));

  /**
   * Arranca el contexto de Spring Boot.
   *
   * @param args argumentos de la línea de comandos
   */
  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }

  /**
   * Registra un cliente desde la consola al arrancar. Solo se activa si la propiedad
   * {@code app.cli.registration.enabled} vale {@code true}.
   *
   * @param authService servicio que hace el registro
   * @return la tarea que se ejecuta al iniciar la aplicación
   */
  @Bean
  @ConditionalOnProperty(name = "app.cli.registration.enabled", havingValue = "true")
  public CommandLineRunner registrarClientePorConsola(AuthService authService) {
    return args -> {
      System.out.println("=== Registro rápido de cliente ===");

      String nombre = leerDato("Nombre del cliente:");
      String email = leerDato("Correo electrónico:");
      String password = leerDato("Contraseña:");

      try {
        var respuesta = authService.registrar(new RegistroRequest(nombre, email, password));
        System.out.println("\nCliente registrado correctamente.");
        System.out.println("ID: " + respuesta.id());
        System.out.println("Nombre: " + respuesta.nombre());
        System.out.println("Email: " + respuesta.email());
        System.out.println("Token JWT: " + respuesta.token());
      } catch (Exception e) {
        System.err.println("No se pudo registrar el cliente: " + e.getMessage());
      }
    };
  }

  private String leerDato(String mensaje) {
    System.out.print(mensaje + " ");

    try {
      return READER.readLine();
    } catch (IOException e) {
      throw new IllegalStateException("No fue posible leer la entrada del usuario", e);
    }
  }
}
