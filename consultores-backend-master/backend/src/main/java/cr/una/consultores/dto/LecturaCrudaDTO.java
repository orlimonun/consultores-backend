package cr.una.consultores.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que el agente envia: numeros crudos, sin veredicto.
 *
 * El agente NO evalua. Manda el valor y el backend decide si esta dentro
 * de rango, porque los umbrales viven en el catalogo del servidor. Asi un
 * cambio de umbral no obliga a redesplegar el agente en la maquina del
 * cliente, y las dos instancias (la autonoma y la tradicional) se evaluan
 * con el mismo codigo, que es lo que hace comparables sus indices.
 */
public class LecturaCrudaDTO {

    /** Codigo de la variable en el catalogo: p1, m4, a6... */
    public String codigo;

    /** Valor numerico. Para variables booleanas, 1 o 0. */
    public Double valor;

    /** Cuando el valor no es numerico (una ruta, un modo). Opcional. */
    public String valorTexto;

    /** Nivel 3: el desglose fisico de esta variable. Opcional. */
    public List<Detalle> detalle = new ArrayList<>();

    public LecturaCrudaDTO() {}

    public LecturaCrudaDTO(String codigo, Double valor) {
        this.codigo = codigo; this.valor = valor;
    }

    /**
     * Una fila del tercer nivel: el tablespace concreto, el datafile con
     * su ruta en disco, el grupo de bitacora. Es lo que se pidio en la
     * revision al preguntar cual es el origen fisico de cada valor.
     */
    public static class Detalle {
        public String nombre;    // TABLESPACE_1, Grupo 3, ...
        public String origen;    // C:\app\...\st101.dbf
        public Double valor;
        public String valorTexto;
        public String nota;

        public Detalle() {}
        public Detalle(String nombre, String origen, Double valor) {
            this.nombre = nombre; this.origen = origen; this.valor = valor;
        }
    }
}