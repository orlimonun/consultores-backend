package cr.una.consultores.agente;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee el espacio disponible en los sistemas de archivos del servidor.
 *
 * Se usa para alimentar la alerta SIN_ESPACIO del panel de control preventivo.
 * Se reporta el punto de montaje con menor espacio disponible, que es el
 * que más riesgo representa para el FRA o el destino de respaldo.
 *
 * Dos estrategias de lectura, en orden de preferencia:
 *   1. Java NIO FileStore — funciona en Linux, macOS y Windows sin deps extra.
 *   2. File.listRoots()  — fallback más portable.
 */
@Service
public class LectorEspacioDisco {

    private static final Logger log = LoggerFactory.getLogger(LectorEspacioDisco.class);

    public static class InfoDisco {
        public String mountPoint;
        public long totalMb;
        public long usadoMb;
        public long libreMb;
        public double usoPct;

        public InfoDisco(String mountPoint, long totalMb, long usadoMb, long libreMb) {
            this.mountPoint = mountPoint;
            this.totalMb    = totalMb;
            this.usadoMb    = usadoMb;
            this.libreMb    = libreMb;
            this.usoPct     = totalMb == 0 ? 0 :
                Math.round((double) usadoMb / totalMb * 1000) / 10.0;
        }
    }

    /** Devuelve info de todos los sistemas de archivo accesibles. */
    public List<InfoDisco> leer() {
        List<InfoDisco> resultado = new ArrayList<>();
        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                try {
                    long total = store.getTotalSpace();
                    if (total == 0) continue;   // pseudo-fs (proc, sysfs, etc.)
                    long libre = store.getUsableSpace();
                    long usado = total - libre;
                    resultado.add(new InfoDisco(
                        store.name() + " (" + store.type() + ")",
                        total / 1024 / 1024,
                        usado / 1024 / 1024,
                        libre / 1024 / 1024
                    ));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("No se pudo leer FileStores via NIO, usando File.listRoots(): {}", e.getMessage());
            for (File root : File.listRoots()) {
                long total = root.getTotalSpace();
                if (total == 0) continue;
                long libre = root.getUsableSpace();
                long usado = total - libre;
                resultado.add(new InfoDisco(
                    root.getAbsolutePath(),
                    total / 1024 / 1024,
                    usado / 1024 / 1024,
                    libre / 1024 / 1024
                ));
            }
        }
        return resultado;
    }

    /**
     * Devuelve el punto de montaje con mayor porcentaje de uso.
     * Es la métrica más relevante para la alerta de espacio.
     */
    public InfoDisco peorParticion() {
        return leer().stream()
            .max((a, b) -> Double.compare(a.usoPct, b.usoPct))
            .orElse(null);
    }
}
