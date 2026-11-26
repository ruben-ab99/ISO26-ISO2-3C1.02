package es.uclm.esi.iso2.cliente.auditoria;

import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IGestorAuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditoriaAdminControladorTest {

    private List<AuditLogDTO> logsSimulados;
    private AuditoriaAdminControlador controlador;

    @BeforeEach
    void setUp() {
        logsSimulados = new ArrayList<>();
        IGestorAuditoriaService stubServicio = new IGestorAuditoriaService() {
            @Override
            public void registrarActividad(String usuario, String accion, boolean exito, String ip, String tokenSSO) {
                logsSimulados.add(new AuditLogDTO(usuario, accion, exito, ip));
            }

            @Override
            public List<AuditLogDTO> consultarHistorialCompleto(String tokenSSO) {
                return new ArrayList<>(logsSimulados);
            }

            @Override
            public List<AuditLogDTO> buscarTrazas(String usuario, String accionContiene, Boolean soloExitosos,
                                                  LocalDateTime desde, LocalDateTime hasta, String tokenSSO) {
                return logsSimulados.stream()
                        .filter(l -> usuario == null || l.getUsuario().equalsIgnoreCase(usuario))
                        .filter(l -> accionContiene == null || l.getAccion().contains(accionContiene))
                        .toList();
            }

            @Override
            public List<AuditLogDTO> consultarIntentosFallidos(String tokenSSO) {
                return logsSimulados.stream().filter(l -> !l.isExito()).toList();
            }
        };

        controlador = new AuditoriaAdminControlador(stubServicio, "token-sso-adm01-ADMINISTRADOR-0001");
    }

    @Test
    @DisplayName("Validación de parámetros del constructor")
    void validarConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new AuditoriaAdminControlador(null, "tok"));
        assertThrows(IllegalArgumentException.class, () -> new AuditoriaAdminControlador(
                new IGestorAuditoriaService() {
                    @Override public void registrarActividad(String u, String a, boolean e, String i, String t) {}
                    @Override public List<AuditLogDTO> consultarHistorialCompleto(String t) { return List.of(); }
                    @Override public List<AuditLogDTO> buscarTrazas(String u, String a, Boolean s, LocalDateTime d, LocalDateTime h, String t) { return List.of(); }
                    @Override public List<AuditLogDTO> consultarIntentosFallidos(String t) { return List.of(); }
                }, " "));
    }

    @Test
    @DisplayName("Operaciones de consulta, filtrado y generación de informe de auditoría")
    void operacionesControladorAuditoria() {
        assertEquals("No existen registros de auditoría en el sistema.", controlador.generarInformeAuditoriaTexto());

        controlador.notificarEventoSistema("adm01", "LOGIN_ADMIN", true, "10.0.0.5");
        controlador.notificarEventoSistema("intr01", "ACCESO_DENEGADO", false, "10.0.0.99");

        assertEquals(2, controlador.obtenerTodasLasTrazas().size());
        assertEquals(1, controlador.filtrarPorUsuario("adm01").size());
        assertEquals(1, controlador.filtrarPorAccionYRango("ACCESO", null, null).size());
        assertEquals(1, controlador.obtenerAlertasSeguridad().size());

        String informe = controlador.generarInformeAuditoriaTexto();
        assertTrue(informe.contains("LOGIN_ADMIN"));
        assertTrue(informe.contains("DENEGADO/ERROR"));
    }
}