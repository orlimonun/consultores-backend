-- ============================================================
-- MIGRACIÓN: Módulo de Gestión de Estrategias de Respaldo RMAN
-- Proyecto: EIF402 Administración de Bases de Datos
-- Aplica sobre la BD PostgreSQL existente (ConsultoresDB)
-- ============================================================

-- Tabla 1: Registro de instancias Oracle gestionadas
CREATE TABLE IF NOT EXISTS base_datos_respaldo (
    id               SERIAL PRIMARY KEY,
    nombre           VARCHAR(150) NOT NULL,
    host             VARCHAR(255) NOT NULL,
    puerto           INTEGER      NOT NULL DEFAULT 1521,
    servicio         VARCHAR(100) NOT NULL,
    modo_archivado   VARCHAR(20)  NOT NULL DEFAULT 'DESCONOCIDO',
    descripcion      VARCHAR(500),
    responsable_id   INTEGER REFERENCES usuario(id) ON DELETE SET NULL,
    agente_id        VARCHAR(100),
    activa           BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Tabla 2: Estrategias de respaldo (QUÉ + CÓMO)
CREATE TABLE IF NOT EXISTS estrategia_respaldo (
    id                      SERIAL PRIMARY KEY,
    nombre                  VARCHAR(150) NOT NULL,
    descripcion             VARCHAR(500),
    base_datos_id           INTEGER NOT NULL REFERENCES base_datos_respaldo(id) ON DELETE CASCADE,
    responsable_id          INTEGER REFERENCES usuario(id) ON DELETE SET NULL,
    prioridad               VARCHAR(10)  NOT NULL DEFAULT 'MEDIA',
    estado                  VARCHAR(30)  NOT NULL DEFAULT 'BORRADOR',

    -- QUÉ respaldar
    incluir_database        BOOLEAN NOT NULL DEFAULT FALSE,
    tablespaces             VARCHAR(500),
    datafiles               VARCHAR(1000),
    incluir_control_file    BOOLEAN NOT NULL DEFAULT TRUE,
    incluir_spfile          BOOLEAN NOT NULL DEFAULT TRUE,
    incluir_archived_logs   BOOLEAN NOT NULL DEFAULT FALSE,

    -- CÓMO respaldar
    tipo_respaldo           VARCHAR(30) NOT NULL DEFAULT 'COMPLETO',
    compresion              BOOLEAN NOT NULL DEFAULT FALSE,
    algoritmo_compresion    VARCHAR(10) DEFAULT 'BASIC',
    delete_archived_logs    BOOLEAN NOT NULL DEFAULT FALSE,
    canales                 INTEGER NOT NULL DEFAULT 1,

    -- Destino
    destino_ruta            VARCHAR(500),
    destino_dispositivo     VARCHAR(50) DEFAULT 'DISK',
    formato_backupset       VARCHAR(200) DEFAULT '%d_%T_%U',

    -- Script generado
    script_rman             TEXT,
    script_generado_en      TIMESTAMP,

    notas                   VARCHAR(1000),
    creada_en               TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizada_en          TIMESTAMP
);

-- Tabla 3: Programación de estrategias (CUÁNDO)
CREATE TABLE IF NOT EXISTS programacion_respaldo (
    id                  SERIAL PRIMARY KEY,
    estrategia_id       INTEGER NOT NULL REFERENCES estrategia_respaldo(id) ON DELETE CASCADE,
    cron_expresion      VARCHAR(100) NOT NULL,
    descripcion_cron    VARCHAR(200),
    fecha_inicio        TIMESTAMP NOT NULL,
    fecha_fin           TIMESTAMP,
    ventana_minutos     INTEGER,
    mecanismo           VARCHAR(30) NOT NULL DEFAULT 'AGENTE_RMAN',
    activa              BOOLEAN NOT NULL DEFAULT TRUE,
    proxima_ejecucion   TIMESTAMP,
    ultima_ejecucion    TIMESTAMP,
    creada_en           TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tabla 4: Evidencia de ejecuciones
CREATE TABLE IF NOT EXISTS ejecucion_respaldo (
    id                  SERIAL PRIMARY KEY,
    estrategia_id       INTEGER NOT NULL REFERENCES estrategia_respaldo(id) ON DELETE CASCADE,
    programacion_id     INTEGER REFERENCES programacion_respaldo(id) ON DELETE SET NULL,
    resultado           VARCHAR(20)  NOT NULL DEFAULT 'EN_PROGRESO',
    script_ejecutado    TEXT,
    tipo_respaldo       VARCHAR(30),
    base_datos_nombre   VARCHAR(150),
    inicio              TIMESTAMP NOT NULL DEFAULT NOW(),
    fin                 TIMESTAMP,
    duracion_segundos   BIGINT,
    salida_rman         TEXT,
    mensaje_error       TEXT,
    ubicacion_respaldo  VARCHAR(500),
    agente_id           VARCHAR(100),
    origen              VARCHAR(15)  NOT NULL DEFAULT 'PROGRAMADO'
);

-- Índices para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_ejecucion_estrategia_inicio
    ON ejecucion_respaldo(estrategia_id, inicio DESC);

CREATE INDEX IF NOT EXISTS idx_ejecucion_resultado
    ON ejecucion_respaldo(resultado);

CREATE INDEX IF NOT EXISTS idx_programacion_estrategia
    ON programacion_respaldo(estrategia_id);

CREATE INDEX IF NOT EXISTS idx_estrategia_estado
    ON estrategia_respaldo(estado);
