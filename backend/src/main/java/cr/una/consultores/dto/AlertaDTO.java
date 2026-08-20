package cr.una.consultores.dto;

public class AlertaDTO {
    public String componente;   // Procesos | Memoria | Archivos
    public String variable;     // nombre de la metrica
    public String valor;        // valor + unidad
    public String nivel;        // warning | degraded | critical
    public String descripcion;
}
