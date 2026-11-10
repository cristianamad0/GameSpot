package com.usta.gamespot.service;

import com.usta.gamespot.dto.EquipoAdminResponse;
import com.usta.gamespot.dto.ModalidadAdminResponse;
import com.usta.gamespot.dto.ModalidadRequest;
import com.usta.gamespot.exception.EquipoNoEncontradoException;
import com.usta.gamespot.exception.ModalidadNoEncontradaException;
import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.EstadoReserva;
import com.usta.gamespot.model.Modalidad;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.ModalidadRepository;
import com.usta.gamespot.repository.ReservaRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-22 (HU-06): el administrador pone equipos fuera de servicio o los habilita, y cambia la
 * tarifa de cada modalidad o la desactiva.
 *
 * <p>Las reservas ya creadas no cambian: guardan su precio y adelanto del momento en que se
 * reservaron, y siguen activas aunque el equipo salga de servicio. ReservaService rechaza las
 * reservas nuevas de un equipo fuera de servicio o de una modalidad desactivada (409).
 */
@Service
@RequiredArgsConstructor
public class AdminEquiposService {

  /** Reservas que todavía van a ocupar el equipo. */
  private static final Set<EstadoReserva> ACTIVAS =
      EnumSet.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA);

  private final EquipoRepository equipoRepository;
  private final ModalidadRepository modalidadRepository;
  private final ReservaRepository reservaRepository;

  /**
   * Lista todos los equipos con su modalidad, tarifa, estado y reservas activas futuras.
   *
   * @return los equipos, del más económico al más costoso
   */
  @Transactional(readOnly = true)
  public List<EquipoAdminResponse> listarEquipos() {
    Instant ahora = Instant.now();
    return equipoRepository.buscarConModalidad().stream()
        .map(equipo -> EquipoAdminResponse.de(equipo, contarActivasFuturas(equipo, ahora)))
        .toList();
  }

  /**
   * Pone un equipo fuera de servicio o lo vuelve a habilitar. Sus reservas existentes no cambian.
   *
   * @param equipoId id del equipo
   * @param estaDisponible true para habilitarlo; false para ponerlo fuera de servicio
   * @return el equipo actualizado, con cuántas reservas activas futuras tiene
   * @throws EquipoNoEncontradoException si el equipo no existe
   */
  @Transactional
  public EquipoAdminResponse cambiarDisponibilidad(Long equipoId, boolean estaDisponible) {
    Equipo equipo =
        equipoRepository
            .findById(equipoId)
            .orElseThrow(() -> new EquipoNoEncontradoException(equipoId));
    equipo.setEstaDisponible(estaDisponible);
    equipoRepository.saveAndFlush(equipo);
    return EquipoAdminResponse.de(equipo, contarActivasFuturas(equipo, Instant.now()));
  }

  /**
   * Lista las modalidades con su tarifa y cuántos equipos tienen.
   *
   * @return las modalidades, de la más económica a la más costosa
   */
  @Transactional(readOnly = true)
  public List<ModalidadAdminResponse> listarModalidades() {
    return modalidadRepository.findAll().stream()
        .sorted(
            Comparator.comparing(Modalidad::getTarifaHoraPesos)
                .thenComparing(Modalidad::getNombre))
        .map(m -> ModalidadAdminResponse.de(m, equipoRepository.countByModalidadId(m.getId())))
        .toList();
  }

  /**
   * Cambia la tarifa por hora de una modalidad y la activa o desactiva. Solo afecta a las reservas
   * que se creen desde ahora.
   *
   * @param modalidadId id de la modalidad
   * @param request nueva tarifa (ya validada como positiva) y si está activa
   * @return la modalidad actualizada
   * @throws ModalidadNoEncontradaException si la modalidad no existe
   */
  @Transactional
  public ModalidadAdminResponse actualizarModalidad(Long modalidadId, ModalidadRequest request) {
    Modalidad modalidad =
        modalidadRepository
            .findById(modalidadId)
            .orElseThrow(() -> new ModalidadNoEncontradaException(modalidadId));
    modalidad.setTarifaHoraPesos(request.tarifaHoraPesos());
    modalidad.setEstaActiva(request.estaActiva());
    modalidadRepository.saveAndFlush(modalidad);
    return ModalidadAdminResponse.de(modalidad, equipoRepository.countByModalidadId(modalidadId));
  }

  private long contarActivasFuturas(Equipo equipo, Instant ahora) {
    return reservaRepository.countByEquipoIdAndEstadoInAndInicioAfter(
        equipo.getId(), ACTIVAS, ahora);
  }
}
