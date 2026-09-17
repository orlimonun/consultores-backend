# Agente de monitoreo — Oracle local

Reporta la salud de una instancia Oracle que no es alcanzable desde internet.

## Por qué existe

El backend está desplegado en la nube y la base está detrás de un router con NAT: el servidor no puede iniciar una conexión hacia ella. El agente sí puede salir hacia internet, porque esa dirección todos los firewalls la permiten.

En vez de que el monitor vaya a buscar los datos, el agente se los lleva. Es el mismo modelo de Datadog, Zabbix, Prometheus y New Relic. No abre ningún puerto ni expone ningún servicio.

## Dónde va

Como carpeta hermana de `backend/`, en el mismo repositorio:

```
consultores-backend/
├── backend/        ← la app que ya existe
└── agente/         ← este módulo
```

Son dos proyectos Maven independientes: cada uno con su `pom.xml` y su despliegue.

## Requisito previo: el usuario de solo lectura

No se debe conectar como SYS. Como SYS en `CDB$ROOT`, en la máquina que tiene Oracle:

```sql
CREATE USER c##monitor IDENTIFIED BY <clave> CONTAINER=ALL;
GRANT CREATE SESSION      TO c##monitor CONTAINER=ALL;
GRANT SET CONTAINER       TO c##monitor CONTAINER=ALL;
GRANT SELECT_CATALOG_ROLE TO c##monitor CONTAINER=ALL;
```

Tiene que ser un usuario **común** (el prefijo `c##`) y con `CONTAINER=ALL`, porque el agente necesita leer los dos contenedores: las bitácoras y el modo de archivado viven en el CDB, y los tablespaces y los índices del esquema en el PDB.

Comprobá que entra a los dos:

```
sqlplus c##monitor/<clave>@localhost:1521/XE
sqlplus c##monitor/<clave>@localhost:1521/XEPDB1
```

## Configuración

Editá `src/main/resources/application.properties`, o mejor, pasá los valores sensibles como variables de entorno.

| Propiedad | Qué es |
|---|---|
| `agente.instancia-id` | Identificador único de esta instancia en el tablero |
| `agente.nombre` | Cómo se muestra |
| `agente.empresa` | A qué cliente pertenece |
| `agente.esquema` | Esquema del proyecto, para medir distribución y claves foráneas |
| `agente.intervalo-ms` | Cada cuánto reporta. 60000 = un minuto |
| `agente.backend-url` | El endpoint de push |
| `agente.clave` | La misma cadena que `AGENTE_CLAVE` en Render |
| `oracle.cdb.url` / `oracle.pdb.url` | Los dos servicios |
| `oracle.user` / `oracle.password` | El usuario de solo lectura |

**Si hay más de una instancia vigilada, cada agente necesita su propio `agente.instancia-id`.** Si dos comparten el mismo, se pisan mutuamente y el tablero muestra una sola.

## Compilar y correr

```
cd agente
mvn clean package
```

En la máquina que tiene Oracle:

```
set AGENTE_CLAVE=<la clave de Render>
set ORACLE_MONITOR_PASSWORD=<la clave de c##monitor>
java -jar target/consultores-agente-0.0.1-SNAPSHOT.jar
```

Queda corriendo en primer plano y escribe una línea por ciclo:

```
Lectura OK  ISBD=94.5  IP=100.0 IM=100.0 IA=80.0 IR=100.0
Reporte enviado (local-xe-01)
```

Para probar contra un backend local en vez del de Render:

```
set BACKEND_URL=http://localhost:8080/api/monitoreo/agente/push
```

## Los cuatro indicadores

| | Contenedor | Qué mide |
|---|---|---|
| **IP** Procesos | CDB | Sesiones, bloqueos y cercanía al límite de procesos |
| **IM** Memoria | CDB | Uso de SGA y eficiencia del buffer cache |
| **IA** Archivos | PDB | Ocupación de tablespaces, datafiles, distribución del esquema y claves foráneas sin índice |
| **IR** Recuperación | CDB | Modo de archivado, multiplexado de bitácoras y área de recuperación |

`ISBD = 0.20·IP + 0.30·IM + 0.30·IA + 0.20·IR`

Memoria y archivos pesan más porque degradan el rendimiento de forma continua. Procesos solo importa cerca del límite. Recuperación se comporta casi como un interruptor: o la base está protegida o no lo está.

**IR es el indicador que una Autonomous no puede reportar.** Allí Oracle administra el almacenamiento y la recuperación y no permite modificarlos, así que no hay nada que el administrador pueda hacer bien o mal. En una instancia tradicional sí es responsabilidad humana, y por eso se mide.

## Dos fallos distintos, dos señales distintas

Si el agente no puede leer Oracle pero sí alcanzar el backend, envía un reporte marcando la instancia como caída: el tablero muestra "Oracle local inaccesible".

Si el agente no puede alcanzar el backend, no envía nada. El backend deja de recibir latidos y a los tres minutos marca la instancia sin contacto.

La diferencia importa: en el primer caso el agente vive y la base murió; en el segundo el que falló fue el agente o la red.

## Diferencia con la medición de la Autonomous

El agente calcula la ocupación de tablespaces con `DBA_TABLESPACE_USAGE_METRICS`, que compara contra el tamaño **máximo** al que el datafile puede crecer. El servicio que lee la Autonomous todavía compara contra el tamaño **actual**, y con `AUTOEXTEND` eso reporta cerca del 100 % siempre.

Es la corrección pendiente del lado del backend, y es de una consulta.
