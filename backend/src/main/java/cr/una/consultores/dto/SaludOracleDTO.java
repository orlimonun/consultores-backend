package cr.una.consultores.dto;

import java.util.List;

public class SaludOracleDTO {
    public double ip;      // indicador procesos
    public double im;      // indicador memoria
    public double ia;      // indicador archivos
    public double isbd;    // indice de salud global
    public String estado;  // optimal|healthy|warning|degraded|critical
    public List<Metrica> metricas;

    public static class Metrica {
        public String componente;  // Procesos|Memoria|Archivos
        public String label;
        public int valor;
        public String estado;      // normal|warning|critical
    }
}
