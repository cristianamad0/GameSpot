package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoPago;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Pago;
import com.usta.gamespot.model.Reserva;
import com.usta.gamespot.model.Usuario;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.PagoRepository;
import com.usta.gamespot.repository.ReservaRepository;
import com.usta.gamespot.repository.UsuarioRepository;
import com.usta.gamespot.service.ExpiracionReservasService;
import com.usta.gamespot.tarea.ExpiracionReservasTarea;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pruebas de la expiración de reservas no pagadas (HU-05, CACS-12). Cada prueba corre en una
 * transacción que se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class ExpiracionReservasTests {

  /** 10 de octubre de 2026, 15:00 en Bogotá (UTC-5). */
  private static final Instant LAS_TRES_PM = Instant.parse("2026-10-10T20:00:00Z");

  @Autowired private ExpiracionReservasService expiracionReservasService;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private EquipoRepository equipoRepository;
  @Autowired private ReservaRepository reservaRepository;
  @Autowired private PagoRepository pagoRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private ApplicationContext contexto;

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

  /** Reserva pendiente de 1 hora cuyo plazo de pago vence en {@code minutosParaVencer}. */
  private Reserva reservaPendiente(Instant inicio, long minutosParaVencer) {
    return reservaRepository.saveAndFlush(
        Reserva.builder()
            .usuario(cliente)
            .equipo(equipo)
            .inicio(inicio)
            .fin(inicio.plus(60, ChronoUnit.MINUTES))
            .precioTotalPesos(5000)
            .adelantoPesos(2000)
            .expiraEn(Instant.now().plus(minutosParaVencer, ChronoUnit.MINUTES))
            .build());
  }

  @Test
  @DisplayName("CP-21a La reserva con el plazo vencido pasa a EXPIRADA y libera el bloque")
  void reservaVencidaExpiraYSuBloqueSePuedeReservar() {
    Reserva vencida = reservaPendiente(LAS_TRES_PM, -1);

    int expiradas = expiracionReservasService.expirarVencidas();
    entityManager.refresh(vencida);

    assertEquals(1, expiradas);
    assertEquals(EstadoReserva.EXPIRADA, vencida.getEstado());
    assertDoesNotThrow(() -> reservaPendiente(LAS_TRES_PM, 15));
  }

  @Test
  @DisplayName("CP-21b La reserva con el plazo vigente sigue en PENDIENTE_PAGO")
  void reservaVigenteSiguePendiente() {
    Reserva vigente = reservaPendiente(LAS_TRES_PM, 1);

    int expiradas = expiracionReservasService.expirarVencidas();
    entityManager.refresh(vigente);

    assertEquals(0, expiradas);
    assertEquals(EstadoReserva.PENDIENTE_PAGO, vigente.getEstado());
  }

  @Test
  @DisplayName("CP-21c La reserva CONFIRMADA nunca expira, aunque su plazo haya vencido")
  void reservaConfirmadaNuncaExpira() {
    Reserva confirmada = reservaPendiente(LAS_TRES_PM, 15);
    pagoRepository.saveAndFlush(
        Pago.builder()
            .reserva(confirmada)
            .referencia("REF-PRUEBA-EXPIRACION")
            .montoCentavos(200_000L)
            .estado(EstadoPago.APROBADO)
            .build());
    entityManager.refresh(confirmada);
    assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
    // Caso extremo: aunque quede con un plazo ya vencido, no debe expirar.
    confirmada.setExpiraEn(Instant.now().minus(1, ChronoUnit.MINUTES));
    reservaRepository.saveAndFlush(confirmada);

    int expiradas = expiracionReservasService.expirarVencidas();
    entityManager.refresh(confirmada);

    assertEquals(0, expiradas);
    assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
  }

  @Test
  @DisplayName("CP-21d La tarea programada está apagada en la configuración de pruebas")
  void tareaApagadaEnPruebas() {
    assertTrue(contexto.getBeansOfType(ExpiracionReservasTarea.class).isEmpty());
  }
}
