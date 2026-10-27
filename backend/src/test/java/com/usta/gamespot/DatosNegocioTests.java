package com.usta.gamespot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.usta.gamespot.model.ConfiguracionNegocio;
import com.usta.gamespot.model.Equipo;
import com.usta.gamespot.model.HorarioAtencion;
import com.usta.gamespot.repository.ConfiguracionNegocioRepository;
import com.usta.gamespot.repository.EquipoRepository;
import com.usta.gamespot.repository.HorarioAtencionRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
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
 * Pruebas de los datos reales del negocio que carga esquema.sql (entregados por el propietario el
 * 8/10/2026). Cada prueba corre en una transacción que se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "app.cli.registration.enabled=false")
@Transactional
class DatosNegocioTests {

  @Autowired private EquipoRepository equipoRepository;
  @Autowired private HorarioAtencionRepository horarioAtencionRepository;
  @Autowired private ConfiguracionNegocioRepository configuracionNegocioRepository;

  @Test
  @DisplayName("CP-17 Están los 6 equipos reales, cada uno en su modalidad")
  void estanLosSeisEquiposReales() {
    Map<String, String> modalidadPorEquipo =
        equipoRepository.findAll().stream()
            .collect(
                Collectors.toMap(
                    Equipo::getNombre,
                    e -> e.getModalidad().getNombre(),
                    (a, b) -> a,
                    TreeMap::new));

    assertEquals(
        Map.of(
            "Xbox 360 #1", "Xbox 360",
            "Xbox Series S #1", "Xbox Series S",
            "Xbox Series S #2", "Xbox Series S",
            "Xbox Series S #3", "Xbox Series S",
            "Xbox Series S #4", "Xbox Series S",
            "Sala VIP Xbox Series X", "Sala VIP Xbox Series X"),
        modalidadPorEquipo);
  }

  @ParameterizedTest(name = "día {0}: de {1} a {2}")
  @DisplayName("CP-18 El horario de atención de cada día es el del propietario")
  @CsvSource({
    "1, 14:00, 22:00",
    "2, 14:00, 22:00",
    "3, 14:00, 22:00",
    "4, 14:00, 22:00",
    "5, 14:00, 22:00",
    "6, 15:00, 21:00",
    "7, 15:00, 21:00"
  })
  void horarioDeAtencionPorDia(short diaSemana, LocalTime apertura, LocalTime cierre) {
    HorarioAtencion horario = horarioAtencionRepository.findByDiaSemana(diaSemana).orElseThrow();

    assertEquals(apertura, horario.getHoraApertura());
    assertEquals(cierre, horario.getHoraCierre());
  }

  @Test
  @DisplayName("CP-18 Hay un horario para cada uno de los 7 días")
  void hayHorarioParaLosSieteDias() {
    List<HorarioAtencion> horarios = horarioAtencionRepository.findAll();

    assertEquals(7, horarios.size());
  }

  @Test
  @DisplayName("CP-19 La configuración es adelanto del 50 %, 1 punto por hora y 15 minutos")
  void configuracionDelNegocio() {
    ConfiguracionNegocio configuracion = configuracionNegocioRepository.obtener();

    assertEquals(50, configuracion.getPorcentajeAdelanto());
    assertEquals(1, configuracion.getPuntosPorHora());
    assertEquals(15, configuracion.getMinutosEsperaPago());
  }

  @ParameterizedTest(name = "porcentaje {0} -> permitido={1}")
  @DisplayName("CP-20 El porcentaje de adelanto va de 1 a 100 (valores límite)")
  @CsvSource({"0, false", "1, true", "100, true", "101, false"})
  void porcentajeDeAdelantoEntreUnoYCien(int porcentaje, boolean esPermitido) {
    ConfiguracionNegocio configuracion = configuracionNegocioRepository.obtener();
    configuracion.setPorcentajeAdelanto(porcentaje);

    if (esPermitido) {
      assertDoesNotThrow(() -> configuracionNegocioRepository.saveAndFlush(configuracion));
    } else {
      assertThrows(
          DataIntegrityViolationException.class,
          () -> configuracionNegocioRepository.saveAndFlush(configuracion));
    }
  }
}
