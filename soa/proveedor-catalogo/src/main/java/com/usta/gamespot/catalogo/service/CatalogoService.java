package com.usta.gamespot.catalogo.service;

import com.usta.gamespot.catalogo.dto.EquipoRequest;
import com.usta.gamespot.catalogo.dto.EquipoResponse;
import com.usta.gamespot.catalogo.dto.HorarioResponse;
import com.usta.gamespot.catalogo.dto.ModalidadResponse;
import com.usta.gamespot.catalogo.dto.TarifaRequest;
import com.usta.gamespot.catalogo.exception.ConflictoException;
import com.usta.gamespot.catalogo.exception.RecursoNoEncontradoException;
import com.usta.gamespot.catalogo.model.Equipo;
import com.usta.gamespot.catalogo.model.Modalidad;
import com.usta.gamespot.catalogo.repository.EquipoRepository;
import com.usta.gamespot.catalogo.repository.HorarioAtencionRepository;
import com.usta.gamespot.catalogo.repository.ModalidadRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CACS-25: lógica del catálogo. El controlador solo valida y delega; aquí se aplican las reglas y
 * se usan los repositorios.
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

  private final EquipoRepository equipoRepository;
  private final ModalidadRepository modalidadRepository;
  private final HorarioAtencionRepository horarioRepository;

  /**
   * Lista todos los equipos.
   *
   * @return equipos del más económico al más costoso
   */
  @Transactional(readOnly = true)
  public List<EquipoResponse> listarEquipos() {
    return equipoRepository.findAllByOrderByModalidadTarifaHoraPesosAscNombreAsc().stream()
        .map(EquipoResponse::de)
        .toList();
  }

  /**
   * Busca un equipo.
   *
   * @param id id del equipo
   * @return el equipo
   * @throws RecursoNoEncontradoException si no existe
   */
  @Transactional(readOnly = true)
  public EquipoResponse obtenerEquipo(Long id) {
    return EquipoResponse.de(buscarEquipo(id));
  }

  /**
   * Crea un equipo.
   *
   * @param request nombre, modalidad y estado
   * @return el equipo creado
   * @throws ConflictoException si ya existe un equipo con ese nombre
   * @throws RecursoNoEncontradoException si la modalidad no existe
   */
  @Transactional
  public EquipoResponse crearEquipo(EquipoRequest request) {
    String nombre = request.nombre().trim();
    if (equipoRepository.existsByNombreIgnoreCase(nombre)) {
      throw new ConflictoException("Ya existe un equipo llamado " + nombre);
    }
    Equipo equipo =
        Equipo.builder()
            .nombre(nombre)
            .modalidad(buscarModalidad(request.modalidadId()))
            .estaDisponible(request.estaDisponible())
            .build();
    return EquipoResponse.de(equipoRepository.save(equipo));
  }

  /**
   * Actualiza todos los datos de un equipo.
   *
   * @param id id del equipo
   * @param request nombre, modalidad y estado nuevos
   * @return el equipo actualizado
   * @throws RecursoNoEncontradoException si el equipo o la modalidad no existen
   * @throws ConflictoException si otro equipo ya tiene ese nombre
   */
  @Transactional
  public EquipoResponse actualizarEquipo(Long id, EquipoRequest request) {
    Equipo equipo = buscarEquipo(id);
    String nombre = request.nombre().trim();
    if (equipoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
      throw new ConflictoException("Ya existe otro equipo llamado " + nombre);
    }
    equipo.setNombre(nombre);
    equipo.setModalidad(buscarModalidad(request.modalidadId()));
    equipo.setEstaDisponible(request.estaDisponible());
    return EquipoResponse.de(equipoRepository.save(equipo));
  }

  /**
   * Elimina un equipo del catálogo. Las reservas que ya tenga en el proveedor de reservas
   * conservan su nombre y su precio, porque se guardaron al reservar.
   *
   * @param id id del equipo
   * @throws RecursoNoEncontradoException si no existe
   */
  @Transactional
  public void eliminarEquipo(Long id) {
    equipoRepository.delete(buscarEquipo(id));
  }

  /**
   * Lista las modalidades.
   *
   * @return modalidades de la más económica a la más costosa
   */
  @Transactional(readOnly = true)
  public List<ModalidadResponse> listarModalidades() {
    return modalidadRepository.findAllByOrderByTarifaHoraPesosAscNombreAsc().stream()
        .map(ModalidadResponse::de)
        .toList();
  }

  /**
   * Cambia la tarifa de una modalidad y si se ofrece.
   *
   * @param id id de la modalidad
   * @param request tarifa nueva y estado
   * @return la modalidad actualizada
   * @throws RecursoNoEncontradoException si no existe
   */
  @Transactional
  public ModalidadResponse actualizarTarifa(Long id, TarifaRequest request) {
    Modalidad modalidad = buscarModalidad(id);
    modalidad.setTarifaHoraPesos(request.tarifaHoraPesos());
    modalidad.setEstaActiva(request.estaActiva());
    return ModalidadResponse.de(modalidadRepository.save(modalidad));
  }

  /**
   * Horario de atención de la semana.
   *
   * @return un horario por día, de lunes a domingo
   */
  @Transactional(readOnly = true)
  public List<HorarioResponse> listarHorarios() {
    return horarioRepository.findAllByOrderByDiaSemanaAsc().stream()
        .map(HorarioResponse::de)
        .toList();
  }

  private Equipo buscarEquipo(Long id) {
    return equipoRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("el equipo", id));
  }

  private Modalidad buscarModalidad(Long id) {
    return modalidadRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("la modalidad", id));
  }
}
