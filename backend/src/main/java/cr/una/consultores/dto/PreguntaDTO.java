package cr.una.consultores.dto;

public class PreguntaDTO {
    public Integer id;
    public String texto;

    public PreguntaDTO() {}
    public PreguntaDTO(Integer id, String texto) {
        this.id = id;
        this.texto = texto;
    }
}