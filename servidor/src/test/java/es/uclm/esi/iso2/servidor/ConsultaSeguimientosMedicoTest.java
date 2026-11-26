package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.ISolicitudPruebaService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaSeguimientosMedicoTest {

    private PrescripcionSeguimientoServiceImpl seguimientoService;
    private SolicitudPruebaServiceImpl solicitudService;

    @BeforeEach
    void setUp() {
        CatalogoPruebasServiceImpl catalogo = new CatalogoPruebasServiceImpl();
        GestionCitasServiceImpl citas = new GestionCitasServiceImpl();
        AuditServiceImpl audit = new AuditServiceImpl();

        seguimientoService = new PrescripcionSeguimientoServiceImpl(catalogo, citas, audit);
        solicitudService = new SolicitudPruebaServiceImpl(catalogo);
    }

    @Test
    @DisplayName("listarSeguimientosMedico filtra exclusivamente las prescripciones del médico indicado")
    void listarSeguimientosMedicoFiltraPorMedico() {
        // Preparar
        LocalDate inicio = LocalDate.now().plusDays(10);
        seguimientoService.prescribirSeguimiento("PAC-01", "MED-01", "RX-TORAX", 30, 2, inicio, "Control 1");
        seguimientoService.prescribirSeguimiento("PAC-02", "MED-01", "ECO-ABDO", 60, 1, inicio.plusDays(1), "Control 2");
        seguimientoService.prescribirSeguimiento("PAC-03", "MED-02", "TAC-ABDO", 90, 1, inicio.plusDays(2), "Otro médico");

        // Ejecutar
        List<PrescripcionSeguimientoDTO> deMed01 = seguimientoService.listarSeguimientosMedico("  MED-01  ");
        List<PrescripcionSeguimientoDTO> deMedInexistente = seguimientoService.listarSeguimientosMedico("MED-99");

        // Comprobar
        assertEquals(2, deMed01.size());
        assertTrue(deMed01.stream().allMatch(s -> "MED-01".equals(s.getMedicoId())));
        assertTrue(deMedInexistente.isEmpty());
    }

    @Test
    @DisplayName("listarSeguimientosMedico rechaza identificador de médico nulo o en blanco")
    void listarSeguimientosMedicoConParametroInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> seguimientoService.listarSeguimientosMedico(null));
        assertThrows(IllegalArgumentException.class, () -> seguimientoService.listarSeguimientosMedico("   "));
    }

    @Test
    @DisplayName("listarSolicitudesMedico filtra las solicitudes del médico indicado y valida nulos")
    void listarSolicitudesMedicoFiltraYValida() {
        // Preparar
        solicitudService.crearSolicitudPrueba("PAC-01", "MED-01", "RX-TORAX", 2, "Sospecha neumonía");
        solicitudService.crearSolicitudPrueba("PAC-02", "MED-02", "RM-CRANEO", 1, "Cefalea aguda");

        // Ejecutar
        List<SolicitudPruebaDTO> deMed01 = solicitudService.listarSolicitudesMedico("MED-01");

        // Comprobar
        assertEquals(1, deMed01.size());
        assertEquals("MED-01", deMed01.get(0).getMedicoId());
        assertThrows(IllegalArgumentException.class, () -> solicitudService.listarSolicitudesMedico(null));
        assertThrows(IllegalArgumentException.class, () -> solicitudService.listarSolicitudesMedico(" "));
    }

    @Test
    @DisplayName("Métodos default en contratos devuelven lista vacía por retrocompatibilidad (ADR-017)")
    void metodosDefaultEnContratosDevuelvenListaVacia() {
        IPrescripcionSeguimientoService stubSeguimiento = new IPrescripcionSeguimientoService() {
            @Override
            public PrescripcionSeguimientoDTO prescribirSeguimiento(String p, String m, String c, int per, int rev, LocalDate f, String ind) { return null; }

            @Override
            public PrescripcionSeguimientoDTO consultarSeguimiento(String id) { return null; }

            @Override
            public List<PrescripcionSeguimientoDTO> listarSeguimientosPaciente(String p) { return List.of(); }

            @Override
            public PrescripcionSeguimientoDTO cancelarSeguimiento(String id, String m) { return null; }
        };

        ISolicitudPruebaService stubSolicitud = new ISolicitudPruebaService() {
            @Override
            public SolicitudPruebaDTO crearSolicitudPrueba(String p, String m, String c, int pr, String j) { return null; }

            @Override
            public SolicitudPruebaDTO consultarSolicitud(String id) { return null; }

            @Override
            public List<SolicitudPruebaDTO> listarSolicitudesPaciente(String p) { return List.of(); }

            @Override
            public SolicitudPruebaDTO cancelarSolicitud(String id, String m) { return null; }
        };

        assertTrue(stubSeguimiento.listarSeguimientosMedico("MED-01").isEmpty());
        assertTrue(stubSolicitud.listarSolicitudesMedico("MED-01").isEmpty());
    }
}