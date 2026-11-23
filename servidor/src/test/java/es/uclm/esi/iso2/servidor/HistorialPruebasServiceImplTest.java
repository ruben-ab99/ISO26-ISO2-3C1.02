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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

    // ---------- Constructor ----------

    @Test
    @DisplayName("El constructor rechaza un servicio de registro nulo")
    void constructorConRegistroNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new HistorialPruebasServiceImpl(null, new StubInformesImagenes(), new StubAudit()));
    }

    @Test
    @DisplayName("El constructor rechaza un servicio de informes nulo")
    void constructorConInformesNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new HistorialPruebasServiceImpl(new StubRegistroPrueba(), null, new StubAudit()));
    }

    // ---------- consultarHistorial ----------

    @Test
    @DisplayName("Devuelve solo las pruebas realizadas del paciente, de la más reciente a la más antigua")
    void consultarHistorialDevuelveRealizadasOrdenadas() {
        List<HistorialPruebaDTO> historial = servicio.consultarHistorial("PAC-001");

        assertEquals(3, historial.size());
        assertEquals("REG-002", historial.get(0).getRegistroId());
        assertEquals("REG-003", historial.get(1).getRegistroId());
        assertEquals("REG-001", historial.get(2).getRegistroId());
        assertTrue(historial.stream().allMatch(p -> "PAC-001".equals(p.getPacienteId())));
    }

    @Test
    @DisplayName("Marca las pruebas que tienen estudio de imagen asociado")
    void consultarHistorialIndicaEstudioDeImagen() {
        List<HistorialPruebaDTO> historial = servicio.consultarHistorial("PAC-001");

        assertTrue(historial.get(0).isTieneEstudioImagen());
        assertFalse(historial.get(1).isTieneEstudioImagen());
        assertEquals("Paciente con claustrofobia", historial.get(0).getIncidencias());
    }

    @Test
    @DisplayName("Un paciente sin pruebas obtiene una lista vacía")
    void consultarHistorialSinPruebasDevuelveListaVacia() {
        assertTrue(servicio.consultarHistorial("PAC-NUEVO").isEmpty());
    }

    @Test
    @DisplayName("Un paciente nulo o vacío lanza IllegalArgumentException")
    void consultarHistorialConPacienteInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarHistorial(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarHistorial("  "));
    }

    @Test
    @DisplayName("Cada consulta del historial queda registrada en auditoría")
    void consultarHistorialRegistraAuditoria() {
        servicio.consultarHistorial(" PAC-001 ");

        assertEquals(1, eventosAuditoria.size());
        assertEquals("PAC-001", eventosAuditoria.get(0).getUsuario());
        assertEquals("CONSULTAR_HISTORIAL_PRUEBAS", eventosAuditoria.get(0).getAccion());
        assertTrue(eventosAuditoria.get(0).isExito());
    }

    @Test
    @DisplayName("Funciona sin servicio de auditoría (es opcional)")
    void consultarHistorialSinAuditoria() {
        HistorialPruebasServiceImpl sinAudit =
                new HistorialPruebasServiceImpl(new StubRegistroPrueba(), new StubInformesImagenes(), null);
        assertEquals(3, sinAudit.consultarHistorial("PAC-001").size());
    }

    // ---------- Filtros ----------

    @Test
    @DisplayName("Filtra el historial por tipo de prueba sin distinguir mayúsculas")
    void consultarHistorialPorTipoFiltra() {
        List<HistorialPruebaDTO> rx = servicio.consultarHistorialPorTipo("PAC-001", "rx tórax");

        assertEquals(2, rx.size());
        assertTrue(rx.stream().allMatch(p -> "RX Tórax".equals(p.getTipoPrueba())));
    }

    @Test
    @DisplayName("El filtro por tipo exige un tipo de prueba")
    void consultarHistorialPorTipoSinTipoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarHistorialPorTipo("PAC-001", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarHistorialPorTipo("PAC-001", " "));
    }

    @Test
    @DisplayName("Filtra el historial por rango de fechas (ambos extremos incluidos)")
    void consultarHistorialPorFechasFiltra() {
        List<HistorialPruebaDTO> octubreNoviembre = servicio.consultarHistorialPorFechas(
                "PAC-001", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 5));

        assertEquals(2, octubreNoviembre.size());
        assertEquals("REG-002", octubreNoviembre.get(0).getRegistroId());
        assertEquals("REG-003", octubreNoviembre.get(1).getRegistroId());
    }

    @Test
    @DisplayName("El filtro por fechas rechaza fechas nulas o un rango invertido")
    void consultarHistorialPorFechasInvalidasLanzaExcepcion() {
        LocalDate hoy = LocalDate.of(2026, 11, 23);
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarHistorialPorFechas("PAC-001", null, hoy));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarHistorialPorFechas("PAC-001", hoy, null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarHistorialPorFechas("PAC-001", hoy, hoy.minusDays(1)));
    }

    // ---------- Detalle ----------

    @Test
    @DisplayName("Devuelve el detalle de una prueba propia del paciente")
    void consultarDetallePruebaPropia() {
        HistorialPruebaDTO detalle = servicio.consultarDetallePrueba("REG-002", "PAC-001");

        assertEquals("TAC Abdominal", detalle.getTipoPrueba());
        assertEquals("MED-010", detalle.getMedicoId());
        assertTrue(detalle.isTieneEstudioImagen());
        assertEquals("CONSULTAR_DETALLE_HISTORIAL:REG-002", eventosAuditoria.get(0).getAccion());
    }

    @Test
    @DisplayName("Deniega el acceso a una prueba de otro paciente y lo audita")
    void consultarDetallePruebaAjenaLanzaSecurityException() {
        assertThrows(SecurityException.class, () -> servicio.consultarDetallePrueba("REG-999", "PAC-001"));

        assertEquals(1, eventosAuditoria.size());
        assertFalse(eventosAuditoria.get(0).isExito());
        assertEquals("ACCESO_DENEGADO_HISTORIAL:REG-999", eventosAuditoria.get(0).getAccion());
    }

    @Test
    @DisplayName("El detalle exige identificador de registro y de paciente")
    void consultarDetallePruebaConParametrosInvalidosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarDetallePrueba(null, "PAC-001"));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarDetallePrueba("REG-001", " "));
    }

    @Test
    @DisplayName("Una prueba inexistente propaga IllegalStateException")
    void consultarDetallePruebaInexistente() {
        assertThrows(IllegalStateException.class, () -> servicio.consultarDetallePrueba("REG-404", "PAC-001"));
    }
}