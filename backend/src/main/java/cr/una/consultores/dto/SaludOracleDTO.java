package cr.una.consultores.dto;

import java.util.List;

/**
 * Salud de UNA instancia Oracle.
 *
 * Los campos ip/im/ia/isbd/estado/metricas son los originales y no cambian,
 * asi que el endpoint /salud sigue funcionando igual para el frontend actual.
 *
 * Los campos nuevos identifican de que instancia se trata y de donde vinieron
 * los datos, porque ahora puede haber mas de una:
 *   - la Autonomous de Oracle Cloud, leida en vivo por el backend
 *   - una instancia tradicional en red privada, que reporta por agente
 */
public class SaludOracleDTO {

    // ---- Indicadores (los de siempre) ----
    public double ip;          // Procesos
    public double im;          // Memoria
    public double ia;          // Archivos
    public Double ir;          // Recuperacion: null si la instancia no lo reporta
    public double isbd;        // Indice global
    public String estado;      // optimal|healthy|warning|degraded|critical|unknown
    public List<Metrica> metricas;

    // ---- Identidad de la instancia ----
    public String instanciaId;      // clave estable, ej: "autonomous-monitordb"
    public String nombre;           // lo que se muestra, ej: "monitordb"
    public String empresa;          // el cliente al que pertenece
    public String tipo;             // "autonomous" | "tradicional"
    public String origen;           // "directo" | "agente"
    public boolean conectado;       // hay contacto con la instancia
    public String ultimaLectura;    // ISO-8601, cuando se tomo la medicion
    public Integer segundosSinContacto;  // solo para agentes; null si es directo

    public static class Metrica {
        public String componente;  // Procesos|Memoria|Archivos|Recuperacion
        public String label;
        public int valor;
        public String estado;      // normal|warning|degraded|critical
    }
}