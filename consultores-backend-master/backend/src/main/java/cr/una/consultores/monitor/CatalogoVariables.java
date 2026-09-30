package cr.una.consultores.monitor;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;

// Direccion se importa con comodin porque sus constantes son unicas.
// Fuente se importa una por una a proposito: importar las dos con comodin
// hacia ambigua cualquier constante con el mismo nombre en ambas enums.
import static cr.una.consultores.monitor.VariableDef.Direccion.*;
import static cr.una.consultores.monitor.VariableDef.Fuente.CONVENCION;
import static cr.una.consultores.monitor.VariableDef.Fuente.DOCUMENTO;
import static cr.una.consultores.monitor.VariableDef.Fuente.MEDICION;
import static cr.una.consultores.monitor.VariableDef.Fuente.REVISION;
import static cr.una.consultores.monitor.VariableDef.Fuente.SIN_UMBRAL;

/**
 * Catalogo de las variables del monitor: las 25 de la especificacion mas
 * las agregadas, con su consulta, sus umbrales y su calificacion de
 * criticidad.
 *
 * ESTRUCTURA DE TRES NIVELES
 *   Indicador (IP, IM, IA)  ->  Grupo  ->  Variable  ->  Detalle fisico
 * Los grupos de procesos son los cuatro procesos de background
 * mandatorios, como se pidio en la revision.
 *
 * SOBRE LAS VARIABLES DESCARTADAS
 * Ninguna se descarto. Las que en la version anterior faltaban (a3, a5,
 * a6) estan aqui. Las que no aportan condicion propia quedan marcadas
 * como REFERENCIA: se muestran pero no puntuan, porque describen
 * capacidad instalada y no un estado que pueda degradarse.
 *
 * POR QUE ESTA EN CODIGO Y NO EN LA BASE
 * El esquema de Postgres esta en ddl-auto=validate: agregar una entidad
 * sin crear antes su tabla impide que la aplicacion arranque. El catalogo
 * es mutable en caliente a traves del endpoint de configuracion, asi que
 * los umbrales se pueden ajustar sin redesplegar. Pasarlo a tabla cuando
 * se agregue el historico es un cambio contenido.
 */
@Component
public class CatalogoVariables {

    private final Map<String, VariableDef> porCodigo = new LinkedHashMap<>();
    private final Map<String, Integer> pesoNormalizado = new HashMap<>();

    // Pesos de los tres indicadores dentro del ISBD (especificacion del curso)
    public static final Map<String, Integer> PESO_INDICADOR =
            Map.of("IP", 30, "IM", 35, "IA", 35);

    public static final Map<String, String> NOMBRE_INDICADOR = Map.of(
            "IP", "Procesos", "IM", "Memoria", "IA", "Archivos");

    public static final Map<String, String> NOMBRE_GRUPO = new LinkedHashMap<>();
    static {
        NOMBRE_GRUPO.put("PMON", "PMON · Limpieza de procesos");
        NOMBRE_GRUPO.put("SMON", "SMON · Mantenimiento del sistema");
        NOMBRE_GRUPO.put("DBWR", "DBWR · Escritura de bloques");
        NOMBRE_GRUPO.put("LGWR", "LGWR · Escritura de redo");
        NOMBRE_GRUPO.put("CARGA", "Carga general de sesiones");
        NOMBRE_GRUPO.put("SGA", "SGA · Memoria compartida");
        NOMBRE_GRUPO.put("PGA", "PGA · Memoria privada");
        NOMBRE_GRUPO.put("DATAFILES", "Datafiles");
        NOMBRE_GRUPO.put("TABLESPACES", "Tablespaces");
        NOMBRE_GRUPO.put("TEMPFILES", "Tempfiles");
        NOMBRE_GRUPO.put("BITACORAS", "Bitácoras y recuperación");
        NOMBRE_GRUPO.put("INTEGRIDAD", "Integridad de archivos");
    }

    private void v(VariableDef d) { porCodigo.put(d.codigo, d); }

    @PostConstruct
    void cargar() {
        // =================================================================
        //  IP · PROCESOS
        // =================================================================
        v(new VariableDef("p1","IP","PMON","Uso del límite de procesos","%",
                "SELECT current_utilization, limit_value FROM v$resource_limit WHERE resource_name='processes'",
                ALTO_MALO, 70.0, 85.0, DOCUMENTO, 5, 5,
                "Al agotarse el límite, Oracle rechaza toda conexión nueva con ORA-00020 aunque el servidor tenga recursos libres.",
                "Aumentar el parámetro processes y reiniciar la instancia, o revisar por qué se abren tantas conexiones."));

        v(new VariableDef("p2","IP","PMON","Pico histórico de procesos","%",
                "SELECT max_utilization, limit_value FROM v$resource_limit WHERE resource_name='processes'",
                ALTO_MALO, 80.0, 95.0, CONVENCION, 2, 5,
                "Mayor cantidad simultánea desde que arrancó la instancia. Revela saturaciones que un muestreo periódico no alcanza a ver.",
                "Si el pico se acerca al límite, dimensionar processes sobre el pico y no sobre el promedio."));

        v(new VariableDef("p3","IP","CARGA","Sesiones actuales","",
                "SELECT COUNT(*) FROM v$session",
                REFERENCIA, null, null, SIN_UMBRAL, 1, 1,
                "Total de sesiones conectadas, incluidas las de fondo de la propia instancia.", null));

        v(new VariableDef("p4","IP","CARGA","Sesiones activas","",
                "SELECT COUNT(*) FROM v$session WHERE status='ACTIVE'",
                REFERENCIA, null, null, SIN_UMBRAL, 2, 3,
                "Sesiones ejecutando trabajo en este momento.", null));

        v(new VariableDef("p5","IP","CARGA","Sesiones inactivas","",
                "SELECT COUNT(*) FROM v$session WHERE status='INACTIVE'",
                REFERENCIA, null, null, SIN_UMBRAL, 1, 1,
                "Conectadas pero sin actividad. Solo importan si empujan contra el límite de sesiones.", null));

        v(new VariableDef("p6","IP","PMON","Sesiones bloqueadas","",
                "SELECT COUNT(*) FROM v$session WHERE blocking_session IS NOT NULL",
                ALTO_MALO, 0.0, 5.0, CONVENCION, 4, 3,
                "Sesiones detenidas esperando un recurso que otra retiene. En cadena pueden paralizar un módulo completo.",
                "Identificar la sesión bloqueante con V$SESSION.blocking_session y evaluar si debe terminarse."));

        v(new VariableDef("p7","IP","SMON","Operaciones prolongadas","",
                "SELECT COUNT(*) FROM v$session_longops WHERE time_remaining > 0",
                REFERENCIA, null, null, SIN_UMBRAL, 2, 1,
                "Operaciones largas en curso. Una operación larga puede ser perfectamente legítima.", null));

        v(new VariableDef("p8","IP","PMON","Uso del límite de sesiones","%",
                "SELECT current_utilization, limit_value FROM v$resource_limit WHERE resource_name='sessions'",
                ALTO_MALO, 70.0, 85.0, DOCUMENTO, 5, 5,
                "Mismo riesgo que el límite de procesos: al agotarse, no entran conexiones nuevas.",
                "Aumentar el parámetro sessions junto con processes."));

        v(new VariableDef("p9","IP","PMON","Sesiones zombis","",
                "SELECT COUNT(*) FROM v$session WHERE status IN ('KILLED','SNIPED')",
                ALTO_MALO, 0.0, 10.0, CONVENCION, 3, 3,
                "Sesiones terminadas que PMON aún no limpió. Si se acumulan, ocupan lugar en la tabla de procesos.",
                "Normalmente se resuelven solas. Si persisten, revisar la carga sobre PMON."));

        v(new VariableDef("p10","IP","SMON","Segmentos temporales pendientes","",
                "SELECT COUNT(*) FROM dba_segments WHERE segment_type IN ('TEMPORARY','DEFERRED ROLLBACK')",
                ALTO_MALO, 5.0, 20.0, CONVENCION, 2, 3,
                "Espacio que SMON aún no ha recuperado. La acumulación indica que la limpieza va atrasada.",
                "Verificar que SMON esté vivo y que no haya transacciones colgadas."));

        v(new VariableDef("p11","IP","SMON","Transacciones en recuperación","",
                "SELECT COUNT(*) FROM v$fast_start_transactions",
                ALTO_MALO, 0.0, 5.0, CONVENCION, 3, 5,
                "SMON está rehaciendo trabajo tras una caída. Mientras dura, afecta a toda la instancia.",
                "Esperar a que termine. Si no avanza, revisar el alert log."));

        v(new VariableDef("p12","IP","DBWR","Free buffer waits","",
                "SELECT total_waits FROM v$system_event WHERE event='free buffer waits'",
                ALTO_MALO, 0.0, 100.0, CONVENCION, 5, 5,
                "Cada espera es una sesión detenida porque no había un buffer libre. Es el síntoma más grave del monitor: DBWR no da abasto y la base se frena entera.",
                "Aumentar el buffer cache, revisar el subsistema de disco, o considerar más procesos DBWR con db_writer_processes."));

        v(new VariableDef("p13","IP","DBWR","Tiempo de escritura de DBWR","ms",
                "SELECT time_waited_micro/total_waits/1000 FROM v$system_event WHERE event='db file parallel write'",
                ALTO_MALO, 10.0, 30.0, CONVENCION, 4, 5,
                "Cuánto tarda DBWR en bajar bloques al disco. Valores altos anticipan las free buffer waits.",
                "Revisar el rendimiento del almacenamiento donde viven los datafiles."));

        v(new VariableDef("p14","IP","DBWR","Buffers inspeccionados por solicitud","",
                "SELECT 'free buffer inspected'/'free buffer requested' FROM v$sysstat",
                ALTO_MALO, 2.0, 10.0, CONVENCION, 4, 5,
                "Cuántos buffers hay que revisar para encontrar uno libre. Una relación alta significa que casi todos están sucios.",
                "Mismo remedio que las free buffer waits: más buffer cache o disco más rápido."));

        v(new VariableDef("p15","IP","LGWR","Log file sync","ms",
                "SELECT time_waited_micro/total_waits/1000 FROM v$system_event WHERE event='log file sync'",
                ALTO_MALO, 20.0, 50.0, CONVENCION, 4, 5,
                "Cuánto espera un COMMIT a que LGWR confirme la escritura. Es lo que el usuario final percibe como lentitud al guardar.",
                "Ubicar las bitácoras en almacenamiento más rápido o reducir la frecuencia de commits."));

        v(new VariableDef("p16","IP","LGWR","Redo log space requests","",
                "SELECT value FROM v$sysstat WHERE name='redo log space requests'",
                ALTO_MALO, 0.0, 50.0, CONVENCION, 3, 5,
                "Sesiones que esperaron espacio en el buffer de redo. Cualquier crecimiento sostenido indica que el buffer se queda corto.",
                "Aumentar el parámetro log_buffer o agrandar los grupos de bitácora."));

        // =================================================================
        //  IM · MEMORIA
        // =================================================================
        v(new VariableDef("m1","IM","SGA","Tamaño total de la SGA","MB",
                "SELECT SUM(bytes) FROM v$sgastat",
                REFERENCIA, null, null, SIN_UMBRAL, 1, 5,
                "Capacidad instalada de memoria compartida. No es una condición que pueda degradarse.", null));

        v(new VariableDef("m2","IM","SGA","SGA sin asignar a componentes","MB",
                "SELECT bytes FROM v$sgainfo WHERE name='Free SGA Memory Available'",
                REFERENCIA, null, null, SIN_UMBRAL, 1, 3,
                "Memoria de la SGA que Oracle aún no repartió entre sus componentes. Que sea cero es lo normal y lo deseable: significa que toda la SGA está asignada. NO mide ocupación, y usarla como tal fue el origen del falso 87 % de la versión anterior.", null));

        v(new VariableDef("m3","IM","SGA","Shared Pool libre","%",
                "SELECT SUM(CASE WHEN name='free memory' THEN bytes END)*100/SUM(bytes) FROM v$sgastat WHERE pool='shared pool'",
                BAJO_MALO, 15.0, 5.0, REVISION, 4, 5,
                "Aquí viven los planes de ejecución ya analizados. Si se agota, Oracle expulsa planes y vuelve a analizar cada consulta, y puede fallar con ORA-04031. Este es el componente donde el porcentaje libre sí es informativo.",
                "Aumentar shared_pool_size, o revisar si hay SQL sin variables de enlace llenando la caché."));

        v(new VariableDef("m4","IM","SGA","Buffer Cache Hit Ratio","%",
                "SELECT (1-(physical reads/(db block gets + consistent gets)))*100 FROM v$sysstat",
                BAJO_MALO, 85.0, 80.0, REVISION, 3, 5,
                "Proporción de lecturas resueltas en memoria. Un valor alto NO garantiza salud: una consulta mal escrita que recorre la misma tabla en ciclo produce un ratio excelente con rendimiento pésimo. Se lee junto a m10.",
                "Si es bajo con esperas de lectura altas, aumentar el buffer cache."));

        v(new VariableDef("m5","IM","PGA","PGA asignada sobre el objetivo","%",
                "SELECT 'total PGA allocated'*100/'aggregate PGA target parameter' FROM v$pgastat",
                REFERENCIA, null, null, MEDICION, 2, 3,
                "Oracle no devuelve la PGA asignada de inmediato, así que este valor ronda el 100 % incluso en una base ociosa. Medido en la instancia de referencia dio 97,75 % sin un solo ordenamiento en disco. Por eso es referencia y no condición: lo que diagnostica es m8 y m9.", null));

        v(new VariableDef("m6","IM","PGA","PGA en uso","MB",
                "SELECT value FROM v$pgastat WHERE name='total PGA inuse'",
                REFERENCIA, null, null, SIN_UMBRAL, 1, 3,
                "Memoria privada realmente ocupada en este momento.", null));

        v(new VariableDef("m7","IM","PGA","PGA máxima histórica","MB",
                "SELECT value FROM v$pgastat WHERE name='maximum PGA allocated'",
                REFERENCIA, null, null, SIN_UMBRAL, 1, 3,
                "Pico alcanzado desde el arranque. Sirve para dimensionar el objetivo: si supera con holgura al objetivo configurado, el objetivo está corto.", null));

        v(new VariableDef("m8","IM","PGA","Over-allocation count","",
                "SELECT value FROM v$pgastat WHERE name='over allocation count'",
                ALTO_MALO, 0.0, 50.0, REVISION, 4, 5,
                "Veces que Oracle tuvo que exceder el objetivo de PGA. Cualquier valor mayor que cero indica que el objetivo se queda corto para la carga real.",
                "Aumentar pga_aggregate_target. En la instancia de referencia el pico llegó al 145 % del objetivo."));

        v(new VariableDef("m9","IM","PGA","Cache hit de PGA","%",
                "SELECT value FROM v$pgastat WHERE name='cache hit percentage'",
                BAJO_MALO, 90.0, 75.0, REVISION, 3, 3,
                "Proporción de operaciones de ordenamiento y combinación resueltas enteramente en memoria.",
                "Aumentar pga_aggregate_target si baja de forma sostenida."));

        v(new VariableDef("m10","IM","SGA","Espera por lectura física","ms",
                "SELECT time_waited_micro/total_waits/1000 FROM v$system_event WHERE event='db file sequential read'",
                ALTO_MALO, 10.0, 20.0, CONVENCION, 4, 5,
                "Cuánto cuesta ir al disco cuando el bloque no está en caché. Es el complemento de m4: distingue un hit ratio alto sano de uno inflado por una consulta en ciclo.",
                "Revisar el rendimiento del almacenamiento o aumentar el buffer cache."));

        v(new VariableDef("m11","IM","PGA","Ordenamientos resueltos en disco","%",
                "SELECT 'sorts (disk)'*100/('sorts (memory)'+'sorts (disk)') FROM v$sysstat",
                ALTO_MALO, 0.5, 5.0, CONVENCION, 3, 3,
                "Consecuencia visible de la presión sobre PGA: un ordenamiento que no cabe en memoria se resuelve en el tablespace temporal y se vuelve órdenes de magnitud más lento.",
                "Aumentar pga_aggregate_target."));

        // =================================================================
        //  IA · ARCHIVOS
        // =================================================================
        v(new VariableDef("a1","IA","DATAFILES","Datafiles online","",
                "SELECT COUNT(*) FROM v$datafile WHERE status IN ('ONLINE','SYSTEM')",
                REFERENCIA, null, null, SIN_UMBRAL, 1, 5,
                "Cantidad de datafiles operativos. La condición se evalúa en a2.", null));

        v(new VariableDef("a2","IA","DATAFILES","Datafiles offline","",
                "SELECT COUNT(*) FROM v$datafile WHERE status NOT IN ('ONLINE','SYSTEM')",
                ALTO_MALO, 0.0, 0.0, DOCUMENTO, 5, 5,
                "Un datafile fuera de línea deja inaccesible de inmediato todo lo que contiene.",
                "Identificar el archivo y ponerlo en línea: ALTER DATABASE DATAFILE '<ruta>' ONLINE."));

        v(new VariableDef("a3","IA","DATAFILES","Datafiles sin crecimiento automático","",
                "SELECT COUNT(*) FROM dba_data_files WHERE autoextensible='NO'",
                ALTO_MALO, 0.0, 3.0, CONVENCION, 3, 3,
                "Un datafile sin AUTOEXTEND se llena y detiene la escritura sin margen de reacción. Esta variable faltaba en la versión anterior del monitor.",
                "ALTER DATABASE DATAFILE '<ruta>' AUTOEXTEND ON NEXT 50M MAXSIZE UNLIMITED;"));

        v(new VariableDef("a4","IA","TABLESPACES","Ocupación del peor tablespace","%",
                "SELECT MAX(used_percent) FROM dba_tablespace_usage_metrics",
                ALTO_MALO, 70.0, 85.0, DOCUMENTO, 4, 5,
                "Se toma el peor, nunca el promedio. Un solo tablespace al 99 % con el resto vacío da un promedio saludable y la base colapsa igual al no poder escribir en él. El cálculo usa el tamaño máximo al que puede crecer el archivo, no el actual: con AUTOEXTEND el archivo siempre está casi lleno por diseño.",
                "Ampliar el datafile del tablespace afectado o agregarle uno nuevo."));

        v(new VariableDef("a5","IA","TEMPFILES","Ocupación del tablespace temporal","%",
                "SELECT (allocated_space-free_space)*100/tablespace_size FROM dba_temp_free_space",
                ALTO_MALO, 70.0, 90.0, CONVENCION, 4, 3,
                "Aquí se resuelven los ordenamientos que no caben en PGA. Un temporal lleno aborta la consulta con ORA-01652. Esta variable faltaba en la versión anterior.",
                "Agregar un tempfile o aumentar pga_aggregate_target para que menos operaciones lleguen aquí."));

        v(new VariableDef("a6","IA","BITACORAS","Grupos de bitácora sin espejo","",
                "SELECT SUM(CASE WHEN members<2 THEN 1 ELSE 0 END) FROM v$log",
                ALTO_MALO, 0.0, 1.0, CONVENCION, 4, 5,
                "Un grupo con un solo miembro es un punto único de falla: si ese archivo se corrompe, el grupo queda inservible y recuperarlo implica perder transacciones. Esta variable faltaba en la versión anterior.",
                "ALTER DATABASE ADD LOGFILE MEMBER '<ruta en otro disco>' TO GROUP <n>;"));

        v(new VariableDef("a7","IA","INTEGRIDAD","Archivos en estado inválido","",
                "SELECT COUNT(*) FROM v$logfile WHERE status IN ('INVALID','STALE')",
                ALTO_MALO, 0.0, 2.0, CONVENCION, 4, 3,
                "Archivo presente pero en estado anómalo. Falla al usarse, no antes, así que el diccionario puede no reflejarlo hasta el siguiente intento de escritura.",
                "Eliminar y volver a agregar el miembro afectado."));

        v(new VariableDef("a8","IA","INTEGRIDAD","Archivos que requieren recuperación","",
                "SELECT COUNT(*) FROM v$recover_file",
                ALTO_MALO, 0.0, 0.0, DOCUMENTO, 5, 5,
                "Condición crítica inmediata: la base no puede operar sobre ese archivo.",
                "Restaurar desde respaldo y aplicar el redo: RESTORE DATAFILE / RECOVER DATAFILE."));

        v(new VariableDef("a9","IA","BITACORAS","Modo de archivado","",
                "SELECT CASE WHEN log_mode='ARCHIVELOG' THEN 1 ELSE 0 END FROM v$database",
                BOOLEANA, null, null, DOCUMENTO, 3, 5,
                "Sin archivado, el redo se sobrescribe al completar el ciclo de bitácoras y no hay recuperación posible ante un fallo de medios: solo se puede restaurar hasta el último respaldo.",
                "SHUTDOWN IMMEDIATE; STARTUP MOUNT; ALTER DATABASE ARCHIVELOG; ALTER DATABASE OPEN;"));

        v(new VariableDef("a10","IA","BITACORAS","Ocupación del área de recuperación","%",
                "SELECT SUM(percent_space_used) FROM v$recovery_area_usage",
                ALTO_MALO, 80.0, 90.0, CONVENCION, 5, 5,
                "Si el área se llena, la instancia completa se detiene con ORA-00257. Si no está configurada, las bitácoras archivadas caen en una carpeta sin control de espacio y fuera del alcance de RMAN.",
                "Purgar con RMAN (DELETE OBSOLETE) o aumentar db_recovery_file_dest_size."));

        v(new VariableDef("a11","IA","BITACORAS","Grupos de bitácora","",
                "SELECT COUNT(*) FROM v$log",
                BAJO_MALO, 3.0, 2.0, CONVENCION, 3, 5,
                "Con pocos grupos, si el archivador se atrasa el motor llega al siguiente antes de que el anterior se haya archivado y la base se detiene a esperar. Oracle exige un mínimo de dos.",
                "ALTER DATABASE ADD LOGFILE GROUP <n> ('<ruta a>','<ruta b>') SIZE 200M;"));

        normalizarPesos();
    }

    /**
     * Convierte velocidad x alcance en un peso porcentual dentro de cada
     * indicador. Asi el peso de cada variable tiene una derivacion
     * reproducible y no un numero puesto a mano.
     */
    private void normalizarPesos() {
        Map<String, Integer> totalPorIndicador = new HashMap<>();
        for (VariableDef d : porCodigo.values()) {
            totalPorIndicador.merge(d.indicador, d.pesoBruto(), Integer::sum);
        }
        for (VariableDef d : porCodigo.values()) {
            int total = totalPorIndicador.getOrDefault(d.indicador, 1);
            pesoNormalizado.put(d.codigo,
                    total == 0 ? 0 : (int) Math.round(d.pesoBruto() * 100.0 / total));
        }
    }

    public VariableDef get(String codigo) { return porCodigo.get(codigo); }
    public Collection<VariableDef> todas() { return porCodigo.values(); }
    public int peso(String codigo) { return pesoNormalizado.getOrDefault(codigo, 0); }

    public List<VariableDef> deIndicador(String indicador) {
        return porCodigo.values().stream()
                .filter(d -> d.indicador.equals(indicador)).toList();
    }

    /** Ajuste de umbral en caliente, para la pantalla de configuracion. */
    public boolean ajustar(String codigo, Double advertencia, Double critico) {
        VariableDef vieja = porCodigo.get(codigo);
        if (vieja == null) return false;
        porCodigo.put(codigo, new VariableDef(vieja.codigo, vieja.indicador, vieja.grupo,
                vieja.nombre, vieja.unidad, vieja.consulta, vieja.direccion,
                advertencia, critico, VariableDef.Fuente.MEDICION,
                vieja.velocidad, vieja.alcance, vieja.ayuda, vieja.remediacion));
        return true;
    }
}