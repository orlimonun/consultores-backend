package cr.una.consultores.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "control")
public class Control {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dominio_id", nullable = false)
    private Dominio dominio;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String objetivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Short peso = 5;

    @Column(name = "afecta_c", nullable = false)
    private Boolean afectaC = false;

    @Column(name = "afecta_i", nullable = false)
    private Boolean afectaI = false;

    @Column(name = "afecta_d", nullable = false)
    private Boolean afectaD = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Dominio getDominio() { return dominio; }
    public void setDominio(Dominio dominio) { this.dominio = dominio; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Short getPeso() { return peso; }
    public void setPeso(Short peso) { this.peso = peso; }
    public Boolean getAfectaC() { return afectaC; }
    public void setAfectaC(Boolean afectaC) { this.afectaC = afectaC; }
    public Boolean getAfectaI() { return afectaI; }
    public void setAfectaI(Boolean afectaI) { this.afectaI = afectaI; }
    public Boolean getAfectaD() { return afectaD; }
    public void setAfectaD(Boolean afectaD) { this.afectaD = afectaD; }
}
