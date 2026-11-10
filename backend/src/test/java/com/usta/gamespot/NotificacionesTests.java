package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.usta.gamespot.dto.ResultadoSimulado;
import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Notificacion;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Rol;
import com.usta.gamespot.model.TipoNotificacion;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.NotificacionRepository;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.service.EnviadorCorreo;
import com.usta.gamespot.service.NotificacionService;
import com.usta.gamespot.service.PagoAnticipoService;
import com.usta.gamespot.service.RegistroResultadoPago;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de las notificaciones por correo (HU-08, CACS-8). El envío de correo está simulado
 * ({@link EnviadorCorreo} es un mock): ninguna prueba envía correos reales. Usan fechas de 2099
 * para que las reservas siempre estén en el futuro. Cada prueba corre en una transacción que se
 * revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class NotificacionesTests {

  /** Lunes 2 de marzo de 2099, 15:00 en Bogotá (UTC-5). */
  private static final Instant LUNES_TRES_PM = Instant.parse("2099-03-02T20:00:00Z");

  /** Plazo de pago que todavía no vence cuando corren las pruebas. */
  private static final Instant PLAZO_VIGENTE = Instant.parse("2099-12-31T05:00:00Z");

  @MockitoBean private EnviadorCorreo enviadorCorreo;

  @Autowired private NotificacionService notificacionService;
  @Autowired private PagoAnticipoService pagoAnticipoService;
  @Autowired private RegistroResultadoPago registroResultadoPago;
  @Autowired private NotificacionRepository notificacionRepository;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private PagoRepository pagoRepository;
  @Autowired private Environment environment;

  private Usuario cliente;
  private Equipo xbox360;

  @BeforeEach
  void prepararDatos() {
    cliente =
        usuarioRepository.saveAndFlush(
            Usuario.builder()
                .nombre("Laura")
                .email("laura@gamespot.test")
                .passwordHash("hash-de-prueba")
                .rol(Rol.CLIENTE)
                .build());
    xbox360 =
        equipoRepository.findAll().stream()
            .filter(e -> e.getNombre().equals("Xbox 360 #1"))
            .findFirst()
            .orElseThrow();
  }

  /** Reserva de una hora del Xbox 360 en PENDIENTE_PAGO, lista para pagar. */
  private Reserva reservaPendiente(Instant inicio) {
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(cliente)
            .equipo(xbox360)
            .inicio(inicio)
            .fin(inicio.plus(1, ChronoUnit.HOURS))
            .precioTotalPesos(5000)
            .adelantoPesos(2500)
            .expiraEn(PLAZO_VIGENTE)
            .build());
  }

  /** Reserva ya confirmada (con su adelanto pagado), sin notificaciones. */
  private Reserva reservaConfirmada(Instant inicio) {
    Reserva reserva = reservaPendiente(inicio);
    reserva.setEstado(EstadoReserva.CONFIRMADA);
    reserva.setTieneAnticipoPagado(true);
    reserva.setExpiraEn(null);
    return reservaRepository.saveAndFlush(reserva);
  }

  private Notificacion notificacion(Reserva reserva, TipoNotificacion tipo, Instant para) {
    return notificacionRepository.saveAndFlush(
        Notificacion.builder()
            .usuario(cliente)
            .reserva(reserva)
            .tipo(tipo)
            .programadaPara(para)
            .build());
  }

  private Notificacion leer(Notificacion notificacion) {
    return notificacionRepository.findById(notificacion.getId()).orElseThrow();
  }

  // Criterio 1: al confirmarse una reserva se crean sus dos notificaciones.

  @Test
  @DisplayName("CP-34a Al aprobarse el pago se programan la confirmación y el recordatorio")
  void pagoAprobadoProgramaLasDosNotificaciones() {
    Reserva reserva = reservaPendiente(LUNES_TRES_PM);
    Instant antes = Instant.now();

    pagoAnticipoService.pagarAdelanto(
        reserva.getId(), cliente.getEmail(), ResultadoSimulado.APROBADO);

    List<Notificacion> notificaciones =
        notificacionRepository.findByReservaIdOrderByProgramadaParaAsc(reserva.getId());
    assertEquals(2, notificaciones.size());
    Notificacion confirmacion = notificaciones.get(0);
    assertEquals(TipoNotificacion.CONFIRMACION, confirmacion.getTipo());
    assertFalse(confirmacion.getProgramadaPara().isBefore(antes));
    assertFalse(confirmacion.getProgramadaPara().isAfter(Instant.now()));
    Notificacion recordatorio = notificaciones.get(1);
    assertEquals(TipoNotificacion.RECORDATORIO, recordatorio.getTipo());
    assertEquals(LUNES_TRES_PM.minus(1, ChronoUnit.HOURS), recordatorio.getProgramadaPara());
    assertFalse(recordatorio.isFueEnviada());
  }

  @Test
  @DisplayName("CP-34b Si falta menos de 1 hora para el inicio, solo se programa la confirmación")
  void menosDeUnaHoraSoloConfirmacion() {
    // El siguiente bloque de :00 o :30: empieza en menos de 30 minutos.
    Instant ahora = Instant.now();
    long segundosBloque = 30 * 60;
    Instant inicio =
        Instant.ofEpochSecond((ahora.getEpochSecond() / segundosBloque + 1) * segundosBloque);
    Reserva reserva = reservaPendiente(inicio);

    pagoAnticipoService.pagarAdelanto(
        reserva.getId(), cliente.getEmail(), ResultadoSimulado.APROBADO);

    List<Notificacion> notificaciones =
        notificacionRepository.findByReservaIdOrderByProgramadaParaAsc(reserva.getId());
    assertEquals(1, notificaciones.size());
    assertEquals(TipoNotificacion.CONFIRMACION, notificaciones.getFirst().getTipo());
  }

  @Test
  @DisplayName("CP-34c Un pago rechazado no programa nada y repetir la confirmación no duplica")
  void pagoRechazadoNoProgramaYNoSeDuplica() {
    Reserva reserva = reservaPendiente(LUNES_TRES_PM);

    pagoAnticipoService.pagarAdelanto(
        reserva.getId(), cliente.getEmail(), ResultadoSimulado.RECHAZADO);
    assertTrue(
        notificacionRepository.findByReservaIdOrderByProgramadaParaAsc(reserva.getId()).isEmpty());

    pagoAnticipoService.pagarAdelanto(
        reserva.getId(), cliente.getEmail(), ResultadoSimulado.APROBADO);
    Reserva confirmada = reservaRepository.findById(reserva.getId()).orElseThrow();
    notificacionService.programarAlConfirmar(confirmada, Instant.now());

    assertEquals(
        2, notificacionRepository.findByReservaIdOrderByProgramadaParaAsc(reserva.getId()).size());
  }

  @Test
  @DisplayName("CP-34d El punto único también sirve para el webhook de Wompi")
  void registroDelWebhookTambienProgramaNotificaciones() {
    Reserva reserva = reservaPendiente(LUNES_TRES_PM);
    Pago pago =
        pagoRepository.saveAndFlush(
            Pago.builder()
                .reserva(reserva)
                .referencia("GS-WOMPI-1")
                .montoCentavos(250_000L)
                .build());

    // Así registra PagoWompiService el resultado de un evento APPROVED.
    registroResultadoPago.registrar(pago, EstadoPago.APROBADO, "tx-1", "CARD", "{}");

    assertEquals(
        2, notificacionRepository.findByReservaIdOrderByProgramadaParaAsc(reserva.getId()).size());
  }

  @Test
  @DisplayName("CP-34e Un pago aprobado sobre una reserva expirada no programa notificaciones")
  void reservaExpiradaNoProgramaNotificaciones() {
    Reserva reserva = reservaPendiente(LUNES_TRES_PM);
    reserva.setEstado(EstadoReserva.EXPIRADA);
    reservaRepository.saveAndFlush(reserva);
    Pago pago =
        pagoRepository.saveAndFlush(
            Pago.builder()
                .reserva(reserva)
                .referencia("GS-TARDE-1")
                .montoCentavos(250_000L)
                .build());

    registroResultadoPago.registrar(pago, EstadoPago.APROBADO, "tx-2", "CARD", null);

    assertTrue(
        notificacionRepository.findByReservaIdOrderByProgramadaParaAsc(reserva.getId()).isEmpty());
  }

  // Criterio 2: solo se envían las que ya deben salir; quedan marcadas como enviadas.

  @Test
  @DisplayName("CP-34f Solo se envían las notificaciones cuya hora llegó, y quedan como enviadas")
  void soloSeEnvianLasQueYaLlegaron() {
    Reserva reserva = reservaConfirmada(LUNES_TRES_PM);
    Instant ahora = LUNES_TRES_PM.minus(3, ChronoUnit.HOURS);
    Notificacion confirmacion =
        notificacion(reserva, TipoNotificacion.CONFIRMACION, ahora.minus(5, ChronoUnit.MINUTES));
    Notificacion recordatorio =
        notificacion(
            reserva, TipoNotificacion.RECORDATORIO, LUNES_TRES_PM.minus(1, ChronoUnit.HOURS));

    assertEquals(1, notificacionService.enviarPendientes(ahora));

    verify(enviadorCorreo)
        .enviar(
            eq("laura@gamespot.test"),
            eq("Tu reserva en Game Spot está confirmada"),
            contains("Equipo: Xbox 360 #1"));
    Notificacion enviada = leer(confirmacion);
    assertTrue(enviada.isFueEnviada());
    assertNotNull(enviada.getEnviadaEn());
    assertFalse(leer(recordatorio).isFueEnviada());

    // Una hora antes del inicio sale el recordatorio, y la confirmación no se repite.
    assertEquals(1, notificacionService.enviarPendientes(LUNES_TRES_PM.minus(1, ChronoUnit.HOURS)));
    verify(enviadorCorreo)
        .enviar(
            eq("laura@gamespot.test"),
            eq("Tu turno en Game Spot empieza en 1 hora"),
            contains("Saldo a pagar en el local: $2.500"));
    verify(enviadorCorreo, times(2)).enviar(anyString(), anyString(), anyString());
    assertTrue(leer(recordatorio).isFueEnviada());
  }

  // Criterio 3: si el envío falla, se cuenta el intento, se guarda el error y se reintenta.

  @Test
  @DisplayName("CP-34g Si el envío falla, suma un intento, guarda el error y se reintenta")
  void envioFallidoSeReintenta() {
    Reserva reserva = reservaConfirmada(LUNES_TRES_PM);
    Instant ahora = LUNES_TRES_PM.minus(3, ChronoUnit.HOURS);
    Notificacion confirmacion = notificacion(reserva, TipoNotificacion.CONFIRMACION, ahora);
    doThrow(new MailSendException("Servidor SMTP no disponible"))
        .doNothing()
        .when(enviadorCorreo)
        .enviar(anyString(), anyString(), anyString());

    assertEquals(0, notificacionService.enviarPendientes(ahora));
    Notificacion fallida = leer(confirmacion);
    assertFalse(fallida.isFueEnviada());
    assertNull(fallida.getEnviadaEn());
    assertEquals(1, fallida.getIntentos());
    assertTrue(fallida.getUltimoError().contains("Servidor SMTP no disponible"));

    assertEquals(1, notificacionService.enviarPendientes(ahora.plus(1, ChronoUnit.MINUTES)));
    Notificacion reintentada = leer(confirmacion);
    assertTrue(reintentada.isFueEnviada());
    assertEquals(1, reintentada.getIntentos());
  }

  // Criterio 4: el recordatorio de una reserva cancelada no se envía.

  @Test
  @DisplayName("CP-34h El recordatorio de una reserva cancelada no se envía")
  void recordatorioDeReservaCanceladaNoSeEnvia() {
    Reserva reserva = reservaConfirmada(LUNES_TRES_PM);
    Instant horaRecordatorio = LUNES_TRES_PM.minus(1, ChronoUnit.HOURS);
    Notificacion recordatorio =
        notificacion(reserva, TipoNotificacion.RECORDATORIO, horaRecordatorio);
    reserva.setEstado(EstadoReserva.CANCELADA);
    reserva.setCanceladaEn(horaRecordatorio.minus(1, ChronoUnit.DAYS));
    reserva.setMotivoCancelacion("El cliente pidió cancelar");
    reservaRepository.saveAndFlush(reserva);

    assertEquals(0, notificacionService.enviarPendientes(horaRecordatorio));

    verify(enviadorCorreo, never()).enviar(anyString(), anyString(), anyString());
    assertFalse(leer(recordatorio).isFueEnviada());
  }

  // Criterio 5: ninguna credencial de correo queda en el código.

  @Test
  @DisplayName("CP-34i Las credenciales de correo solo llegan por variables de entorno")
  void credencialesSoloPorVariablesDeEntorno() throws Exception {
    String configuracion;
    try (InputStream yaml = getClass().getResourceAsStream("/application.yaml")) {
      configuracion = new String(yaml.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertTrue(configuracion.contains("username: \"${CORREO_USUARIO:}\""));
    assertTrue(configuracion.contains("password: \"${CORREO_CONTRASENA_APLICACION:}\""));

    // Sin las variables definidas, no hay ningún valor por defecto escondido.
    assertEquals("", environment.getProperty("spring.mail.password"));
  }
}
