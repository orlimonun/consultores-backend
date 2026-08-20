package cr.una.consultores.entity;

import jakarta.persistence.*;

/**
 * Una instancia Oracle monitoreada (un "cliente" en el tablero del monitor).
 */
@Entity
@Table(name = "instancia_monitoreada")
public class InstanciaMonitoreada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String instancia;   // ej. ORCL-PROD-01

    @Column(length = 120)
    private String ubicacion;

    @Column(nullable = false)
    private boolean activo = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getInstancia() { return instancia; }
    public void setInstancia(String instancia) { this.instancia = instancia; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
