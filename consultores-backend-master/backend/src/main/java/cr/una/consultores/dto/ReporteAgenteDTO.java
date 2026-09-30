package cr.una.consultores.dto;

import java.util.ArrayList;
import java.util.List;

/** El cuerpo completo que un agente envia en cada ciclo. */
public class ReporteAgenteDTO {
    public String instanciaId;
    public String nombre;
    public String empresa;
    public String tipo;            // "tradicional" | "autonomous"
    public String ultimaLectura;   // ISO-8601
    public boolean baseAccesible = true;
    public String errorLectura;    // si no se pudo leer la base
    public List<LecturaCrudaDTO> lecturas = new ArrayList<>();
}