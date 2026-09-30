package cr.una.consultores.agente;

import java.util.ArrayList;
import java.util.List;

/**
 * El JSON que viaja al backend. Debe coincidir campo por campo con
 * SaludOracleDTO del lado del servidor: es el contrato entre los dos.
 */
public class ReporteSalud {

    public double ip;
    public double im;
    public double ia;
    public Double ir;          // Recuperacion: solo lo reportan las tradicionales
    public double isbd;
    public String estado;
    public List<Metrica> metricas = new ArrayList<>();

    public String instanciaId;
    public String nombre;
    public String empresa;
    public String tipo = "tradicional";
    public String ultimaLectura;

    public static class Metrica {
        public String componente;
        public String label;
        public int valor;
        public String estado;

        public Metrica(String componente, String label, int valor, String estado) {
            this.componente = componente;
            this.label = label;
            this.valor = valor;
            this.estado = estado == null ? "normal" : estado;
        }
    }

    public void add(String componente, String label, int valor, String estado) {
        metricas.add(new Metrica(componente, label, valor, estado));
    }
}
