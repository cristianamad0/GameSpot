package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.ConfiguracionNegocioRepository;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de las reglas de negocio que garantiza la base de datos (objetivos 1 y 2 del acta).
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

  private void pagarAdelanto(Reserva reserva, String referencia) {
    pagoRepository.saveAndFlush(
        Pago.builder()
            .reserva(reserva)
            .referencia(referencia)
            .montoCentavos(ADELANTO_CENTAVOS)
            .estado(EstadoPago.APROBADO)
            .build());
    // El trigger confirmó la reserva en la base de datos; se vuelve a leer.
    entityManager.refresh(reserva);
  }

  @Test
  void noPermiteReservarUnBloqueQueSeCruzaConOtro() {
    reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));

    Reserva queSeCruza = nuevaReserva(LAS_TRES_PM.plus(30, ChronoUnit.MINUTES), 60);

    assertThrows(
        DataIntegrityViolationException.class, () -> reservaRepository.saveAndFlush(queSeCruza));
  }

  @Test
  void permiteReservasSeguidasSinCruce() {
    reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));

    Reserva siguiente = nuevaReserva(LAS_TRES_PM.plus(60, ChronoUnit.MINUTES), 60);

    assertDoesNotThrow(() -> reservaRepository.saveAndFlush(siguiente));
  }

  @Test
  void noPermiteConfirmarUnaReservaSinAdelantoPagado() {
    Reserva reserva = reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));
    reserva.setEstado(EstadoReserva.CONFIRMADA);
    reserva.setExpiraEn(null);

    assertThrows(
        DataIntegrityViolationException.class, () -> reservaRepository.saveAndFlush(reserva));
  }

  @Test
  void pagoAprobadoConfirmaLaReserva() {
    Reserva reserva = reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));

    pagarAdelanto(reserva, "REF-PRUEBA-CONFIRMAR");

    assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    assertTrue(reserva.isTieneAnticipoPagado());
  }

  @Test
  void completarReservaSumaPuntosAlCliente() {
    Reserva reserva = reservaRepository.saveAndFlush(nuevaReserva(LAS_TRES_PM, 60));
    pagarAdelanto(reserva, "REF-PRUEBA-PUNTOS");

    reserva.setEstado(EstadoReserva.COMPLETADA);
    reservaRepository.saveAndFlush(reserva);
    entityManager.refresh(cliente);

    int puntosEsperados = configuracionNegocioRepository.obtener().getPuntosPorHora();
    assertEquals(puntosEsperados, cliente.getPuntosFidelidad());
  }
}
