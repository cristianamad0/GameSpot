package com.usta.gamespot.demo;

import static com.usta.gamespot.service.AdminReservasService.ZONA_NEGOCIO;

import com.usta.gamespot.model.ConfiguracionNegocio;
import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.MovimientoPuntos;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.TipoMovimientoPuntos;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.ConfiguracionNegocioRepository;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.MovimientoPuntosRepository;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.service.ProveedorAnticiposSimulado;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-16: carga datos de ejemplo al arrancar la API en local con {@code ./mvnw
 * spring-boot:test-run}, que activa el perfil demo (ver TestBackendApplication y
 * application-demo.yaml).
 *
 * <p>Vive en el código de pruebas: nunca llega al jar ni corre contra Supabase. Además solo se
 * activa con {@code app.demo.datos=true}, que está apagada en las pruebas automatizadas.
 *
 * <p>Crea un administrador, dos clientes, reservas en los cinco estados con fechas cercanas al
 * día del arranque, pagos y movimientos de puntos. Los estados CONFIRMADA y COMPLETADA y los puntos
 * por horas jugadas los ponen los triggers de la base, igual que en el uso real.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.demo.datos", havingValue = "true")
@RequiredArgsConstructor
public class DatosDemostracion implements ApplicationRunner {

  /** Correo del administrador de demostración. */
  public static final String EMAIL_ADMIN = "admin@gamespot.demo";

  /** Cliente con reservas en casi todos los estados y puntos acumulados. */
  public static final String EMAIL_LAURA = "laura@gamespot.demo";

  /** Cliente con una reserva para hoy y una expirada con pago aprobado tarde. */
  public static final String EMAIL_ANDRES = "andres@gamespot.demo";

  /** Contraseña de prueba del administrador; está documentada en el README, no es un secreto. */
  public static final String CONTRASENA_ADMIN = "AdminDemo123";

  /** Contraseña de prueba de los dos clientes; documentada en el README. */
  public static final String CONTRASENA_CLIENTE = "ClienteDemo123";

  private final UsuarioRepository usuarioRepository;
  private final EquipoRepository equipoRepository;
  private final ReservaRepository reservaRepository;
  private final PagoRepository pagoRepository;
  private final MovimientoPuntosRepository movimientoPuntosRepository;
  private final ConfiguracionNegocioRepository configuracionNegocioRepository;
  private final PasswordEncoder passwordEncoder;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (usuarioRepository.existsByEmail(EMAIL_ADMIN)) {
      log.info("Los datos de demostración ya estaban cargados");
      return;
    }

    usuario("Administrador Demo", EMAIL_ADMIN, CONTRASENA_ADMIN, Rol.ADMIN);
    Usuario laura = usuario("Laura Gómez", EMAIL_LAURA, CONTRASENA_CLIENTE, Rol.CLIENTE);
    Usuario andres = usuario("Andrés Rojas", EMAIL_ANDRES, CONTRASENA_CLIENTE, Rol.CLIENTE);

    Instant ahora = Instant.now();
    LocalDate hoy = LocalDate.now(ZONA_NEGOCIO);
    LocalDate ayer = hoy.minusDays(1);
    LocalDate manana = hoy.plusDays(1);
    // Las horas caen entre las 16:00 y las 21:00, dentro del horario de todos los días.

    // Laura jugó ayer dos horas: la reserva se paga y se completa, y gana sus puntos.
    Reserva completada = reserva(laura, "Xbox Series S #1", a(ayer, 16), 120, ahora);
    pagar(completada, EstadoPago.APROBADO, 1);
    completada.setEstado(EstadoReserva.COMPLETADA);
    reservaRepository.saveAndFlush(completada);

    // Laura tiene para mañana una reserva pagada, una pendiente y una cancelada.
    Reserva confirmada = reserva(laura, "Xbox Series S #2", a(manana, 17), 60, ahora);
    pagar(confirmada, EstadoPago.APROBADO, 1);

    // El plazo de pago llega hasta la hora de inicio, para que no expire durante la demostración.
    Reserva pendiente = reserva(laura, "Xbox 360 #1", a(manana, 18), 60, a(manana, 18));
    pagar(pendiente, EstadoPago.RECHAZADO, 1);

    Reserva cancelada = reserva(laura, "Sala VIP Xbox Series X", a(manana, 16), 60, ahora);
    cancelada.setEstado(EstadoReserva.CANCELADA);
    cancelada.setCanceladaEn(ahora);
    cancelada.setMotivoCancelacion("El cliente pidió cancelar (demostración)");
    reservaRepository.saveAndFlush(cancelada);

    // Andrés juega hoy en la Sala VIP, para que el panel del día tenga datos.
    Reserva deHoy = reserva(andres, "Sala VIP Xbox Series X", a(hoy, 20), 60, ahora);
    pagar(deHoy, EstadoPago.APROBADO, 1);

    // Andrés no pagó a tiempo ayer y su pago llegó después: el admin lo ve en "expiradas con pago".
    Reserva expirada = reserva(andres, "Xbox 360 #1", a(ayer, 19), 60, a(ayer, 18));
    expirada.setEstado(EstadoReserva.EXPIRADA);
    reservaRepository.saveAndFlush(expirada);
    pagar(expirada, EstadoPago.APROBADO, 1);

    ajustarPuntos(laura, 10, "Bono de bienvenida (demostración)");
    ajustarPuntos(andres, 5, "Bono de bienvenida (demostración)");

    log.info(
        "Datos de demostración cargados: {} (ADMIN), {} y {} (CLIENTE). Contraseñas en el README",
        EMAIL_ADMIN,
        EMAIL_LAURA,
        EMAIL_ANDRES);
  }

  private Usuario usuario(String nombre, String email, String contrasena, Rol rol) {
    return usuarioRepository.saveAndFlush(
        Usuario.builder()
            .nombre(nombre)
            .email(email)
            .passwordHash(passwordEncoder.encode(contrasena))
            .rol(rol)
            .build());
  }

  /** Momento del día indicado a la hora en punto, en hora de Bogotá. */
  private static Instant a(LocalDate dia, int hora) {
    return dia.atTime(hora, 0).atZone(ZONA_NEGOCIO).toInstant();
  }

  /** Reserva en PENDIENTE_PAGO con precio y adelanto calculados como en ReservaService. */
  private Reserva reserva(
      Usuario usuario, String nombreEquipo, Instant inicio, int duracionMinutos, Instant expiraEn) {
    Equipo equipo =
        equipoRepository.findAll().stream()
            .filter(e -> e.getNombre().equals(nombreEquipo))
            .findFirst()
            .orElseThrow();
    ConfiguracionNegocio configuracion = configuracionNegocioRepository.obtener();
    int precioTotalPesos =
        Math.ceilDiv(equipo.getModalidad().getTarifaHoraPesos() * duracionMinutos, 60);
    int adelantoPesos =
        Math.ceilDiv(precioTotalPesos * configuracion.getPorcentajeAdelanto(), 100);

    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(usuario)
            .equipo(equipo)
            .inicio(inicio)
            .fin(inicio.plus(duracionMinutos, ChronoUnit.MINUTES))
            .precioTotalPesos(precioTotalPesos)
            .adelantoPesos(adelantoPesos)
            .expiraEn(expiraEn)
            .build());
  }

  /**
   * Registra un pago simulado del adelanto. Si es APROBADO y la reserva sigue PENDIENTE_PAGO, el
   * trigger de la base la confirma; por eso se vuelve a leer la reserva.
   */
  private void pagar(Reserva reserva, EstadoPago estado, int intento) {
    pagoRepository.saveAndFlush(
        Pago.builder()
            .reserva(reserva)
            .referencia("DEMO-" + reserva.getId() + "-" + intento)
            .montoCentavos(reserva.getAdelantoPesos() * 100L)
            .estado(estado)
            .metodoPago(ProveedorAnticiposSimulado.METODO_SIMULADO)
            .build());
    entityManager.refresh(reserva);
  }

  private void ajustarPuntos(Usuario usuario, int puntos, String descripcion) {
    movimientoPuntosRepository.saveAndFlush(
        MovimientoPuntos.builder()
            .usuario(usuario)
            .puntos(puntos)
            .tipo(TipoMovimientoPuntos.AJUSTE_ADMIN)
            .descripcion(descripcion)
            .build());
  }
}
