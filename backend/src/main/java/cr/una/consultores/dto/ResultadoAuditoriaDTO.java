package cr.una.consultores.dto;

import java.util.List;

public class ResultadoAuditoriaDTO {
    public Integer auditoriaId;
    public double madurezPromedioGeneral;
    // Se elimino indiceGeneralRiesgo: el riesgo se reporta por eje del
    // CID (C, I, D) por separado, sin promediarlos en un unico numero.
    public double riesgoC;
    public double riesgoI;
    public double riesgoD;
    public List<ResultadoControlDTO> controles;
    public List<ResultadoDominioDTO> dominios;
    public List<ResultadoControlDTO> menorMadurez;
    public List<ResultadoControlDTO> mayorRiesgo;
}
