package cr.una.consultores.service;

import cr.una.consultores.dto.SaludOracleDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda la ultima medicion que reporto cada agente.
 *
 * POR QUE EN MEMORIA Y NO EN LA BASE
 * Para no tocar el esquema de Neon, que esta en ddl-auto=validate: agregar
 * una entidad sin crear antes su tabla impide que la aplicacion arranque.
 * Cuando esto ya funcione se cambia por una tabla y se gana el historico.
 *
 * Consecuencia a tener presente: si el servicio de Render se reinicia o se
 * duerme, este mapa queda vacio hasta que el agente vuelva a reportar. Con
 * un intervalo de un minuto, el hueco es de un minuto.
 *
 * LATIDO
 * Si un agente deja de reportar, su instancia no desaparece: se conserva la
 * ultima lectura y se marca como sin contacto. Un monitor que oculta lo que
 * no responde no sirve, porque el silencio es justamente la senal mas
 * importante.
 */
@Service
public class AgenteMetricasService {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final Map<String, SaludOracleDTO> ultimas = new ConcurrentHashMap<>();
    private final Map<String, Instant> recibido = new ConcurrentHashMap<>();

    /** Segundos sin reportar tras los cuales se considera perdido el contacto. */
    @Value("${agente.timeout-segundos:180}")
    private int timeoutSegundos;

    public void registrar(SaludOracleDTO dto) {
        if (dto == null || dto.instanciaId == null || dto.instanciaId.isBlank()) {
            throw new IllegalArgumentException("El reporte debe traer instanciaId");
        }
        Instant ahora = Instant.now();
        dto.origen = "agente";
        dto.conectado = true;
        dto.segundosSinContacto = 0;
        if (dto.ultimaLectura == null || dto.ultimaLectura.isBlank()) {
            dto.ultimaLectura = ISO.format(ahora);
        }
        if (dto.tipo == null || dto.tipo.isBlank()) {
            dto.tipo = "tradicional";
        }
        ultimas.put(dto.instanciaId, dto);
        recibido.put(dto.instanciaId, ahora);
    }

    /** Todas las instancias reportadas por agentes, con su latido evaluado. */
    public List<SaludOracleDTO> listar() {
        Instant ahora = Instant.now();
        List<SaludOracleDTO> salida = new ArrayList<>();

        for (Map.Entry<String, SaludOracleDTO> e : ultimas.entrySet()) {
            SaludOracleDTO dto = e.getValue();
            Instant visto = recibido.get(e.getKey());
            long seg = visto == null ? Long.MAX_VALUE
                    : Duration.between(visto, ahora).getSeconds();

            dto.segundosSinContacto = (int) Math.min(seg, Integer.MAX_VALUE);
            if (seg > timeoutSegundos) {
                dto.conectado = false;
                dto.estado = "unknown";   // se conservan los valores, no el veredicto
            } else {
                dto.conectado = true;
            }
            salida.add(dto);
        }
        salida.sort(Comparator.comparing(d -> d.nombre == null ? "" : d.nombre));
        return salida;
    }

    public int cantidad() {
        return ultimas.size();
    }

    public void olvidar(String instanciaId) {
        ultimas.remove(instanciaId);
        recibido.remove(instanciaId);
    }
}