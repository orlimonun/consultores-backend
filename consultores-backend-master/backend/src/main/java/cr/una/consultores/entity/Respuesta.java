package cr.una.consultores.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "respuesta",
        uniqueConstraints = @UniqueConstraint(columnNames = {"auditoria_id", "pregunta_id"}))
public class Respuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private Auditoria auditoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @Column(nullable = false, length = 10)
    private String respuesta;

    // Nivel de madurez (0-5) que el auditor elige para esta pregunta,
    // segun la escala de la norma. Reemplaza el calculo automatico.
    @Column(name = "nivel_madurez")
    private Short nivelMadurez;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Auditoria getAuditoria() { return auditoria; }
    public void setAuditoria(Auditoria auditoria) { this.auditoria = auditoria; }
    public Pregunta getPregunta() { return pregunta; }
    public void setPregunta(Pregunta pregunta) { this.pregunta = pregunta; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public Short getNivelMadurez() { return nivelMadurez; }
    public void setNivelMadurez(Short nivelMadurez) { this.nivelMadurez = nivelMadurez; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
