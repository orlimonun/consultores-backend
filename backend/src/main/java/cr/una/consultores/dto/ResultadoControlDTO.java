package cr.una.consultores.dto;

public class ResultadoControlDTO {
    public Integer controlId;
    public String codigo;
    public String nombre;
    public String dominio;
    public double nivelMadurez;   // ahora es promedio (puede ser decimal, ej. 3.5)
    public double cumplimiento;
    public double riesgoControl;
    public boolean afectaC;
    public boolean afectaI;
    public boolean afectaD;
}
