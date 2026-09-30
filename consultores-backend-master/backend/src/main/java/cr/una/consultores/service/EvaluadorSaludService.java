package cr.una.consultores.service;

import cr.una.consultores.dto.LecturaCrudaDTO;
import cr.una.consultores.dto.ReporteAgenteDTO;
import cr.una.consultores.dto.SaludInstanciaDTO;
import cr.una.consultores.dto.SaludInstanciaDTO.*;
import cr.una.consultores.monitor.CatalogoVariables;
import cr.una.consultores.monitor.VariableDef;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Convierte lecturas crudas en el arbol evaluado de tres niveles.
 *
 * Es el UNICO lugar donde se decide si un valor esta bien o mal. Las dos
 * instancias pasan por aqui: la autonoma que lee el backend en vivo y la
 * tradicional que reporta un agente. Por eso sus indices son comparables.
 *
 * TRES REGLAS QUE VIENEN DE LA REVISION
 *
 * 1. ARCHIVOS NO PROMEDIA. Toma el peor caso. Un tablespace al 99 % con
 *    el resto vacio da un promedio saludable, y la base colapsa igual al
 *    no poder escribir en el. La analogia de la revision: el resto del
 *    sistema circulatorio puede estar limpio, pero una sola arteria
 *    obstruida produce el infarto.
 *
 * 2. PREVALENCIA DEL CRITICO. Si cualquier variable con peso queda en
 *    critico, el estado global pasa a critico sin importar el promedio, y
 *    se listan las causas. Un indice alto no puede ocultar un componente
 *    en falla.
 *
 * 3. LOS UMBRALES SE MUESTRAN. Cada variable viaja con su consulta, sus
 *    limites y la fuente del umbral, porque se pidio poder demostrar de
 *    donde sale cada numero.
 */
@Service
public class EvaluadorSaludService {

    private final CatalogoVariables catalogo;

    public EvaluadorSaludService(CatalogoVariables catalogo) {
        this.catalogo = catalogo;
    }

    public static final String FORMULA = "0.30(IP) + 0.35(IM) + 0.35(IA)";

    public SaludInstanciaDTO evaluar(ReporteAgenteDTO reporte, String origen) {
        SaludInstanciaDTO d = new SaludInstanciaDTO();
        d.instanciaId = reporte.instanciaId;
        d.nombre = reporte.nombre;
        d.empresa = reporte.empresa;
        d.tipo = reporte.tipo == null ? "tradicional" : reporte.tipo;
        d.origen = origen;
        d.ultimaLectura = reporte.ultimaLectura;
        d.conectado = reporte.baseAccesible;
        d.formula = FORMULA;

        // La base no respondio: se reporta caida en vez de inventar un indice
        if (!reporte.baseAccesible) {
            d.isbd = 0;
            d.estado = "critical";
            d.causasCriticas.add("No se pudo leer la instancia" +
                    (reporte.errorLectura == null ? "" : ": " + reporte.errorLectura));
            return d;
        }

        Map<String, LecturaCrudaDTO> porCodigo = new HashMap<>();
        for (LecturaCrudaDTO l : reporte.lecturas) {
            if (l.codigo != null) porCodigo.put(l.codigo, l);
        }

        double isbd = 0;
        int pesoTotalUsado = 0;

        for (String cod : List.of("IP", "IM", "IA")) {
            IndicadorDTO ind = construirIndicador(cod, porCodigo, d.causasCriticas);
            if (ind == null) continue;
            d.indicadores.add(ind);
            int peso = CatalogoVariables.PESO_INDICADOR.getOrDefault(cod, 0);
            isbd += ind.puntaje * peso / 100.0;
            pesoTotalUsado += peso;
        }

        // Si falto algun indicador (el agente no reporto nada de esa area),
        // se reescala en vez de castigar con ceros lo que no se midio.
        if (pesoTotalUsado > 0 && pesoTotalUsado < 100) {
            isbd = isbd * 100 / pesoTotalUsado;
        }

        d.isbdPonderado = Math.round(isbd * 10) / 10.0;

        // Un indice de 96 junto a un estado critico se contradice a simple
        // vista. Cuando hay una variable en falla, el indice baja a la banda
        // critica: el promedio ponderado no puede tapar el componente roto.
        // El valor sin topar se conserva en isbdPonderado para no ocultar
        // nada: la interfaz puede mostrar los dos y explicar la diferencia.
        d.isbd = d.causasCriticas.isEmpty()
                ? d.isbdPonderado
                : Math.min(d.isbdPonderado, 39.0);
        d.estado = d.causasCriticas.isEmpty() ? escala(d.isbd) : "critical";
        return d;
    }

    // -----------------------------------------------------------------
    private IndicadorDTO construirIndicador(String codigo,
                                            Map<String, LecturaCrudaDTO> lecturas,
                                            List<String> causasCriticas) {
        List<VariableDef> defs = catalogo.deIndicador(codigo);
        if (defs.isEmpty()) return null;

        IndicadorDTO ind = new IndicadorDTO();
        ind.codigo = codigo;
        ind.nombre = CatalogoVariables.NOMBRE_INDICADOR.get(codigo);
        ind.peso = CatalogoVariables.PESO_INDICADOR.getOrDefault(codigo, 0);

        boolean esArchivos = "IA".equals(codigo);
        ind.agregacion = esArchivos ? "peor_caso" : "ponderado";
        ind.notaAgregacion = esArchivos
                ? "Se toma el peor archivo, nunca el promedio: un solo tablespace lleno detiene la escritura sobre él aunque el resto esté vacío."
                : "Promedio ponderado por la criticidad de cada variable.";

        // Agrupar por grupo, respetando el orden del catalogo
        Map<String, GrupoDTO> grupos = new LinkedHashMap<>();
        boolean hubo = false;

        for (VariableDef def : defs) {
            LecturaCrudaDTO l = lecturas.get(def.codigo);
            if (l == null) continue;          // el agente no la reporto
            hubo = true;

            VariableDTO v = construirVariable(def, l);
            GrupoDTO g = grupos.computeIfAbsent(def.grupo, k -> {
                GrupoDTO nuevo = new GrupoDTO();
                nuevo.codigo = k;
                nuevo.nombre = CatalogoVariables.NOMBRE_GRUPO.getOrDefault(k, k);
                return nuevo;
            });
            g.variables.add(v);
            g.peso += v.peso;

            if ("critico".equals(v.estado) && v.peso > 0) {
                causasCriticas.add(ind.nombre + " · " + v.nombre +
                        " = " + textoValor(v) + " (límite " + v.rangoNormal + ")");
            }
        }
        if (!hubo) return null;

        // Puntaje de cada grupo: ponderado entre sus variables.
        // El estado critico SUBE por el arbol: un grupo que contiene una
        // variable en falla no puede mostrarse en verde porque el resto
        // de sus variables compense el promedio.
        for (GrupoDTO g : grupos.values()) {
            g.puntaje = Math.round(ponderar(g.variables) * 10) / 10.0;
            g.estado = peorEstado(estadoDePuntaje(g.puntaje), g.variables);
        }
        ind.grupos.addAll(grupos.values());

        // Puntaje del indicador
        List<VariableDTO> todas = new ArrayList<>();
        grupos.values().forEach(g -> todas.addAll(g.variables));

        if (esArchivos) {
            // REGLA DEL PEOR CASO: manda la variable con peor puntaje entre
            // las que tienen peso. No se promedia.
            ind.puntaje = todas.stream()
                    .filter(v -> v.peso > 0)
                    .mapToDouble(v -> v.puntaje)
                    .min().orElse(100);
        } else {
            ind.puntaje = ponderar(todas);
        }
        ind.puntaje = Math.round(ind.puntaje * 10) / 10.0;
        ind.estado = peorEstado(estadoDePuntaje(ind.puntaje), todas);
        return ind;
    }

    /** Devuelve el peor entre el estado calculado y el de sus variables. */
    private String peorEstado(String calculado, List<VariableDTO> vars) {
        boolean critico = vars.stream().anyMatch(v -> v.peso > 0 && "critico".equals(v.estado));
        if (critico) return "critico";
        boolean adv = vars.stream().anyMatch(v -> v.peso > 0 && "advertencia".equals(v.estado));
        if (adv && "normal".equals(calculado)) return "advertencia";
        return calculado;
    }

    private double ponderar(List<VariableDTO> vars) {
        int peso = 0; double suma = 0;
        for (VariableDTO v : vars) {
            if (v.peso <= 0) continue;     // las de referencia no puntuan
            suma += v.puntaje * v.peso;
            peso += v.peso;
        }
        return peso == 0 ? 100 : suma / peso;
    }

    private VariableDTO construirVariable(VariableDef def, LecturaCrudaDTO l) {
        VariableDTO v = new VariableDTO();
        v.codigo = def.codigo;
        v.nombre = def.nombre;
        v.valor = l.valor;
        v.valorTexto = l.valorTexto;
        v.unidad = def.unidad;
        v.consulta = def.consulta;
        v.limiteInferior = def.limiteInferior();
        v.limiteSuperior = def.limiteSuperior();
        v.rangoNormal = rangoTexto(def);
        v.direccion = def.direccion.name();
        v.fuenteUmbral = def.fuente.name();
        v.peso = catalogo.peso(def.codigo);
        v.puntaje = Math.round(def.puntuar(l.valor) * 10) / 10.0;
        v.estado = def.evaluar(l.valor);
        v.ayuda = def.ayuda;
        v.remediacion = "normal".equals(v.estado) ? null : def.remediacion;

        // Nivel 3: el origen fisico de cada valor
        if (l.detalle != null) {
            for (LecturaCrudaDTO.Detalle det : l.detalle) {
                DetalleDTO dd = new DetalleDTO();
                dd.nombre = det.nombre;
                dd.origen = det.origen;
                dd.valor = det.valor;
                dd.valorTexto = det.valorTexto;
                dd.nota = det.nota;
                dd.estado = def.evaluar(det.valor);
                v.detalle.add(dd);
            }
            // El peor primero: es el que explica el estado de la variable
            v.detalle.sort(Comparator.comparingInt(x -> switch (x.estado) {
                case "critico" -> 0; case "advertencia" -> 1; default -> 2; }));
        }
        return v;
    }

    private String rangoTexto(VariableDef def) {
        return switch (def.direccion) {
            case REFERENCIA -> "Informativa, sin umbral";
            case BOOLEANA   -> "Debe cumplirse";
            case ALTO_MALO  -> "Hasta " + fmt(def.umbralAdvertencia) + def.unidad +
                    " · crítico desde " + fmt(def.umbralCritico) + def.unidad;
            case BAJO_MALO  -> "Desde " + fmt(def.umbralAdvertencia) + def.unidad +
                    " · crítico bajo " + fmt(def.umbralCritico) + def.unidad;
        };
    }

    private String fmt(Double d) {
        if (d == null) return "—";
        return d == Math.floor(d) ? String.valueOf(d.intValue()) : String.valueOf(d);
    }

    private String textoValor(VariableDTO v) {
        if (v.valorTexto != null) return v.valorTexto;
        if (v.valor == null) return "—";
        return fmt(v.valor) + (v.unidad == null ? "" : v.unidad);
    }

    private String estadoDePuntaje(double p) {
        if (p < 40) return "critico";
        if (p < 75) return "advertencia";
        return "normal";
    }

    public static String escala(double v) {
        if (v >= 90) return "optimal";
        if (v >= 75) return "healthy";
        if (v >= 60) return "warning";
        if (v >= 40) return "degraded";
        return "critical";
    }
}