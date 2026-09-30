package cr.una.consultores.agente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Agente de monitoreo + ejecucion RMAN para una instancia Oracle local.
 *
 * POR QUE EXISTE
 * Una base local vive detras de un router con NAT: el backend desplegado en la
 * nube no puede iniciar una conexion hacia ella. El agente si puede salir hacia
 * internet. Ademas, RMAN debe ejecutarse en el mismo servidor que Oracle, por
 * lo que el agente recibe el script del backend y lo ejecuta localmente.
 *
 * MODO DUAL
 *   - Monitoreo: lee la base y envia metricas al backend cada minuto.
 *   - Ejecucion RMAN: expone /api/rman/ejecutar en 127.0.0.1 para recibir
 *     scripts del backend y reportar el resultado via webhook.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
public class AgenteApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgenteApplication.class, args);
    }
}
