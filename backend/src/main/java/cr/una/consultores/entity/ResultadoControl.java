package cr.una.consultores.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "resultado_control",
       uniqueConstraints = @UniqueConstraint(columnNames = {"auditoria_id", "control_id"}))
public class ResultadoControl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private Auditoria auditoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @Column(name = "nivel_madurez", nullable = false)
    private Short nivelMadurez;

    private BigDecimal cumplimiento;

    @Column(name = "riesgo_c")
    private BigDecimal riesgoC;

    @Column(name = "riesgo_i")
    private BigDecimal riesgoI;

    @Column(name = "riesgo_d")
    private BigDecimal riesgoD;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Auditoria getAuditoria() { return auditoria; }
    public void setAuditoria(Auditoria auditoria) { this.auditoria = auditoria; }
    public Control getControl() { return control; }
    public void setControl(Control control) { this.control = control; }
    public Short getNivelMadurez() { return nivelMadurez; }
    public void setNivelMadurez(Short nivelMadurez) { this.nivelMadurez = nivelMadurez; }
    public BigDecimal getCumplimiento() { return cumplimiento; }
    public void setCumplimiento(BigDecimal cumplimiento) { this.cumplimiento = cumplimiento; }
    public BigDecimal getRiesgoC() { return riesgoC; }
    public void setRiesgoC(BigDecimal riesgoC) { this.riesgoC = riesgoC; }
    public BigDecimal getRiesgoI() { return riesgoI; }
    public void setRiesgoI(BigDecimal riesgoI) { this.riesgoI = riesgoI; }
    public BigDecimal getRiesgoD() { return riesgoD; }
    public void setRiesgoD(BigDecimal riesgoD) { this.riesgoD = riesgoD; }
}
