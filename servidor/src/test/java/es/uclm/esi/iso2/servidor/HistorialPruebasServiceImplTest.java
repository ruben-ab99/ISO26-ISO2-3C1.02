package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.historial.HistorialPruebaDTO;
import es.uclm.sescam.contratos.informes.EstudioImagenDTO;
import es.uclm.sescam.contratos.informes.IInformesImagenesService;
import es.uclm.sescam.contratos.informes.IRegistroPruebaService;
import es.uclm.sescam.contratos.informes.InformeDTO;
import es.uclm.sescam.contratos.informes.RegistroPruebaDTO;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IAuditService;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-03 (Consultar historial de pruebas realizadas).
 * Técnicas: clases de equivalencia (paciente con / sin pruebas, prueba propia / ajena /
 * inexistente), valores límite del rango de fechas (extremos incluidos; fin un día anterior al
 * inicio no válido), criterio each use entre parámetros y conjetura de errores (mayúsculas en el
 * filtro, auditoría opcional). Los servicios colaboradores se sustituyen por stubs (ADR-012).
 */
class HistorialPruebasServiceImplTest {

    private Map<String, RegistroPruebaDTO> registrosStub;
    private List<EstudioImagenDTO> estudiosStub;
    private List<AuditLogDTO> eventosAuditoria;
    private HistorialPruebasServiceImpl servicio;

    // ---------- Stubs inyectados por constructor (ADR-012) ----------

    private class StubRegistroPrueba implements IRegistroPruebaService {
        @Override
        public RegistroPruebaDTO registrarRealizacionPrueba(String citaId, String pacienteId, String medicoId,
                                                            String tipoPrueba, String observaciones, String incidencias) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RegistroPruebaDTO consultarRegistroPrueba(String registroId) {
            RegistroPruebaDTO registro = registrosStub.get(registroId);
            if (registro == null) {
                throw new IllegalStateException("No existe ningún registro de prueba con ID: " + registroId);
            }
            return registro;
        }

        @Override
        public List<RegistroPruebaDTO> listarRegistrosPaciente(String pacienteId) {
            List<RegistroPruebaDTO> resultado = new ArrayList<>();
            for (RegistroPruebaDTO r : registrosStub.values()) {
                if (pacienteId.equals(r.getPacienteId())) {
                    resultado.add(r);
                }
            }
            return resultado;
        }
    }

    private class StubInformesImagenes implements IInformesImagenesService {
        @Override
        public InformeDTO crearInforme(String solicitudId, String pacienteId, String medicoId, String contenidoClinico) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InformeDTO consultarInforme(String informeId, String usuarioId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EstudioImagenDTO> listarEstudiosPaciente(String pacienteId) {
            List<EstudioImagenDTO> resultado = new ArrayList<>();
            for (EstudioImagenDTO e : estudiosStub) {
                if (pacienteId.equals(e.getPacienteId())) {
                    resultado.add(e);
                }
            }
            return resultado;
        }
    }

    private class StubAudit implements IAuditService {
        @Override
        public void registrarEvento(String usuario, String accion, boolean exito, String ip) {
            eventosAuditoria.add(new AuditLogDTO(usuario, accion, exito, ip));
        }

        @Override
        public List<AuditLogDTO> obtenerTrazas() {
            return eventosAuditoria;
        }
    }

    @BeforeEach
    void setUp() {
        registrosStub = new LinkedHashMap<>();
        estudiosStub = new ArrayList<>();
        eventosAuditoria = new ArrayList<>();

        registrosStub.put("REG-001", new RegistroPruebaDTO("REG-001", "CITA-1", "PAC-001", "MED-010",
                "RX Tórax", "", "", true, LocalDateTime.of(2026, 9, 10, 10, 0)));
        registrosStub.put("REG-002", new RegistroPruebaDTO("REG-002", "CITA-2", "PAC-001", "MED-010",
                "TAC Abdominal", "", "Paciente con claustrofobia", true, LocalDateTime.of(2026, 11, 5, 9, 30)));
        registrosStub.put("REG-003", new RegistroPruebaDTO("REG-003", "CITA-3", "PAC-001", "MED-020",
                "RX Tórax", "", "", true, LocalDateTime.of(2026, 10, 1, 12, 0)));
        registrosStub.put("REG-004", new RegistroPruebaDTO("REG-004", "CITA-4", "PAC-001", "MED-020",
                "RM Craneal", "", "", false, null));
        registrosStub.put("REG-999", new RegistroPruebaDTO("REG-999", "CITA-9", "PAC-999", "MED-030",
                "RM Craneal", "", "", true, LocalDateTime.of(2026, 10, 20, 8, 0)));

        estudiosStub.add(new EstudioImagenDTO("EST-1", "PAC-001", "TAC",
                LocalDate.of(2026, 11, 5), "pacs://est-1"));

        servicio = new HistorialPruebasServiceImpl(new StubRegistroPrueba(), new StubInformesImagenes(), new StubAudit());
    }

    private static final String MSG_PACIENTE = "El identificador del paciente es obligatorio";

    // ---------------------------------------------------------------- constructor

    @Test
    @DisplayName("El constructor rechaza un servicio de registro nulo")
    void constructorConRegistroNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new HistorialPruebasServiceImpl(null, new StubInformesImagenes(), new StubAudit()));

        assertEquals("El servicio de registro de pruebas es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("El constructor rechaza un servicio de informes nulo")
    void constructorConInformesNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new HistorialPruebasServiceImpl(new StubRegistroPrueba(), null, new StubAudit()));

        assertEquals("El servicio de informes e imágenes es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- consultarHistorial

    @Test
    @DisplayName("Devuelve solo las pruebas realizadas del paciente, de la más reciente a la más antigua")
    void consultarHistorialDevuelveRealizadasOrdenadas() {
        List<HistorialPruebaDTO> historial = servicio.consultarHistorial("PAC-001");

        assertAll(
                () -> assertEquals(3, historial.size()),
                () -> assertEquals("REG-002", historial.get(0).getRegistroId()),
                () -> assertEquals("REG-003", historial.get(1).getRegistroId()),
                () -> assertEquals("REG-001", historial.get(2).getRegistroId()),
                () -> assertTrue(historial.stream().allMatch(p -> "PAC-001".equals(p.getPacienteId()))));
    }

    @Test
    @DisplayName("Una prueba pendiente (REG-004) no aparece en el historial")
    void consultarHistorialExcluyeLasPruebasNoRealizadas() {
        List<HistorialPruebaDTO> historial = servicio.consultarHistorial("PAC-001");

        assertTrue(historial.stream().noneMatch(p -> "REG-004".equals(p.getRegistroId())));
    }

    @Test
    @DisplayName("Marca las pruebas que tienen estudio de imagen asociado")
    void consultarHistorialIndicaEstudioDeImagen() {
        List<HistorialPruebaDTO> historial = servicio.consultarHistorial("PAC-001");

        assertAll(
                () -> assertTrue(historial.get(0).isTieneEstudioImagen()),
                () -> assertFalse(historial.get(1).isTieneEstudioImagen()),
                () -> assertEquals("Paciente con claustrofobia", historial.get(0).getIncidencias()));
    }

    @Test
    @DisplayName("Un paciente sin pruebas obtiene una lista vacía")
    void consultarHistorialSinPruebasDevuelveListaVacia() {
        assertTrue(servicio.consultarHistorial("PAC-NUEVO").isEmpty());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Consultar con paciente nulo, vacío o en blanco lanza excepción")
    void consultarHistorialConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarHistorial(paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    @Test
    @DisplayName("Cada consulta del historial queda registrada en auditoría con el paciente sin espacios")
    void consultarHistorialRegistraAuditoria() {
        servicio.consultarHistorial(" PAC-001 ");

        assertAll(
                () -> assertEquals(1, eventosAuditoria.size()),
                () -> assertEquals("PAC-001", eventosAuditoria.get(0).getUsuario()),
                () -> assertEquals("CONSULTAR_HISTORIAL_PRUEBAS", eventosAuditoria.get(0).getAccion()),
                () -> assertTrue(eventosAuditoria.get(0).isExito()));
    }

    @Test
    @DisplayName("Funciona sin servicio de auditoría (es opcional)")
    void consultarHistorialSinAuditoriaFunciona() {
        HistorialPruebasServiceImpl sinAudit =
                new HistorialPruebasServiceImpl(new StubRegistroPrueba(), new StubInformesImagenes(), null);

        assertEquals(3, sinAudit.consultarHistorial("PAC-001").size());
    }

    // ---------------------------------------------------------------- consultarHistorialPorTipo

    @ParameterizedTest(name = "el filtro \"{0}\" devuelve las 2 radiografías")
    @ValueSource(strings = {"RX Tórax", "rx tórax", "RX TÓRAX"})
    @DisplayName("Filtrar por tipo de prueba no distingue mayúsculas")
    void consultarHistorialPorTipoFiltraSinDistinguirMayusculas(String tipo) {
        List<HistorialPruebaDTO> rx = servicio.consultarHistorialPorTipo("PAC-001", tipo);

        assertAll(
                () -> assertEquals(2, rx.size()),
                () -> assertTrue(rx.stream().allMatch(p -> "RX Tórax".equals(p.getTipoPrueba()))));
    }

    @Test
    @DisplayName("Filtrar por un tipo sin pruebas devuelve una lista vacía")
    void consultarHistorialPorTipoSinCoincidenciasDevuelveListaVacia() {
        assertTrue(servicio.consultarHistorialPorTipo("PAC-001", "Ecografía").isEmpty());
    }

    @ParameterizedTest(name = "tipo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Filtrar con tipo nulo, vacío o en blanco lanza excepción")
    void consultarHistorialPorTipoSinTipoLanzaExcepcion(String tipo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarHistorialPorTipo("PAC-001", tipo));

        assertEquals("El tipo de prueba es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- consultarHistorialPorFechas

    @Test
    @DisplayName("Filtrar por rango de fechas incluye ambos extremos")
    void consultarHistorialPorFechasIncluyeAmbosExtremos() {
        List<HistorialPruebaDTO> octubreNoviembre = servicio.consultarHistorialPorFechas(
                "PAC-001", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 5));

        assertAll(
                () -> assertEquals(2, octubreNoviembre.size()),
                () -> assertEquals("REG-002", octubreNoviembre.get(0).getRegistroId()),
                () -> assertEquals("REG-003", octubreNoviembre.get(1).getRegistroId()));
    }

    @Test
    @DisplayName("Un rango de un solo día (inicio igual a fin) es válido")
    void consultarHistorialPorFechasDeUnSoloDiaEsValido() {
        LocalDate dia = LocalDate.of(2026, 11, 5);

        List<HistorialPruebaDTO> resultado = servicio.consultarHistorialPorFechas("PAC-001", dia, dia);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Un rango sin pruebas devuelve una lista vacía")
    void consultarHistorialPorFechasSinPruebasDevuelveListaVacia() {
        List<HistorialPruebaDTO> resultado = servicio.consultarHistorialPorFechas(
                "PAC-001", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Filtrar sin fecha de inicio o sin fecha de fin lanza excepción")
    void consultarHistorialPorFechasNulasLanzaExcepcion() {
        LocalDate hoy = LocalDate.of(2026, 11, 23);
        String mensaje = "Las fechas de inicio y fin son obligatorias";

        assertAll(
                () -> assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                        () -> servicio.consultarHistorialPorFechas("PAC-001", null, hoy)).getMessage()),
                () -> assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                        () -> servicio.consultarHistorialPorFechas("PAC-001", hoy, null)).getMessage()));
    }

    @Test
    @DisplayName("Un rango invertido (fin un día antes que el inicio) lanza excepción")
    void consultarHistorialPorFechasInvertidasLanzaExcepcion() {
        LocalDate hoy = LocalDate.of(2026, 11, 23);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarHistorialPorFechas("PAC-001", hoy, hoy.minusDays(1)));

        assertEquals("La fecha de inicio no puede ser posterior a la fecha de fin", e.getMessage());
    }

    // ---------------------------------------------------------------- consultarDetallePrueba

    @Test
    @DisplayName("Devuelve el detalle de una prueba propia del paciente y lo audita")
    void consultarDetallePruebaPropiaLaDevuelveYLaAudita() {
        HistorialPruebaDTO detalle = servicio.consultarDetallePrueba("REG-002", "PAC-001");

        assertAll(
                () -> assertEquals("TAC Abdominal", detalle.getTipoPrueba()),
                () -> assertEquals("MED-010", detalle.getMedicoId()),
                () -> assertTrue(detalle.isTieneEstudioImagen()),
                () -> assertEquals("CONSULTAR_DETALLE_HISTORIAL:REG-002", eventosAuditoria.get(0).getAccion()));
    }

    @Test
    @DisplayName("Deniega el acceso a una prueba de otro paciente y lo audita como denegado")
    void consultarDetallePruebaAjenaLanzaSecurityExceptionYAudita() {
        SecurityException e = assertThrows(SecurityException.class,
                () -> servicio.consultarDetallePrueba("REG-999", "PAC-001"));

        assertAll(
                () -> assertEquals("Acceso denegado: la prueba no pertenece al paciente", e.getMessage()),
                () -> assertEquals(1, eventosAuditoria.size()),
                () -> assertFalse(eventosAuditoria.get(0).isExito()),
                () -> assertEquals("ACCESO_DENEGADO_HISTORIAL:REG-999", eventosAuditoria.get(0).getAccion()));
    }

    @ParameterizedTest(name = "registro [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("El detalle con identificador de registro nulo, vacío o en blanco lanza excepción")
    void consultarDetallePruebaConRegistroInvalidoLanzaExcepcion(String registroId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarDetallePrueba(registroId, "PAC-001"));

        assertEquals("El identificador del registro es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("El detalle con paciente nulo, vacío o en blanco lanza excepción")
    void consultarDetallePruebaConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarDetallePrueba("REG-001", paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    @Test
    @DisplayName("Una prueba inexistente propaga IllegalStateException")
    void consultarDetallePruebaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarDetallePrueba("REG-404", "PAC-001"));

        assertEquals("No existe ningún registro de prueba con ID: REG-404", e.getMessage());
    }
}
