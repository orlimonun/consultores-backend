package cr.una.consultores.dto;

import java.util.List;

public class IndicadorDTO {
    public String codigo;   // IP | IM | IA
    public String label;    // Procesos | Memoria | Archivos
    public double valor;    // 0-100
    public String estado;
    public List<MetricaDTO> metricas;

    public static class MetricaDTO {
        public String key;
        public String label;
        public Integer valor;
        public String estado;   // normal | warning | degraded | critical
    }
}
