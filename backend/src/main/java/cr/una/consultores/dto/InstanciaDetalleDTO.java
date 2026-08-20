package cr.una.consultores.dto;

import java.util.List;

public class InstanciaDetalleDTO {
    public Integer id;
    public String nombre;
    public String instancia;
    public String ubicacion;
    public double isbd;
    public String estado;
    public List<IndicadorDTO> indicadores;   // IP, IM, IA con sus metricas
    public List<AlertaDTO> alertas;
}
