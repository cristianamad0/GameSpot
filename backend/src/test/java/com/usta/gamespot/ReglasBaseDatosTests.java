package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.MovimientoPuntos;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.TipoMovimientoPuntos;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.ConfiguracionNegocioRepository;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.MovimientoPuntosRepository;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de las reglas de negocio que garantiza la base de datos (HU-02, HU-03 y HU-04).
 * Cada prueba corre en una transacción que se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class ReglasBaseDatosTests {

  /** 10 de octubre de 2026, 15:00 en Bogotá (UTC-5). */
  private static final Instant LAS_TRES_PM = Instant.parse("2026-10-10T20:00:00Z");

  private static final long ADELANTO_CENTAVOS = 200_000L;

  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private PagoRepository pagoRepository;
  @Autowired private MovimientoPuntosRepository movimientoPuntosRepository;
  @Autowired private ConfiguracionNegocioRepository configuracionNegocioRepository;
  @Autowired private EntityManager entityManager;

  private Usuario cliente;
  private Equipo equipo;

  @BeforeEach
  void prepararDatos() {
    cliente =
        usuarioRepository.saveAndFlush(
            Usuario.builder()
                .nombre("Cliente Prueba")
                .email("cliente@gamespot.test")
                .passwordHash("hash-de-prueba")
                .build());
    equipo = equipoRepository.findAll().getFirst();
  }

  private Reserva nuevaReserva(Instant inicio, int duracionMinutos) {
    return Reserva.builder()
        .usuario(cliente)
        .equipo(equipo)
        .inicio(inicio)
        .fin(inicio.plus(duracionMinutos, ChronoUnit.MINUTES))
        .precioTotalPesos(5000)
        .adelantoPesos(2000)
        .expiraEn(Instant.now().plus(15, ChronoUnit.MINUTES))
        .build();
  }

  private void registrarPago(Reserva reserva, String referencia, EstadoPago estado, long monto) {
    pagoRepository.saveAndFlush(
        Pago.builder()
            .reserva(reserva)
            .referencia(referencia)
            .montoCentavos(monto)
            .estado(estado)
            .build());
    // El trigger pudo cambiar la reserva en la base de datos; se vuelve a leer.
    entityManager.refresh(reserva);
  }

  private void pagarAdelanto(Reserva reserva, String referencia) {
    registrarPago(reserva, referencia, EstadoPago.APROBADO, ADELANTO_CENTAVOS);
  }

  private MovimientoPuntos ajusteDePuntos(int puntos) {
    return MovimientoPuntos.builder()
        .usuario(cliente)
        .puntos(puntos)
        .tipo(TipoMovimientoPuntos.AJUSTE_ADMIN)
        .build();
  }

  // ---------------------------------------------------------------------------------------------
  // HU-02: reservas por bloques de 30 minutos
  // ---------------------------------------------------------------------------------------------

  @ParameterizedTest(name = "inicio +{0} min, duración {1} min -> válida={2}")
  @DisplayName("CP-08 Alineación y duración de los bloques (valores límite)")
  @CsvSource({
    "0, 30, true",
    "30, 30, true",
    "29, 30, false",
    "31, 30, false",
    "0, 0, false",
    "0, 45, false",
    "0, 60, true"
  })
  void soloSeAceptanBloquesDeTreintaMinutos(
      int desplazamientoMinutos, int duracionMinutos, boolean esValida) {
    Reserva reserva =
        nuevaReserva(LAS_TRES_PM.plus(desplazamientoMinutos, ChronoUnit.MINUTES), duracionMinutos);

    if (esValida) {
      assertDoesNotThrow(() -> reservaRepository.saveAndFlush(reserva));
    } else {
      assertThrows(
          DataIntegrityViolationException.class, () -> reservaRepository.saveAndFlush(reserva));
    }
  }

  @Test
  @DisplayName("CP-09 No permite reservar un bloque que se cruza con otro")
  void noPermiteReservarUnBloqueQueSeCruzaConOtro() {
    reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));

    Reserva queSeCruza = nuevaReserva(LAS_TRES_PM.plus(30, ChronoUnit.MINUTES), 60);

    assertThrows(
        DataIntegrityViolationException.class, () -> reservaRepository.saveAndFlush(queSeCruza));
  }

  @Test
  @DisplayName("CP-10 Permite reservas seguidas sin cruce (valor límite)")
  void permiteReservasSeguidasSinCruce() {
    reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));

    Reserva siguiente = nuevaReserva(LAS_TRES_PM.plus(60, ChronoUnit.MINUTES), 60);

    assertDoesNotThrow(() -> reservaRepository.saveAndFlush(siguiente));
  }

  @Test
  @DisplayName("CP-11 Permite el mismo horario en otro equipo")
  void permiteElMismoHorarioEnOtroEquipo() {
    reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));
    Equipo otroEquipo =
        equipoRepository.findAll().stream()
            .filter(e -> !e.getId().equals(equipo.getId()))
            .findFirst()
            .orElseThrow();

    Reserva enOtroEquipo = nuevaReserva(LAS_TRES_PM, 60);
    enOtroEquipo.setEquipo(otroEquipo);

    assertDoesNotThrow(() -> reservaRepository.saveAndFlush(enOtroEquipo));
  }

  // ---------------------------------------------------------------------------------------------
  // HU-03: pago de adelanto
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("CP-12 No permite confirmar una reserva sin adelanto pagado")
  void noPermiteConfirmarUnaReservaSinAdelantoPagado() {
    Reserva reserva = reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));
    reserva.setEstado(EstadoReserva.CONFIRMADA);
    reserva.setExpiraEn(null);

    assertThrows(
        DataIntegrityViolationException.class, () -> reservaRepository.saveAndFlush(reserva));
  }

  @ParameterizedTest(name = "pago {0} por {1} centavos -> reserva {2}")
  @DisplayName("CP-13 Resultado del pago sobre la reserva (tabla de decisión)")
  @CsvSource({
    "APROBADO, 200000, CONFIRMADA",
    "APROBADO, 100000, PENDIENTE_PAGO",
    "RECHAZADO, 200000, PENDIENTE_PAGO",
    "PENDIENTE, 200000, PENDIENTE_PAGO"
  })
  void resultadoDelPagoDefineElEstadoDeLaReserva(
      EstadoPago estadoPago, long montoCentavos, EstadoReserva estadoEsperado) {
    Reserva reserva = reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));

    registrarPago(reserva, "REF-TD-" + estadoPago + "-" + montoCentavos, estadoPago, montoCentavos);

    assertEquals(estadoEsperado, reserva.getEstado());
  }

  @Test
  @DisplayName("CP-14 La reserva vencida expira y libera el bloque (valores límite)")
  void reservaVencidaExpiraYLiberaElBloque() {
    Reserva vencida = nuevaReserva(LAS_TRES_PM, 60);
    vencida.setExpiraEn(Instant.now().minus(1, ChronoUnit.MINUTES));
    reservaRepository.saveAndFlush(vencida);
    Reserva vigente = nuevaReserva(LAS_TRES_PM.plus(2, ChronoUnit.HOURS), 60);
    reservaRepository.saveAndFlush(vigente);

    int reservasExpiradas = reservaRepository.expirarPendientes();
    entityManager.refresh(vencida);
    entityManager.refresh(vigente);

    assertEquals(1, reservasExpiradas);
    assertEquals(EstadoReserva.EXPIRADA, vencida.getEstado());
    assertEquals(EstadoReserva.PENDIENTE_PAGO, vigente.getEstado());
    assertDoesNotThrow(() -> reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60)));
  }

  // ---------------------------------------------------------------------------------------------
  // HU-04: fidelización
  // ---------------------------------------------------------------------------------------------

  @Test
  @DisplayName("CP-15a Completar una reserva suma los puntos al cliente")
  void completarReservaSumaPuntosAlCliente() {
    Reserva reserva = reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));
    pagarAdelanto(reserva, "REF-PRUEBA-PUNTOS");

    reserva.setEstado(EstadoReserva.COMPLETADA);
    reservaRepository.saveAndFlush(reserva);
    entityManager.refresh(cliente);

    int puntosEsperados = configuracionNegocioRepository.obtener().getPuntosPorHora();
    assertEquals(puntosEsperados, cliente.getPuntosFidelidad());
  }

  @Test
  @DisplayName("CP-15b Completar dos veces la misma reserva no duplica los puntos")
  void completarDosVecesNoDuplicaLosPuntos() {
    Reserva reserva = reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));
    pagarAdelanto(reserva, "REF-PRUEBA-DUPLICADO");

    reserva.setEstado(EstadoReserva.COMPLETADA);
    reservaRepository.saveAndFlush(reserva);
    reserva.setEstado(EstadoReserva.CONFIRMADA);
    reservaRepository.saveAndFlush(reserva);
    reserva.setEstado(EstadoReserva.COMPLETADA);
    reservaRepository.saveAndFlush(reserva);
    entityManager.refresh(cliente);

    int puntosEsperados = configuracionNegocioRepository.obtener().getPuntosPorHora();
    assertEquals(puntosEsperados, cliente.getPuntosFidelidad());
  }

  @ParameterizedTest(name = "ajuste de {0} puntos sobre un saldo de 5 -> permitido={1}")
  @DisplayName("CP-16 El saldo de puntos no puede quedar negativo (valores límite)")
  @CsvSource({"-5, true", "-6, false"})
  void elSaldoDePuntosNoPuedeQuedarNegativo(int ajustePuntos, boolean esPermitido) {
    movimientoPuntosRepository.saveAndFlush(ajusteDePuntos(5));
    MovimientoPuntos ajuste = ajusteDePuntos(ajustePuntos);

    if (esPermitido) {
      movimientoPuntosRepository.saveAndFlush(ajuste);
      entityManager.refresh(cliente);
      assertEquals(0, cliente.getPuntosFidelidad());
    } else {
      assertThrows(
          DataIntegrityViolationException.class,
          () -> movimientoPuntosRepository.saveAndFlush(ajuste));
    }
  }
}
