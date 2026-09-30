package cr.una.consultores.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que el frontend recibe: el arbol completo de tres niveles ya
 * evaluado.
 *
 *   Indicador (IP/IM/IA)  ->  Grupo  ->  Variable  ->  Detalle fisico
 */
public class SaludInstanciaDTO {

    // ---- Identidad ----
    public String instanciaId;
    public String nombre;
    public String empresa;
    public String tipo;              // autonomous | tradicional
    public String origen;            // directo | agente
    public boolean conectado;
    public String ultimaLectura;
    public Integer segundosSinContacto;

    // ---- Resultado global ----
    public double isbd;
    /** El promedio ponderado sin topar. Cuando hay un componente en falla,
     *  isbd baja a la banda critica y este campo conserva el calculo
     *  original, para que la interfaz pueda mostrar los dos y explicar por
     *  que difieren en vez de ocultar uno de ellos. */
    public double isbdPonderado;
    public String estado;            // optimal|healthy|warning|degraded|critical|unknown
    public String formula;

    /** Por la regla de prevalencia: si algun componente esta critico, se
     *  nombran aqui las causas. Un indice alto no puede ocultarlas. */
    public List<String> causasCriticas = new ArrayList<>();

    public List<IndicadorDTO> indicadores = new ArrayList<>();

    // =================== Nivel 1 ===================
    public static class IndicadorDTO {
        public String codigo;        // IP | IM | IA
        public String nombre;
        public int peso;             // 30 | 35 | 35
        public double puntaje;
        public String estado;
        /** "ponderado" o "peor_caso". Archivos usa peor_caso a proposito. */
        public String agregacion;
        public String notaAgregacion;
        public List<GrupoDTO> grupos = new ArrayList<>();
    }

    // =================== Nivel 2 ===================
    public static class GrupoDTO {
        public String codigo;        // PMON | SGA | TABLESPACES ...
        public String nombre;
        public int peso;             // suma de los pesos de sus variables
        public double puntaje;
        public String estado;
        public List<VariableDTO> variables = new ArrayList<>();
    }

    // =================== Nivel 3 ===================
    public static class VariableDTO {
        public String codigo;        // p1, m4, a6
        public String nombre;
        public Double valor;
        public String valorTexto;
        public String unidad;
        /** La consulta exacta que produce el valor. Se muestra en la
         *  interfaz: la revision pidio poder demostrar de donde sale cada
         *  numero, no solo el numero. */
        public String consulta;
        public Double limiteInferior;
        public Double limiteSuperior;
        public String rangoNormal;   // texto listo para la tabla
        public String direccion;
        public String fuenteUmbral;  // DOCUMENTO|REVISION|MEDICION|CONVENCION|REFERENCIA
        public int peso;
        public double puntaje;
        public String estado;
        public String ayuda;
        public String remediacion;
        public List<DetalleDTO> detalle = new ArrayList<>();
    }

    // =================== Nivel 4: origen fisico ===================
    public static class DetalleDTO {
        public String nombre;
        public String origen;        // la ruta en disco
        public Double valor;
        public String valorTexto;
        public String estado;
        public String nota;
    }
}