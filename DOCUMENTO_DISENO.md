# Documento de Diseño
## Herramienta para la Gestión de Estrategias de Respaldo de Bases de Datos Oracle
**Universidad Nacional · Escuela de Informática**
**EIF402: Administración de Bases de Datos · II ciclo 2026**

---

## 1. Arquitectura del Sistema

El sistema sigue una arquitectura de **tres capas** con un componente adicional de agente:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    NAVEGADOR (Usuario / Admin)                       │
│              React 19 + Vite · http://localhost:5173                 │
└───────────────────────────┬─────────────────────────────────────────┘
                            │  HTTPS / JWT
┌───────────────────────────▼─────────────────────────────────────────┐
│                     BACKEND (Spring Boot 3.3)                        │
│                     Java 21 · puerto 8080                            │
│                                                                      │
│  ┌──────────────┐  ┌───────────────┐  ┌───────────────────────────┐ │
│  │  Auditoría   │  │  Módulo RMAN  │  │  Monitor Oracle           │ │
│  │  ISO 27002   │  │  (nuevo)      │  │  (Autonomous + Agentes)   │ │
│  └──────────────┘  └───────────────┘  └───────────────────────────┘ │
└────────┬──────────────────┬────────────────────────┬────────────────┘
         │  JPA/Hibernate   │  JDBC Oracle            │  HTTP Webhook
┌────────▼──────┐    ┌──────▼──────────────┐  ┌──────▼─────────────┐
│  PostgreSQL   │    │  Oracle Autonomous   │  │  AGENTE RMAN       │
│  (principal)  │    │  (monitor Autonomous)│  │  (servidor Oracle) │
│  · auditoría  │    └─────────────────────┘  │  · LectorOracle    │
│  · estrategias│                              │  · EjecutorRman    │
│  · ejecuciones│                              │  · LectorDisco     │
└───────────────┘                              └─────────┬──────────┘
                                                         │  JDBC local
                                               ┌─────────▼──────────┐
                                               │  Oracle DB (local)  │
                                               │  XE / 19c / 21c     │
                                               └────────────────────┘
```

### Decisiones de arquitectura

| Decisión | Justificación |
|---|---|
| Backend único (no microservicios) | Proyecto académico; la complejidad de microservicios no aporta valor aquí |
| PostgreSQL para metadatos, Oracle solo para monitoreo/ejecución | Separa el almacenamiento de la herramienta del motor que gestiona |
| Agente RMAN independiente | RMAN debe ejecutarse en el mismo servidor que Oracle; el agente resuelve el problema de red |
| JWT stateless | Compatible con despliegue en Render sin sesiones persistentes |
| ARCHIVELOG/NOARCHIVELOG detectado manualmente | El cambio de modo es una decisión de arquitectura del DBA, no de la herramienta |

---

## 2. Módulos del Sistema

### 2.1 Backend — Módulos

```
cr.una.consultores/
├── config/
│   ├── SecurityConfig.java         — JWT, CORS, filtros de autenticación
│   ├── OracleWalletConfig.java     — Conexión secundaria Oracle Autonomous
│   ├── JacksonConfig.java          — Serialización JSON
│   └── DataSeeder.java             — Usuario admin por defecto
│
├── entity/                         — Entidades JPA (tablas en PostgreSQL)
│   ├── [Auditoría ISO 27002]
│   │   ├── Auditoria, Control, Dominio, Organizacion
│   │   ├── Pregunta, Respuesta, ResultadoControl, Usuario
│   └── [Módulo RMAN - nuevo]
│       ├── BaseDatosRespaldo       — Instancias Oracle registradas
│       ├── EstrategiaRespaldo      — QUÉ + CÓMO respaldar
│       ├── ProgramacionRespaldo    — CUÁNDO (cron)
│       └── EjecucionRespaldo       — Evidencia de cada ejecución
│
├── service/
│   ├── [Auditoría]
│   │   ├── AuthService, CalculoRiesgoService, EvaluadorSaludService
│   │   └── OracleMonitorService, AgenteMetricasService
│   └── [RMAN - nuevo]
│       ├── RmanScriptBuilder       — Transforma estrategia → script RMAN
│       └── AlertaRespaldoService   — Evalúa 8 condiciones de control preventivo
│
├── controller/
│   ├── [Auditoría existente]
│   │   ├── AuthController, AuditoriaController, ControlController
│   │   ├── DominioController, OrganizacionController
│   │   ├── OracleMonitorController, AgenteMonitorController
│   │   └── RespuestaController, ResultadoController, UsuarioController
│   └── [RMAN - nuevo]
│       └── RespaldoController      — 20 endpoints bajo /api/respaldo/*
│
└── repository/                     — JPA Repositories
    ├── [Existentes: Auditoria, Control, ...]
    └── [Nuevos: BaseDatosRespaldo, EstrategiaRespaldo,
               ProgramacionRespaldo, EjecucionRespaldo]
```

### 2.2 Frontend — Módulos

```
src/
├── api/
│   ├── client.js           — HTTP client con JWT automático
│   ├── [Existentes: auditorias, auth, controles, organizaciones...]
│   └── respaldo.js         — Cliente API para todos los endpoints RMAN
│
├── context/
│   ├── AuthContext.jsx     — Autenticación y rol del usuario
│   ├── ThemeContext.jsx    — Modo claro/oscuro
│   └── LanguageContext.jsx — Idioma
│
├── components/
│   ├── Layout.jsx          — Estructura principal con Sidebar
│   ├── Sidebar.jsx         — Navegación (incluye sección Respaldo RMAN)
│   └── ProtectedRoute.jsx  — Guard de rutas autenticadas
│
└── pages/
    ├── [Auditoría existente: Dashboard, Auditorias, Controles...]
    └── respaldo/           — [nuevo]
        ├── BasesDatosRespaldo.jsx  — CRUD de instancias Oracle
        ├── EstrategiasList.jsx     — Lista con acciones rápidas
        ├── EstrategiaForm.jsx      — Wizard 5 pasos QUÉ·CÓMO·CUÁNDO·Script
        ├── HistorialRespaldo.jsx   — Evidencia de ejecuciones
        └── AlertasRespaldo.jsx     — Panel de control preventivo
```

### 2.3 Agente RMAN — Módulos

```
cr.una.consultores.agente/
├── AgenteApplication.java      — Spring Boot: scheduling + web en 127.0.0.1
├── ConexionesConfig.java       — DataSources CDB y PDB
├── LectorOracle.java           — Lee métricas de la instancia local
├── LectorEspacioDisco.java     — [nuevo] Lee espacio libre del SO
├── EjecutorRman.java           — [nuevo] Ejecuta scripts RMAN via ProcessBuilder
├── RmanController.java         — [nuevo] POST /api/rman/ejecutar
├── Enviador.java               — Ciclo de monitoreo → POST al backend
├── ComandoRman.java            — [nuevo] DTO entrada del comando
├── ResultadoRman.java          — [nuevo] DTO resultado con datos de disco
└── ReporteSalud.java           — DTO del reporte de monitoreo
```

---

## 3. Modelo de Datos

### 3.1 Entidades del Módulo RMAN

```
┌─────────────────────────────────┐
│       base_datos_respaldo       │
├─────────────────────────────────┤
│ id             SERIAL PK        │
│ nombre         VARCHAR(150)     │
│ host           VARCHAR(255)     │
│ puerto         INTEGER          │
│ servicio       VARCHAR(100)     │
│ modo_archivado VARCHAR(20)      │  ← ARCHIVELOG | NOARCHIVELOG | DESCONOCIDO
│ descripcion    VARCHAR(500)     │
│ responsable_id → usuario(id)    │
│ agente_id      VARCHAR(100)     │  ← ID del agente RMAN que la gestiona
│ activa         BOOLEAN          │
└──────────────┬──────────────────┘
               │ 1:N
               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      estrategia_respaldo                            │
├─────────────────────────────────────────────────────────────────────┤
│ id                     SERIAL PK                                    │
│ nombre                 VARCHAR(150)                                 │
│ descripcion            VARCHAR(500)                                 │
│ base_datos_id          → base_datos_respaldo(id)                   │
│ responsable_id         → usuario(id)                               │
│ prioridad              VARCHAR(10)    ← ALTA | MEDIA | BAJA        │
│ estado                 VARCHAR(30)    ← BORRADOR | ACTIVA |        │
│                                          INACTIVA | PENDIENTE...   │
│ -- QUÉ respaldar --                                                 │
│ incluir_database       BOOLEAN                                      │
│ tablespaces            VARCHAR(500)   ← separados por coma         │
│ datafiles              VARCHAR(1000)  ← rutas separadas por coma   │
│ incluir_control_file   BOOLEAN                                      │
│ incluir_spfile         BOOLEAN                                      │
│ incluir_archived_logs  BOOLEAN                                      │
│ -- CÓMO respaldar --                                                │
│ tipo_respaldo          VARCHAR(30)    ← COMPLETO | INCREMENTAL_... │
│ compresion             BOOLEAN                                      │
│ algoritmo_compresion   VARCHAR(10)                                  │
│ delete_archived_logs   BOOLEAN                                      │
│ canales                INTEGER                                      │
│ -- Destino --                                                       │
│ destino_ruta           VARCHAR(500)                                 │
│ destino_dispositivo    VARCHAR(50)                                  │
│ formato_backupset      VARCHAR(200)                                 │
│ -- Script generado --                                               │
│ script_rman            TEXT                                         │
│ script_generado_en     TIMESTAMP                                    │
│ notas                  VARCHAR(1000)                                │
│ creada_en              TIMESTAMP                                    │
│ actualizada_en         TIMESTAMP                                    │
└──────────┬──────────────────────────────────────┬──────────────────┘
           │ 1:N                                   │ 1:N
           ▼                                       ▼
┌──────────────────────────┐        ┌──────────────────────────────────┐
│  programacion_respaldo   │        │       ejecucion_respaldo         │
├──────────────────────────┤        ├──────────────────────────────────┤
│ id             SERIAL PK │        │ id               SERIAL PK       │
│ estrategia_id  → estrat. │        │ estrategia_id    → estrategia    │
│ cron_expresion VARCHAR   │        │ programacion_id  → programacion  │
│ descripcion_cron VARCHAR │        │ resultado        VARCHAR(20)     │
│ fecha_inicio   TIMESTAMP │        │   ← EXITOSO | CON_ADVERTENCIAS  │
│ fecha_fin      TIMESTAMP │        │       | FALLIDO | EN_PROGRESO    │
│ ventana_minutos INTEGER  │        │ script_ejecutado TEXT            │
│ mecanismo      VARCHAR   │        │ tipo_respaldo    VARCHAR(30)     │
│ activa         BOOLEAN   │        │ base_datos_nombre VARCHAR(150)   │
│ proxima_ejec.  TIMESTAMP │        │ inicio           TIMESTAMP       │
│ ultima_ejec.   TIMESTAMP │        │ fin              TIMESTAMP       │
│ creada_en      TIMESTAMP │        │ duracion_segundos BIGINT         │
└──────────────────────────┘        │ salida_rman      TEXT            │
                                    │ mensaje_error    TEXT            │
                                    │ ubicacion_respaldo VARCHAR       │
                                    │ agente_id        VARCHAR(100)    │
                                    │ origen           VARCHAR(15)     │
                                    │   ← PROGRAMADO | MANUAL         │
                                    │ disco_uso_pct    DECIMAL(5,2)    │
                                    │ disco_libre_mb   BIGINT          │
                                    │ disco_mount_point VARCHAR(200)   │
                                    └──────────────────────────────────┘
```

### 3.2 Relación con entidades existentes

```
usuario (existente)
  ├──< base_datos_respaldo.responsable_id
  └──< estrategia_respaldo.responsable_id
```

---

## 4. Endpoints REST del Módulo RMAN

### Base de Datos

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/respaldo/bases` | Listar instancias Oracle |
| POST | `/api/respaldo/bases` | Registrar instancia |
| PUT | `/api/respaldo/bases/{id}` | Actualizar instancia |
| DELETE | `/api/respaldo/bases/{id}` | Eliminar instancia |

### Estrategias

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/respaldo/estrategias` | Listar (filtro por baseDatosId) |
| GET | `/api/respaldo/estrategias/{id}` | Detalle con advertencias calculadas |
| POST | `/api/respaldo/estrategias` | Crear |
| PUT | `/api/respaldo/estrategias/{id}` | Actualizar (invalida el script) |
| DELETE | `/api/respaldo/estrategias/{id}` | Eliminar |
| POST | `/api/respaldo/estrategias/{id}/generar-script` | Genera el script RMAN |
| POST | `/api/respaldo/estrategias/{id}/ejecutar` | Lanza ejecución manual |

### Programaciones, Ejecuciones, Alertas y Webhook

| Método | Ruta | Descripción |
|---|---|---|
| GET/POST | `/api/respaldo/programaciones` | CRUD de programaciones |
| PUT/DELETE | `/api/respaldo/programaciones/{id}` | Actualizar / eliminar |
| GET | `/api/respaldo/ejecuciones` | Historial global (últimas 50) |
| GET | `/api/respaldo/ejecuciones/{id}` | Detalle con script y salida RMAN |
| GET | `/api/respaldo/ejecuciones/estrategia/{id}` | Historial por estrategia |
| GET | `/api/respaldo/alertas` | Panel de control preventivo |
| POST | `/api/respaldo/webhook/resultado` | Recibe resultado del agente RMAN |

---

## 5. Flujo de Construcción de Estrategias

```
Administrador abre el wizard
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  PASO 1: Información general                                │
│  Nombre · Descripción · Base de datos · Prioridad · Estado  │
│  ↳ Se muestra aviso ARCHIVELOG/NOARCHIVELOG según la BD     │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  PASO 2: QUÉ respaldar                                      │
│  DATABASE / Tablespaces / Datafiles / Control File /        │
│  SPFILE / Archived Redo Logs / DELETE INPUT                 │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  PASO 3: CÓMO respaldar                                     │
│  Tipo de respaldo (5 opciones con descripción)              │
│  Compresión · Algoritmo · Canales paralelos                 │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  PASO 4: Destino y CUÁNDO                                   │
│  Ruta · Dispositivo · Formato backupset                     │
│  Expresión cron · Inicio · Ventana de respaldo              │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
             [Guardar estrategia]
                        │
                        ▼
           POST /api/respaldo/estrategias
                        │
                        ▼
             [Generar script RMAN]
                        │
                        ▼
    POST /api/respaldo/estrategias/{id}/generar-script
                        │
                        ▼
        RmanScriptBuilder.construir(estrategia)
              ↳ Valida la estrategia
              ↳ Genera advertencias
              ↳ Construye el script RMAN
              ↳ Estado → PENDIENTE_APROBACION
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  PASO 5: Visor del script                                   │
│  El administrador revisa el RMAN generado                   │
│  Puede regenerar si necesita cambios                        │
│  Puede copiar al portapapeles                               │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
        [Administrador aprueba y activa]
                        │
                        ▼
        PUT /api/respaldo/estrategias/{id}
              estado = "ACTIVA"
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  Ejecución (automática o manual)                            │
│                                                             │
│  Automática: Agente RMAN detecta la programación           │
│  Manual: POST /api/respaldo/estrategias/{id}/ejecutar       │
└───────────────────────┬─────────────────────────────────────┘
                        ▼
         Se crea EjecucionRespaldo (EN_PROGRESO)
                        │
                        ▼
    Agente RMAN recibe el script por POST /api/rman/ejecutar
                        │
                        ▼
    EjecutorRman escribe el script en archivo temporal
                        │
                        ▼
    ProcessBuilder: rman target / cmdfile <archivo>
                        │
                        ▼
    Captura stdout + stderr de RMAN
                        │
                        ▼
    Determina resultado: EXITOSO | CON_ADVERTENCIAS | FALLIDO
    (código de salida + patrones RMAN- / ORA- en la salida)
                        │
                        ▼
    POST /api/respaldo/webhook/resultado
    (con resultado, salida RMAN, duración, datos de disco)
                        │
                        ▼
    EjecucionRespaldo se actualiza como evidencia inmutable
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  Historial RMAN: evidencia consultable                      │
│  Panel de alertas: control preventivo activo                │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. Diseño de la Interfaz

### 6.1 Sección Respaldo RMAN en el Sidebar

```
> _ ISO27002
──────────────────
◈  Dashboard
⌘  Organizaciones
☰  Controles ISO
✓  Auditorías
▤  Resultados
↗  Histórico
●  Monitoreo
◎  Monitor Oracle
──── RESPALDO RMAN ────
◷  Bases RMAN
⊞  Estrategias
⊟  Historial RMAN
⚠  Alertas RMAN
──────────────────
```

### 6.2 Wizard de 5 pasos (EstrategiaForm)

```
1. Información general | 2. QUÉ respaldar | 3. CÓMO respaldar | 4. CUÁNDO y destino | 5. Script RMAN
─────────────────────────────────────────────────────────────────────────────────────
```

### 6.3 Panel de Alertas — Códigos de color

| Severidad | Color | Descripción |
|---|---|---|
| CRITICA | Rojo | Requiere acción inmediata |
| ADVERTENCIA | Amarillo/ámbar | Requiere atención pronto |
| INFO | Azul | Recomendación informativa |

---

## 7. Generación del Script RMAN — Lógica de Transformación

La clase [`RmanScriptBuilder`](consultores-backend-master/backend/src/main/java/cr/una/consultores/service/RmanScriptBuilder.java) transforma la estrategia configurada en instrucciones RMAN siguiendo esta lógica:

### 7.1 Estructura general del script

```sql
-- Encabezado con metadatos (nombre, tipo, fecha de generación, advertencias)

CONFIGURE DEFAULT DEVICE TYPE TO DISK;           -- Si hay ruta de destino
CONFIGURE CHANNEL DEVICE TYPE DISK FORMAT '...'; -- Con el formato configurado

RUN {
  ALLOCATE CHANNEL ch1 DEVICE TYPE DISK;          -- Si canales > 1
  ALLOCATE CHANNEL ch2 DEVICE TYPE DISK;

  BACKUP [AS COMPRESSED BACKUPSET]                -- Si compresión = true
         [INCREMENTAL LEVEL 0|1|1 CUMULATIVE]     -- Según tipo de respaldo
         DATABASE | TABLESPACE ts1,ts2 | DATAFILE '/ruta'
         [INCLUDE CURRENT CONTROLFILE]             -- Si incluirControlFile
         FORMAT '...';

  BACKUP SPFILE FORMAT '...';                     -- Si incluirSpfile

  BACKUP ARCHIVELOG ALL [DELETE INPUT]            -- Si incluirArchivedLogs
         FORMAT '...';

  RELEASE CHANNEL ch1;                           -- Si canales > 1
  RELEASE CHANNEL ch2;
}

CROSSCHECK BACKUP;                               -- Verificación siempre
LIST BACKUP SUMMARY;
```

### 7.2 Mapeo tipo de respaldo → cláusula RMAN

| Tipo configurado | Cláusula RMAN |
|---|---|
| COMPLETO | `BACKUP DATABASE` (sin INCREMENTAL) |
| INCREMENTAL_NIVEL_0 | `BACKUP INCREMENTAL LEVEL 0 DATABASE` |
| INCREMENTAL_NIVEL_1 | `BACKUP INCREMENTAL LEVEL 1 DATABASE` |
| INCREMENTAL_DIFERENCIAL | `BACKUP INCREMENTAL LEVEL 1 DATABASE` |
| INCREMENTAL_ACUMULATIVO | `BACKUP INCREMENTAL LEVEL 1 CUMULATIVE DATABASE` |

### 7.3 Validaciones antes de generar

| Condición | Advertencia |
|---|---|
| NOARCHIVELOG + incluirArchivedLogs | Error: no hay logs para respaldar |
| NOARCHIVELOG | Advertencia: recuperación limitada |
| ARCHIVELOG + !incluirArchivedLogs | Recomendación: considerar incluirlos |
| Sin objetos seleccionados | Fallback a DATABASE |
| Sin ruta de destino | Usará Fast Recovery Area si está configurada |
| Tipo incremental nivel 1+ | Advertencia: requiere nivel 0 previo |
