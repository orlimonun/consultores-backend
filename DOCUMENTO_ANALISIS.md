# Documento de Análisis
## Herramienta para la Gestión de Estrategias de Respaldo de Bases de Datos Oracle
**Universidad Nacional · Escuela de Informática**
**EIF402: Administración de Bases de Datos · II ciclo 2026**
**Profesor: MSc. Johnny Villalobos Murillo**

---

## 1. Descripción del Problema

Las bases de datos Oracle almacenan información crítica para la operación de cualquier organización. La pérdida o indisponibilidad de esa información puede tener consecuencias graves: interrupciones operativas, pérdidas económicas, incumplimientos legales y daño reputacional.

El problema central es que **la mayoría de los incidentes de pérdida de datos no ocurren por falta de conocimiento, sino por ausencia de un proceso estructurado, automatizado y verificable de respaldo**. Los administradores de bases de datos frecuentemente:

- Ejecutan respaldos manualmente y de forma inconsistente.
- No documentan las estrategias de respaldo utilizadas.
- Carecen de evidencia verificable de que los respaldos se ejecutaron correctamente.
- No reciben alertas cuando un respaldo falla o deja de ejecutarse.
- No consideran el modo de archivado de la base de datos al diseñar la estrategia.

Oracle Recovery Manager (RMAN) es la herramienta estándar para la gestión de respaldos en Oracle, pero su uso manual requiere conocimiento avanzado y disciplina operativa que no siempre está garantizada.

---

## 2. Riesgos Identificados

La situación descrita expone a la organización a dos dimensiones de riesgo:

### 2.1 Riesgo de Disponibilidad
**Definición:** Posibilidad de que la información o la base de datos no se encuentre disponible cuando sea necesaria para la operación.

**Causas asociadas:**
- Ausencia de respaldos recientes que permitan una recuperación rápida.
- Respaldos programados que no se ejecutaron sin que nadie lo detectara.
- Destino de respaldo sin espacio suficiente que cause fallo silencioso.
- Base de datos en modo NOARCHIVELOG, que limita las opciones de recuperación.

### 2.2 Riesgo de Integridad
**Definición:** Posibilidad de pérdida, corrupción o alteración de información que impida recuperar un estado correcto de la base de datos.

**Causas asociadas:**
- Uso exclusivo de respaldos completos sin estrategia incremental, lo que aumenta la brecha temporal entre respaldos.
- No incluir archived redo logs en la estrategia, lo que imposibilita la recuperación hasta un punto exacto en el tiempo.
- Scripts RMAN incorrectos o incompletos que aparentan éxito pero no respaldan todos los objetos necesarios.
- No verificar la integridad de los respaldos después de generarlos.

---

## 3. Justificación

La herramienta desarrollada responde a la necesidad de **pasar de un proceso reactivo y manual a un proceso preventivo, automatizado y verificable**. No se trata simplemente de un ejecutor de scripts RMAN: es un sistema que:

1. Obliga al administrador a pensar en la estrategia antes de ejecutar.
2. Genera el script RMAN correcto a partir de decisiones de alto nivel.
3. Automatiza la ejecución y libera al administrador de la dependencia de procesos manuales.
4. Conserva evidencia inmutable de cada operación realizada.
5. Detecta proactivamente condiciones que comprometen la estrategia.

Esto representa un **control preventivo**: actúa antes de que ocurra el incidente, no después.

---

## 4. Objetivos

### Objetivo General
Desarrollar una herramienta de gestión de estrategias de respaldo para bases de datos Oracle que utilice RMAN como mecanismo de ejecución, permitiendo definir, automatizar y monitorear respaldos de manera estructurada y verificable.

### Objetivos Específicos
1. Implementar un modelo de gestión de estrategias basado en los componentes QUÉ, CÓMO y CUÁNDO respaldar.
2. Desarrollar un generador automático de scripts RMAN a partir de la configuración de estrategias.
3. Automatizar la ejecución de respaldos mediante programación cron gestionada por un agente RMAN.
4. Registrar evidencia completa de cada ejecución, incluyendo script ejecutado, resultado y salida de RMAN.
5. Implementar un panel de control preventivo con alertas sobre condiciones que comprometan la estrategia.
6. Distinguir y gestionar explícitamente los modos ARCHIVELOG y NOARCHIVELOG de Oracle.

---

## 5. Requerimientos

### Requerimientos Funcionales

| ID | Requerimiento | Prioridad |
|---|---|---|
| RF-01 | Registrar instancias Oracle con su modo de archivado | Alta |
| RF-02 | Crear estrategias de respaldo con nombre, descripción, prioridad y estado | Alta |
| RF-03 | Seleccionar elementos a respaldar: DATABASE, tablespaces, datafiles, Control File, SPFILE, archived logs | Alta |
| RF-04 | Seleccionar el tipo de respaldo: completo, incremental nivel 0/1, diferencial, acumulativo | Alta |
| RF-05 | Configurar opciones de respaldo: compresión, canales paralelos, dispositivo de destino | Media |
| RF-06 | Definir la programación mediante expresión cron, fecha de inicio y ventana de respaldo | Alta |
| RF-07 | Generar automáticamente el script RMAN a partir de la configuración de la estrategia | Alta |
| RF-08 | Visualizar el script generado antes de su aprobación y ejecución | Alta |
| RF-09 | Ejecutar el respaldo manualmente o de forma programada mediante el agente RMAN | Alta |
| RF-10 | Registrar evidencia de cada ejecución: resultado, script, salida RMAN, duración, ubicación | Alta |
| RF-11 | Consultar historial de ejecuciones con filtros | Media |
| RF-12 | Mostrar advertencia cuando la base esté en NOARCHIVELOG | Alta |
| RF-13 | Mostrar recomendación de incluir archived logs cuando la base esté en ARCHIVELOG | Media |
| RF-14 | Generar alertas de control preventivo (sin programación, fallida, sin espacio, etc.) | Alta |

### Requerimientos No Funcionales

| ID | Requerimiento |
|---|---|
| RNF-01 | La interfaz no debe requerir que el administrador escriba SQL ni código RMAN manualmente |
| RNF-02 | El sistema debe distinguir claramente entre advertencia, recomendación y alerta crítica |
| RNF-03 | La evidencia de ejecución debe ser inmutable una vez registrada |
| RNF-04 | El agente RMAN debe ejecutarse en el mismo servidor que Oracle |
| RNF-05 | El sistema no debe asumir que una estrategia es correcta solo porque el script no tuvo errores |

---

## 6. Modelo de Estrategia: QUÉ · CÓMO · CUÁNDO

La herramienta organiza cada estrategia alrededor de tres preguntas fundamentales:

### QUÉ respaldar
Define los objetos Oracle que formarán parte del respaldo:

| Objeto | Descripción | Cuándo incluirlo |
|---|---|---|
| DATABASE | Todos los datafiles de la base | Respaldo completo o cuando no se conocen los tablespaces críticos |
| TABLESPACE | Uno o más tablespaces específicos | Cuando se conocen los tablespaces con datos críticos |
| DATAFILE | Archivos de datos específicos | Respaldos parciales muy específicos |
| CONTROL FILE | Archivo de control de la base | Siempre; es indispensable para la recuperación |
| SPFILE | Archivo de parámetros del servidor | Siempre; permite reconstruir la instancia |
| ARCHIVED REDO LOGS | Logs archivados | Solo en ARCHIVELOG; permiten recuperación puntual |

### CÓMO respaldar
Define el tipo y las opciones del respaldo (ver sección 7 para análisis detallado).

### CUÁNDO respaldar
Define la programación mediante expresión cron, fecha de inicio, ventana de respaldo y el mecanismo de automatización (Agente RMAN).

---

## 7. Análisis de Tipos de Respaldo

### 7.1 Respaldo Completo

**Descripción:** Copia todos los bloques utilizados de los objetos seleccionados, sin importar si fueron modificados recientemente.

**Script RMAN generado:**
```sql
BACKUP DATABASE FORMAT '%d_%T_%U';
```

| Criterio | Evaluación |
|---|---|
| Espacio de almacenamiento | Alto — copia todo en cada ejecución |
| Tiempo de respaldo | Alto — proporcional al tamaño total |
| Frecuencia típica | Semanal o quincenal |
| Complejidad de recuperación | Baja — un solo conjunto de archivos |
| Recuperación puntual | Solo si se combina con archived logs |

**Cuándo es apropiado:** Como base de una estrategia incremental (nivel 0), en bases de datos pequeñas donde el tiempo de respaldo es aceptable, o cuando la simplicidad de la recuperación es prioritaria.

---

### 7.2 Respaldo Incremental Nivel 0

**Descripción:** Funciona como un respaldo completo pero marca un punto de partida para estrategias incrementales. RMAN registra internamente que los bloques subsiguientes deben compararse contra este respaldo.

**Script RMAN generado:**
```sql
BACKUP INCREMENTAL LEVEL 0 DATABASE FORMAT '%d_%T_%U';
```

| Criterio | Evaluación |
|---|---|
| Espacio de almacenamiento | Alto — equivalente al completo |
| Tiempo de respaldo | Alto — igual que el completo |
| Frecuencia típica | Semanal (inicio del ciclo incremental) |
| Complejidad de recuperación | Media — requiere nivel 1 subsiguientes |
| Recuperación puntual | Sí, con archived logs |

**Cuándo es apropiado:** Como primer paso de una estrategia incremental. Sin un nivel 0 previo, RMAN no puede ejecutar un nivel 1.

---

### 7.3 Respaldo Incremental Nivel 1 (Diferencial)

**Descripción:** Respalda únicamente los bloques que han cambiado desde el **último respaldo incremental de cualquier nivel** (nivel 0 o nivel 1 anterior). Es el comportamiento por defecto de `INCREMENTAL LEVEL 1`.

**Script RMAN generado:**
```sql
BACKUP INCREMENTAL LEVEL 1 DATABASE FORMAT '%d_%T_%U';
```

| Criterio | Evaluación |
|---|---|
| Espacio de almacenamiento | Bajo — solo bloques modificados desde el último respaldo |
| Tiempo de respaldo | Bajo — proporcional al volumen de cambios |
| Frecuencia típica | Diaria |
| Complejidad de recuperación | Alta — requiere nivel 0 + todos los nivel 1 anteriores |
| Recuperación puntual | Sí, con archived logs |

**Cuándo es apropiado:** Entornos con alta frecuencia de cambios pero donde el tamaño de los respaldos es una preocupación. Requiere una cadena completa de respaldos para la recuperación.

---

### 7.4 Incremental Diferencial (explícito)

**Descripción:** Semánticamente equivalente al Nivel 1 estándar. Se usa cuando se quiere ser explícito en el tipo de estrategia. En RMAN, `INCREMENTAL LEVEL 1` sin la cláusula `CUMULATIVE` es diferencial por defecto.

**Diferencia clave con acumulativo:** Solo respalda cambios desde el **último nivel 1**, no desde el nivel 0. Esto produce respaldos más pequeños pero una cadena más larga para recuperar.

---

### 7.5 Incremental Acumulativo

**Descripción:** Respalda todos los bloques que han cambiado desde el **último respaldo nivel 0**, independientemente de cuántos nivel 1 intermedios existan.

**Script RMAN generado:**
```sql
BACKUP INCREMENTAL LEVEL 1 CUMULATIVE DATABASE FORMAT '%d_%T_%U';
```

| Criterio | Evaluación |
|---|---|
| Espacio de almacenamiento | Medio-Alto — crece con el tiempo desde el nivel 0 |
| Tiempo de respaldo | Medio — más que el diferencial |
| Frecuencia típica | Diaria o semanal |
| Complejidad de recuperación | Baja — solo nivel 0 + el último acumulativo |
| Recuperación puntual | Sí, con archived logs |

**Cuándo es apropiado:** Cuando se prefiere una recuperación más simple (menos archivos) a cambio de respaldos más grandes. Ideal cuando el tiempo de recuperación (RTO) es más crítico que el espacio de almacenamiento.

---

### 7.6 Tabla Comparativa

| Criterio | Completo | Incremental L0 | Incremental L1 (Dif) | Acumulativo |
|---|---|---|---|---|
| Espacio por respaldo | Alto | Alto | Bajo | Medio |
| Tiempo de respaldo | Alto | Alto | Bajo | Medio |
| Archivos para recuperar | 1 | 1 + logs | L0 + todos L1 + logs | L0 + último acum + logs |
| Simplicidad de recuperación | ★★★ | ★★ | ★ | ★★ |
| Requiere nivel 0 previo | No | No | Sí | Sí |

---

## 8. Análisis ARCHIVELOG / NOARCHIVELOG

### 8.1 Modo NOARCHIVELOG

En este modo, Oracle **sobrescribe los redo logs** al completar cada ciclo. Una vez sobrescritos, los cambios anteriores no pueden reproducirse.

**Implicaciones para respaldo:**
- Solo se pueden realizar respaldos **offline** (con la base cerrada) en la mayoría de versiones.
- **No es posible** la recuperación hasta un punto exacto en el tiempo (*point-in-time recovery*).
- Si ocurre un fallo entre dos respaldos completos, **se pierden todos los cambios** desde el último respaldo.
- RMAN puede hacer respaldos en línea con NOARCHIVELOG en algunas configuraciones, pero la recuperación es siempre al estado del último respaldo completo.

**Advertencia mostrada por la herramienta:**
> ⚠ La base de datos se encuentra en modo NOARCHIVELOG. Las posibilidades de recuperación son más limitadas. Revise la estrategia de respaldo y los requerimientos de recuperación antes de continuar.

### 8.2 Modo ARCHIVELOG

En este modo, Oracle **archiva cada redo log** antes de sobrescribirlo, conservando un historial completo de todos los cambios realizados.

**Implicaciones para respaldo:**
- Permite **recuperación hasta un punto exacto en el tiempo** (antes de un error específico).
- RMAN puede respaldar la base mientras está en línea y en uso (*online backup*).
- Los archived redo logs son **parte esencial de la estrategia**: sin ellos no es posible la recuperación puntual.
- Es el modo recomendado para cualquier base de datos en producción.

**Recomendación mostrada por la herramienta:**
> ✓ La base de datos se encuentra en modo ARCHIVELOG. Considere incorporar el respaldo periódico de los archived redo logs dentro de la estrategia para mejorar las posibilidades de recuperación.

### 8.3 Decisión del administrador

La herramienta **no cambia automáticamente el modo de archivado**. Esta es una decisión de arquitectura que debe tomar el administrador. Para activar ARCHIVELOG en Oracle:

```sql
-- Requiere reiniciar la base en modo MOUNT
SHUTDOWN IMMEDIATE;
STARTUP MOUNT;
ALTER DATABASE ARCHIVELOG;
ALTER DATABASE OPEN;
```

---

## 9. Controles Preventivos Propuestos

Un **control preventivo** actúa antes de que ocurra el incidente. La herramienta implementa los siguientes:

| Control | Mecanismo | Riesgo que mitiga |
|---|---|---|
| Planificación obligatoria antes de ejecutar | El wizard obliga a definir QUÉ, CÓMO y CUÁNDO antes de generar el script | Disponibilidad e Integridad |
| Validación de la estrategia | `RmanScriptBuilder.advertencias()` detecta configuraciones incompletas o contradictorias antes de generar el script | Integridad |
| Aprobación del administrador | El script se genera en estado PENDIENTE_APROBACION; requiere revisión explícita | Integridad |
| Automatización con agente | El agente RMAN ejecuta los scripts según la programación sin intervención manual | Disponibilidad |
| Evidencia inmutable | Cada ejecución registra script, resultado, salida RMAN y timestamps | Disponibilidad e Integridad |
| Verificación post-respaldo | El script generado incluye `CROSSCHECK BACKUP` y `LIST BACKUP SUMMARY` | Integridad |
| Alerta SIN_PROGRAMACION | Detecta estrategias activas sin programación | Disponibilidad |
| Alerta RESPALDO_VENCIDO | Detecta cuando la hora programada pasó y no hay ejecución registrada | Disponibilidad |
| Alerta EJECUCION_FALLIDA | Detecta fallos en las últimas 24 horas | Disponibilidad e Integridad |
| Alerta NOARCHIVELOG | Advierte sobre limitaciones de recuperación | Integridad |
| Alerta SIN_RESPALDO_RECIENTE | Detecta bases sin respaldo exitoso en 7 días | Disponibilidad |
| Alerta SIN_ESPACIO | Detecta disco al 85%+ de uso en el servidor Oracle | Disponibilidad |
| Alerta SCRIPT_INCOMPLETO | Detecta estrategias sin script generado | Integridad |

### Respuesta a la pregunta orientadora

**¿Cómo puede una herramienta de gestión de estrategias de respaldo utilizar RMAN para establecer controles preventivos que permitan reducir los riesgos asociados con la disponibilidad e integridad de la información de una base de datos Oracle?**

La herramienta responde esta pregunta en tres niveles:

1. **Antes de la ejecución:** Obliga a definir una estrategia estructurada (QUÉ, CÓMO, CUÁNDO), valida su coherencia, alerta sobre condiciones de riesgo (NOARCHIVELOG, sin espacio, script incompleto) y requiere aprobación explícita del administrador antes de ejecutar. Esto reduce el riesgo de ejecutar respaldos incorrectos o incompletos.

2. **Durante y después de la ejecución:** El agente RMAN ejecuta el script en el servidor Oracle, captura la salida completa de RMAN y la reporta al backend. El resultado se clasifica como EXITOSO, CON_ADVERTENCIAS o FALLIDO basándose tanto en el código de salida como en los patrones de la salida (`RMAN-`, `ORA-`). Esto garantiza que el sistema no asuma éxito por la simple ausencia de errores.

3. **Monitoreo continuo:** El panel de alertas evalúa periódicamente el estado de todas las estrategias y detecta condiciones que podrían comprometer la disponibilidad o integridad de la información, permitiendo al administrador actuar antes de que ocurra un incidente.
