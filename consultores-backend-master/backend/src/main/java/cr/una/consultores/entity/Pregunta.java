package cr.una.consultores.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pregunta")
public class Pregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @Column(nullable = false, length = 300)
    private String texto;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Control getControl() { return control; }
    public void setControl(Control control) { this.control = control; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
