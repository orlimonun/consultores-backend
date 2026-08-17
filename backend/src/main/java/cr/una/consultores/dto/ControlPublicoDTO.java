package cr.una.consultores.dto;

import java.util.List;

// DTO de solo lectura para exponer el catalogo de controles/preguntas
// en la demo publica, sin exponer entidades ni datos de auditorias reales.
public class ControlPublicoDTO {
    public Integer id;
    public String codigo;
    public String nombre;
    public String dominio;
    public String objetivo;
    public Short peso;
    public Boolean afectaC;
    public Boolean afectaI;
    public Boolean afectaD;
    public List<PreguntaDTO> preguntas;

    public ControlPublicoDTO() {}

    public ControlPublicoDTO(Integer id, String codigo, String nombre, String dominio, String objetivo,
                             Short peso, Boolean afectaC, Boolean afectaI, Boolean afectaD,
                             List<PreguntaDTO> preguntas) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.dominio = dominio;
        this.objetivo = objetivo;
        this.peso = peso;
        this.afectaC = afectaC;
        this.afectaI = afectaI;
        this.afectaD = afectaD;
        this.preguntas = preguntas;
    }
}