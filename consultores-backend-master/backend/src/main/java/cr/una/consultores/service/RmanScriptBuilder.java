package cr.una.consultores.service;

import cr.una.consultores.entity.EstrategiaRespaldo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Transforma una EstrategiaRespaldo en el script RMAN correspondiente.
 *
 * Reglas de construcción:
 *   1. Se abre con RUN { para agrupar todos los comandos en un bloque.
 *   2. Si hay más de un canal, se asignan antes del BACKUP.
 *   3. El bloque BACKUP se construye según el tipo de respaldo y los
 *      elementos seleccionados (QUÉ).
 *   4. El respaldo de archived logs se agrega como bloque separado dentro
 *      del mismo RUN { si corresponde.
 *   5. Si deleteArchivedLogs = true se agrega DELETE INPUT.
 *   6. CONTROL FILE y SPFILE se incluyen cuando están marcados.
 *   7. El bloque cierra con } y agrega CROSSCHECK/VALIDATE opcionales.
 *
 * Documentación de referencia: Oracle RMAN Reference 19c/21c.
 */
@Service
public class RmanScriptBuilder {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Genera el script RMAN completo a partir de la estrategia.
     * El script resultante puede copiarse directamente a un archivo .rman
     * y ejecutarse con: rman target / cmdfile archivo.rman
     */
    public String construir(EstrategiaRespaldo e) {
        List<String> advertencias = advertencias(e);
        StringBuilder sb = new StringBuilder();

        // ── Encabezado informativo ──────────────────────────────────────────
        sb.append("-- ============================================================\n");
        sb.append("-- SCRIPT RMAN GENERADO AUTOMÁTICAMENTE\n");
        sb.append("-- Estrategia : ").append(e.getNombre()).append("\n");
        sb.append("-- Base datos : ").append(e.getBaseDatos().getNombre()).append("\n");
        sb.append("-- Tipo       : ").append(etiquetaTipo(e.getTipoRespaldo())).append("\n");
        sb.append("-- Prioridad  : ").append(e.getPrioridad()).append("\n");
        sb.append("-- Generado   : ").append(LocalDateTime.now().format(FMT)).append("\n");
        if (!advertencias.isEmpty()) {
            sb.append("-- ADVERTENCIAS:\n");
            advertencias.forEach(a -> sb.append("--   * ").append(a).append("\n"));
        }
        sb.append("-- ============================================================\n\n");

        // ── Configuración de destino ────────────────────────────────────────
        if (e.getDestinoRuta() != null && !e.getDestinoRuta().isBlank()) {
            sb.append("CONFIGURE DEFAULT DEVICE TYPE TO ").append(e.getDestinoDispositivo()).append(";\n");
            sb.append("CONFIGURE CHANNEL DEVICE TYPE ").append(e.getDestinoDispositivo())
              .append(" FORMAT '").append(destinoFormato(e)).append("';\n\n");
        }

        // ── Bloque RUN ──────────────────────────────────────────────────────
        sb.append("RUN {\n");

        // Asignación de canales si hay más de uno
        if (e.getCanales() != null && e.getCanales() > 1) {
            for (int i = 1; i <= e.getCanales(); i++) {
                sb.append("  ALLOCATE CHANNEL ch").append(i)
                  .append(" DEVICE TYPE ").append(e.getDestinoDispositivo()).append(";\n");
            }
            sb.append("\n");
        }

        // ── Comando BACKUP principal ────────────────────────────────────────
        sb.append("  BACKUP");

        // Compresión
        if (Boolean.TRUE.equals(e.getCompresion())) {
            String algo = e.getAlgoritmoCompresion() != null ? e.getAlgoritmoCompresion() : "BASIC";
            sb.append(" AS COMPRESSED BACKUPSET");
        }

        // Tipo de respaldo (nivel incremental)
        switch (e.getTipoRespaldo()) {
            case "INCREMENTAL_NIVEL_0" ->
                sb.append(" INCREMENTAL LEVEL 0");
            case "INCREMENTAL_NIVEL_1", "INCREMENTAL_DIFERENCIAL" ->
                sb.append(" INCREMENTAL LEVEL 1");
            case "INCREMENTAL_ACUMULATIVO" ->
                sb.append(" INCREMENTAL LEVEL 1 CUMULATIVE");
            // COMPLETO: sin cláusula incremental
        }

        // QUÉ respaldar
        sb.append("\n");
        if (Boolean.TRUE.equals(e.getIncluirDatabase())) {
            sb.append("    DATABASE");
        } else {
            // Elementos individuales
            List<String> elementos = new ArrayList<>();
            if (e.getTablespaces() != null && !e.getTablespaces().isBlank()) {
                for (String ts : e.getTablespaces().split(",")) {
                    elementos.add("TABLESPACE " + ts.trim());
                }
            }
            if (e.getDatafiles() != null && !e.getDatafiles().isBlank()) {
                for (String df : e.getDatafiles().split(",")) {
                    elementos.add("DATAFILE '" + df.trim() + "'");
                }
            }
            if (elementos.isEmpty()) {
                // Fallback: respaldar DATABASE si no hay nada seleccionado
                elementos.add("DATABASE");
            }
            sb.append("    ").append(String.join("\n    ", elementos));
        }

        // Incluir control file dentro del BACKUP si está marcado
        if (Boolean.TRUE.equals(e.getIncluirControlFile()) &&
            !Boolean.TRUE.equals(e.getIncluirDatabase())) {
            sb.append("\n    INCLUDE CURRENT CONTROLFILE");
        }

        // Formato del backupset
        if (e.getFormatoBackupset() != null && !e.getFormatoBackupset().isBlank()) {
            sb.append("\n    FORMAT '").append(destinoFormato(e)).append("'");
        }

        sb.append(";\n");

        // ── SPFILE (bloque separado) ────────────────────────────────────────
        if (Boolean.TRUE.equals(e.getIncluirSpfile())) {
            sb.append("\n  BACKUP SPFILE");
            if (e.getDestinoRuta() != null && !e.getDestinoRuta().isBlank()) {
                sb.append(" FORMAT '").append(destinoFormato(e)).append("'");
            }
            sb.append(";\n");
        }

        // ── Archived redo logs ──────────────────────────────────────────────
        if (Boolean.TRUE.equals(e.getIncluirArchivedLogs())) {
            sb.append("\n  BACKUP ARCHIVELOG ALL");
            if (Boolean.TRUE.equals(e.getDeleteArchivedLogs())) {
                sb.append(" DELETE INPUT");
            }
            if (e.getDestinoRuta() != null && !e.getDestinoRuta().isBlank()) {
                sb.append("\n    FORMAT '").append(destinoFormato(e)).append("'");
            }
            sb.append(";\n");
        }

        // Liberar canales si se asignaron manualmente
        if (e.getCanales() != null && e.getCanales() > 1) {
            sb.append("\n");
            for (int i = 1; i <= e.getCanales(); i++) {
                sb.append("  RELEASE CHANNEL ch").append(i).append(";\n");
            }
        }

        sb.append("}\n\n");

        // ── Verificación del respaldo ───────────────────────────────────────
        sb.append("-- Verificar integridad del respaldo\n");
        sb.append("CROSSCHECK BACKUP;\n");
        sb.append("LIST BACKUP SUMMARY;\n");

        return sb.toString();
    }

    /**
     * Valida la estrategia antes de generar el script.
     * Devuelve lista de advertencias (no bloquean la generación pero
     * deben mostrarse al administrador).
     */
    public List<String> advertencias(EstrategiaRespaldo e) {
        List<String> avisos = new ArrayList<>();

        String modo = e.getBaseDatos().getModoArchivado();

        // ARCHIVELOG / NOARCHIVELOG
        if ("NOARCHIVELOG".equalsIgnoreCase(modo)) {
            avisos.add("La base de datos está en modo NOARCHIVELOG. " +
                       "Las posibilidades de recuperación son más limitadas. " +
                       "Revise la estrategia antes de continuar.");
            if (Boolean.TRUE.equals(e.getIncluirArchivedLogs())) {
                avisos.add("Se seleccionaron archived redo logs pero la base está en NOARCHIVELOG. " +
                           "No hay logs para respaldar.");
            }
        } else if ("ARCHIVELOG".equalsIgnoreCase(modo)) {
            if (!Boolean.TRUE.equals(e.getIncluirArchivedLogs())) {
                avisos.add("Recomendación: La base está en modo ARCHIVELOG. " +
                           "Considere incluir el respaldo periódico de archived redo logs " +
                           "para mejorar las posibilidades de recuperación.");
            }
        }

        // Nada seleccionado para respaldar
        boolean sinObjeto = !Boolean.TRUE.equals(e.getIncluirDatabase())
            && (e.getTablespaces() == null || e.getTablespaces().isBlank())
            && (e.getDatafiles() == null || e.getDatafiles().isBlank());
        if (sinObjeto) {
            avisos.add("No se seleccionó ningún objeto para respaldar (database, tablespaces ni datafiles). " +
                       "Se usará DATABASE como fallback.");
        }

        // Sin destino
        if (e.getDestinoRuta() == null || e.getDestinoRuta().isBlank()) {
            avisos.add("No se definió una ruta de destino. " +
                       "El respaldo usará el Fast Recovery Area (FRA) si está configurada en Oracle.");
        }

        // Incremental sin nivel 0 previo (sólo advertencia informativa)
        if (e.getTipoRespaldo() != null &&
            (e.getTipoRespaldo().equals("INCREMENTAL_NIVEL_1") ||
             e.getTipoRespaldo().equals("INCREMENTAL_DIFERENCIAL") ||
             e.getTipoRespaldo().equals("INCREMENTAL_ACUMULATIVO"))) {
            avisos.add("Un respaldo incremental nivel 1 requiere que exista un respaldo " +
                       "incremental nivel 0 previo como punto de partida.");
        }

        return avisos;
    }

    // ── utilidades privadas ─────────────────────────────────────────────────

    private String destinoFormato(EstrategiaRespaldo e) {
        String base = (e.getDestinoRuta() != null && !e.getDestinoRuta().isBlank())
                ? e.getDestinoRuta().replaceAll("/$", "") + "/"
                : "";
        String fmt = (e.getFormatoBackupset() != null && !e.getFormatoBackupset().isBlank())
                ? e.getFormatoBackupset()
                : "%d_%T_%U";
        return base + fmt;
    }

    private String etiquetaTipo(String tipo) {
        if (tipo == null) return "COMPLETO";
        return switch (tipo) {
            case "COMPLETO"                -> "Respaldo Completo";
            case "INCREMENTAL_NIVEL_0"     -> "Incremental Nivel 0";
            case "INCREMENTAL_NIVEL_1"     -> "Incremental Nivel 1 (Diferencial)";
            case "INCREMENTAL_DIFERENCIAL" -> "Incremental Diferencial";
            case "INCREMENTAL_ACUMULATIVO" -> "Incremental Acumulativo";
            default -> tipo;
        };
    }
}
