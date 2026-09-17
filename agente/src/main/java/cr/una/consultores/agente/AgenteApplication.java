package cr.una.consultores.agente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Agente de monitoreo para una instancia Oracle que no es alcanzable desde
 * internet.
 *
 * POR QUE EXISTE
 * Una base local vive detras de un router con NAT: el backend desplegado en la
 * nube no puede iniciar una conexion hacia ella. El agente si puede salir hacia
 * internet, porque esa direccion todos los firewalls la permiten. En vez de que
 * el monitor vaya a buscar los datos, el agente se los lleva.
 *
 * Es el mismo modelo de Datadog, Zabbix, Prometheus y New Relic.
 *
 * No abre ningun puerto ni expone ningun servicio: solo lee la base local y
 * hace peticiones salientes.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
public class AgenteApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgenteApplication.class, args);
    }
}
