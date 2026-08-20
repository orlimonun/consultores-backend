package cr.una.consultores.dto;

public class InstanciaSaludDTO {
    public Integer id;
    public String nombre;
    public String instancia;
    public String ubicacion;
    public String ultimaActualizacion;   // texto tipo "Hace 12 s" o fecha
    public double ip;      // indicador de procesos
    public double im;      // indicador de memoria
    public double ia;      // indicador de archivos
    public double isbd;    // indice de salud global
    public String estado;  // optimal | healthy | warning | degraded | critical
    public int totalAlertas;
}
