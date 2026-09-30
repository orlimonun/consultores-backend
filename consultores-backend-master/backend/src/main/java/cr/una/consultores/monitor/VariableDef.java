package cr.una.consultores.monitor;

/**
 * Definicion de UNA variable del monitor.
 *
 * Aqui viven los umbrales, no en el agente. Razones:
 *   1. Se cambian en un solo lugar y valen para todas las instancias.
 *   2. Se pueden exponer en la interfaz, que es lo que se pidio en la
 *      revision: los rangos tienen que ser demostrables en pantalla, no
 *      estar escondidos en el codigo.
 *   3. El agente queda tonto a proposito: solo lee y reporta numeros
 *      crudos. Cambiar un umbral no obliga a redesplegar el jar en la
 *      maquina del cliente.
 *
 * El peso NO se escribe a mano: se deriva de velocidad x alcance y se
 * normaliza dentro de cada indicador. Asi cada peso tiene una
 * justificacion reproducible en vez de ser un numero de criterio.
 */
public class VariableDef {

    public enum Direccion {
        ALTO_MALO,     // cuanto mas alto, peor (ej: sesiones bloqueadas)
        BAJO_MALO,     // cuanto mas bajo, peor (ej: buffer cache hit)
        BOOLEANA,      // 1 = correcto, 0 = incorrecto (ej: modo ARCHIVELOG)
        REFERENCIA     // informativa: no tiene umbral ni afecta el puntaje
    }

    /** De donde sale el umbral. Se muestra en la interfaz para poder
     *  responder "de donde sacaron ese numero". */
    public enum Fuente {
        DOCUMENTO,    // esta en la especificacion del curso
        REVISION,     // esta en la tabla entregada en la revision
        MEDICION,     // se fijo midiendo la base real
        CONVENCION,   // practica habitual de Oracle, aun sin validar aqui
        SIN_UMBRAL    // variable informativa: no tiene umbral que citar
    }

    // Nota: esta constante NO se llama REFERENCIA aunque seria el nombre
    // natural. Direccion ya tiene una REFERENCIA, y como las dos enums se
    // importan estaticamente en el catalogo, el nombre repetido vuelve
    // ambigua cada referencia. SIN_UMBRAL ademas describe mejor el caso.

    public final String codigo;        // p1, m4, a6...
    public final String indicador;     // IP | IM | IA
    public final String grupo;         // PMON | SGA | TABLESPACES...
    public final String nombre;
    public final String unidad;
    public final String consulta;      // el SQL exacto, visible en la interfaz
    public final Direccion direccion;
    public final Double umbralAdvertencia;
    public final Double umbralCritico;
    public final Fuente fuente;
    public final int velocidad;        // 1-5: que tan rapido tumba la base
    public final int alcance;          // 1,3,5: a quien afecta
    public final String ayuda;
    public final String remediacion;   // que hacer si esta fuera de rango

    public VariableDef(String codigo, String indicador, String grupo, String nombre,
                       String unidad, String consulta, Direccion direccion,
                       Double umbralAdvertencia, Double umbralCritico, Fuente fuente,
                       int velocidad, int alcance, String ayuda, String remediacion) {
        this.codigo = codigo; this.indicador = indicador; this.grupo = grupo;
        this.nombre = nombre; this.unidad = unidad; this.consulta = consulta;
        this.direccion = direccion; this.umbralAdvertencia = umbralAdvertencia;
        this.umbralCritico = umbralCritico; this.fuente = fuente;
        this.velocidad = velocidad; this.alcance = alcance;
        this.ayuda = ayuda; this.remediacion = remediacion;
    }

    /** Peso bruto antes de normalizar: velocidad x alcance. */
    public int pesoBruto() {
        return direccion == Direccion.REFERENCIA ? 0 : velocidad * alcance;
    }

    /** Limite inferior del rango aceptable, para mostrar en la tabla. */
    public Double limiteInferior() {
        if (direccion == Direccion.ALTO_MALO) return Double.valueOf(0);
        if (direccion == Direccion.BAJO_MALO) return umbralAdvertencia;
        return null;
    }

    /** Limite superior del rango aceptable, para mostrar en la tabla. */
    public Double limiteSuperior() {
        if (direccion == Direccion.ALTO_MALO) return umbralAdvertencia;
        return null;
    }

    /**
     * Evalua un valor contra los umbrales. Devuelve normal|advertencia|critico.
     *
     * CONVENCION: el valor del umbral SI es aceptable; el problema empieza
     * al pasarlo. "Hasta 70 %" significa que 70 esta bien y 70,1 ya no.
     * Es como se plantean los rangos permitidos en la especificacion, y
     * evita el caso incomodo de una variable marcada en advertencia con
     * puntaje 100 porque evaluar y puntuar usaban comparadores distintos.
     */
    public String evaluar(Double valor) {
        if (direccion == Direccion.REFERENCIA || valor == null) return "normal";
        if (direccion == Direccion.BOOLEANA) return valor >= 1 ? "normal" : "critico";
        if (direccion == Direccion.ALTO_MALO) {
            if (umbralCritico != null && valor > umbralCritico) return "critico";
            if (umbralAdvertencia != null && valor > umbralAdvertencia) return "advertencia";
            return "normal";
        }
        // BAJO_MALO
        if (umbralCritico != null && valor < umbralCritico) return "critico";
        if (umbralAdvertencia != null && valor < umbralAdvertencia) return "advertencia";
        return "normal";
    }

    /**
     * Puntaje de 0 a 100. La escala no es lineal a proposito: entre el
     * umbral de advertencia y el critico cae de 100 a 40, y a partir del
     * critico de 40 a 0. Asi una variable en advertencia ya pesa en el
     * indicador, pero solo una critica lo hunde.
     */
    public double puntuar(Double valor) {
        if (direccion == Direccion.REFERENCIA || valor == null) return 100;
        if (direccion == Direccion.BOOLEANA) return valor >= 1 ? 100 : 0;

        double adv = umbralAdvertencia == null ? 0 : umbralAdvertencia;
        double cri = umbralCritico == null ? adv : umbralCritico;

        if (direccion == Direccion.ALTO_MALO) {
            if (valor <= adv) return 100;
            if (valor > cri) {
                double exceso = (valor - cri) / Math.max(cri, 1);
                return Math.max(0, 40 - exceso * 40);
            }
            return 100 - ((valor - adv) / Math.max(cri - adv, 1e-9)) * 60;
        }
        if (valor >= adv) return 100;
        if (valor < cri) {
            double falta = (cri - valor) / Math.max(cri, 1);
            return Math.max(0, 40 - falta * 40);
        }
        return 40 + ((valor - cri) / Math.max(adv - cri, 1e-9)) * 60;
    }
}