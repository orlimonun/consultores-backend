package cr.una.consultores.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "organizacion")
public class Organizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "area_evaluada", nullable = false, length = 150)
    private String areaEvaluada;

    @Column(name = "dba_nombre", length = 120)
    private String dbaNombre;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getAreaEvaluada() { return areaEvaluada; }
    public void setAreaEvaluada(String areaEvaluada) { this.areaEvaluada = areaEvaluada; }
    public String getDbaNombre() { return dbaNombre; }
    public void setDbaNombre(String dbaNombre) { this.dbaNombre = dbaNombre; }
}
