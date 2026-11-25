package es.uclm.sescam.contratos.analitica;

import java.time.LocalDate;

/**
 * CU-16 · Generar informes operativos (Personal Administrativo / Admisión): ocupación de agendas,
 * tiempos de espera y cancelaciones de un periodo.
 */
public interface IInformesOperativosService {

    InformeOperativoDTO generarInformeOperativo(LocalDate desde, LocalDate hasta,
                                                String usuarioAdmisionId, String tokenSSO);
}
