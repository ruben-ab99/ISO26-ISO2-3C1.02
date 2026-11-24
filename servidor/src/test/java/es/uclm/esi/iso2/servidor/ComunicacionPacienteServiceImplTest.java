package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.portal.EncuestaSatisfaccionDTO;
import es.uclm.sescam.contratos.portal.IncidenciaPacienteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComunicacionPacienteServiceImplTest {

    private AuditServiceImpl auditService;
    private ComunicacionPacienteServiceImpl servicio;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl();
        servicio = new ComunicacionPacienteServiceImpl(auditService);
    }

    @Test
    @DisplayName("Registrar incidencia válida crea el registro en estado ABIERTA y audita el evento")
    void registrarIncidenciaValida() {
        // Preparar
        String paciente = "PAC-001";

        // Ejecutar
        IncidenciaPacienteDTO incConRef = servicio.registrarIncidencia(
                paciente, "CIT-101", "cita", "Retraso de 40 minutos en la sala de espera"
        );
        IncidenciaPacienteDTO incSinRef = servicio.registrarIncidencia(
                paciente, null, "ATENCION", "Duda sobre preparación previa"
        );

        // Comprobar
        assertNotNull(incConRef);
        assertTrue(incConRef.getIncidenciaId().startsWith("INC-"));
        assertEquals("CITA", incConRef.getCategoria());
        assertEquals("CIT-101", incConRef.getReferenciaCitaOPruebaId());
        assertEquals("ABIERTA", incConRef.getEstado());
        assertNotNull(incConRef.getFechaRegistro());

        assertEquals("", incSinRef.getReferenciaCitaOPruebaId());
        assertEquals(2, auditService.obtenerTrazas().size());
    }

    @Test
    @DisplayName("Registrar incidencia con parámetros inválidos o categoría no permitida lanza excepción")
    void registrarIncidenciaInvalidaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarIncidencia(null, "CIT-1", "CITA", "Desc"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarIncidencia("  ", "CIT-1", "CITA", "Desc"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarIncidencia("PAC-1", "CIT-1", null, "Desc"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarIncidencia("PAC-1", "CIT-1", " ", "Desc"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarIncidencia("PAC-1", "CIT-1", "INVENTADA", "Desc"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarIncidencia("PAC-1", "CIT-1", "PRUEBA", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarIncidencia("PAC-1", "CIT-1", "OTRA", "   "));
    }

    @Test
    @DisplayName("Listar mis incidencias filtra exclusivamente las del paciente solicitado")
    void listarMisIncidenciasFiltraPorPaciente() {
        // Preparar
        servicio.registrarIncidencia("PAC-01", "CIT-1", "CITA", "Problema con horario");
        servicio.registrarIncidencia("PAC-01", "REG-2", "PRUEBA", "Molestia leve tras contraste");
        servicio.registrarIncidencia("PAC-02", "CIT-9", "OTRA", "Consulta general");

        // Ejecutar
        List<IncidenciaPacienteDTO> listaPac1 = servicio.listarMisIncidencias("PAC-01");
        List<IncidenciaPacienteDTO> listaVacia = servicio.listarMisIncidencias("PAC-99");

        // Comprobar
        assertEquals(2, listaPac1.size());
        assertTrue(listaVacia.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> servicio.listarMisIncidencias(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarMisIncidencias(" "));
    }

    @Test
    @DisplayName("Responder encuesta válida admite valores límite 1..5 y bloquea duplicados por cita")
    void responderEncuestaValidaYDuplicada() {
        // Preparar & Ejecutar
        EncuestaSatisfaccionDTO encMin = servicio.responderEncuesta(
                "PAC-01", "CIT-200", 1, 5, "La atención médica fue excelente pese a la espera"
        );
        EncuestaSatisfaccionDTO encMax = servicio.responderEncuesta(
                "PAC-01", "CIT-201", 5, 1, null
        );

        // Comprobar
        assertNotNull(encMin);
        assertTrue(encMin.getEncuestaId().startsWith("ENC-"));
        assertEquals(1, encMin.getPuntuacionGlobal());
        assertEquals(5, encMin.getPuntuacionAtencion());
        assertNotNull(encMin.getFechaRespuesta());
        assertEquals("", encMax.getComentarios());

        assertThrows(IllegalStateException.class, () ->
                servicio.responderEncuesta("PAC-01", "cit-200", 4, 4, "Intento duplicado"));
    }

    @Test
    @DisplayName("Responder encuesta con puntuaciones fuera de rango (0 o 6) o datos vacíos lanza excepción")
    void responderEncuestaInvalidaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.responderEncuesta(null, "CIT-1", 4, 4, "Ok"));
        assertThrows(IllegalArgumentException.class, () -> servicio.responderEncuesta(" ", "CIT-1", 4, 4, "Ok"));
        assertThrows(IllegalArgumentException.class, () -> servicio.responderEncuesta("PAC-1", null, 4, 4, "Ok"));
        assertThrows(IllegalArgumentException.class, () -> servicio.responderEncuesta("PAC-1", "  ", 4, 4, "Ok"));
        assertThrows(IllegalArgumentException.class, () -> servicio.responderEncuesta("PAC-1", "CIT-1", 0, 4, "Ok"));
        assertThrows(IllegalArgumentException.class, () -> servicio.responderEncuesta("PAC-1", "CIT-1", 6, 4, "Ok"));
        assertThrows(IllegalArgumentException.class, () -> servicio.responderEncuesta("PAC-1", "CIT-1", 4, 0, "Ok"));
        assertThrows(IllegalArgumentException.class, () -> servicio.responderEncuesta("PAC-1", "CIT-1", 4, 6, "Ok"));
    }

    @Test
    @DisplayName("Listar mis encuestas devuelve solo las del paciente y funciona sin auditoría")
    void listarMisEncuestasYConstructorSinAuditoria() {
        // Preparar
        ComunicacionPacienteServiceImpl sinAudit = new ComunicacionPacienteServiceImpl(null);
        sinAudit.responderEncuesta("PAC-10", "CIT-10", 5, 5, "Todo perfecto");
        sinAudit.responderEncuesta("PAC-20", "CIT-20", 3, 4, "Correcto");

        // Ejecutar
        List<EncuestaSatisfaccionDTO> encuestasPac10 = sinAudit.listarMisEncuestas("PAC-10");

        // Comprobar
        assertEquals(1, encuestasPac10.size());
        assertEquals("CIT-10", encuestasPac10.get(0).getCitaId());
        assertTrue(sinAudit.listarMisEncuestas("PAC-SIN-ENCUESTAS").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> sinAudit.listarMisEncuestas(" "));
    }
}