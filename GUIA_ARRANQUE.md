# Guía de arranque — Herramienta de Gestión de Estrategias de Respaldo RMAN
**EIF402 · Administración de Bases de Datos · UNA**

> Esta guía asume macOS con Homebrew instalado. Para Linux los comandos de instalación cambian pero el resto es idéntico.

---

## Requisitos previos

| Herramienta | Versión mínima | Verificar |
|---|---|---|
| Java (JDK) | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 18+ | `node -v` |
| PostgreSQL | 14+ | `psql --version` |

---

## Paso 1 — Instalar dependencias faltantes

```bash
brew install maven postgresql@16
```

Iniciar PostgreSQL y dejarlo activo al reiniciar:

```bash
brew services start postgresql@16
```

Verificar que corrió:

```bash
psql postgres -c "SELECT version();"
```

---

## Paso 2 — Crear la base de datos

```bash
psql postgres -c "CREATE DATABASE \"ConsultoresDB\";"
psql postgres -c "CREATE USER root WITH PASSWORD 'root';"
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE \"ConsultoresDB\" TO root;"
```

---

## Paso 3 — Crear las tablas

Primero el schema principal de auditoría ISO 27002 (si tienes el archivo):

```bash
psql -U root -d ConsultoresDB -f consultores-backend-master/backend/src/main/resources/schema_auditoria_iso27002.sql
```

Luego la migración del módulo de respaldo RMAN:

```bash
psql -U root -d ConsultoresDB -f consultores-backend-master/backend/src/main/resources/respaldo_migration.sql
```

> **Alternativa rápida:** Si no tienes el schema, puedes dejar que Hibernate lo genere automáticamente.
> Abre `consultores-backend-master/backend/src/main/resources/application.properties` y cambia la línea:
> ```
> spring.jpa.hibernate.ddl-auto=validate
> ```
> por:
> ```
> spring.jpa.hibernate.ddl-auto=update
> ```
> Esto solo la primera vez. Después vuelve a `validate`.

---

## Paso 4 — Arrancar el backend

```bash
cd "consultores-backend-master/backend"
mvn spring-boot:run
```

La primera vez descarga todas las dependencias de Maven (~2-3 minutos). Al terminar verás:

```
Started ConsultoresApplication on port 8080
```

El backend queda disponible en `http://localhost:8080`.

> **Usuario por defecto creado automáticamente:**
> - Email: `admin@consultores.cr`
> - Contraseña: `admin123`

---

## Paso 5 — Arrancar el frontend

Abrir **una segunda terminal** (el backend debe seguir corriendo en la primera):

```bash
cd "auditoria-iso27002-frontend-master"
npm install
npm run dev
```

Abrir en el navegador: **http://localhost:5173**

---

## Paso 6 — Verificar que todo funciona

1. Ir a `http://localhost:5173`
2. Iniciar sesión con `admin@consultores.cr` / `admin123`
3. En el sidebar izquierdo, bajo la sección **Respaldo RMAN**, deben aparecer 4 opciones:
   - Bases RMAN
   - Estrategias
   - Historial RMAN
   - Alertas RMAN

---

## Flujo de uso del módulo de respaldo

```
1. Bases RMAN        → Registrar la instancia Oracle (host, servicio, modo ARCHIVELOG)
        ↓
2. Estrategias       → Crear estrategia: QUÉ respaldar · CÓMO · prioridad
        ↓
3. Estrategias       → Generar script RMAN (botón "⚙ Script")
        ↓
4. Estrategias       → Revisar el script generado (paso 5 del wizard)
        ↓
5. Estrategias       → Configurar programación cron (paso 4 del wizard)
        ↓
6. Estrategias       → Ejecutar manualmente (botón "▶ Ejecutar")
        ↓
7. Historial RMAN    → Ver evidencia de la ejecución (resultado, salida RMAN)
        ↓
8. Alertas RMAN      → Panel de control preventivo
```

---

## Sobre el agente RMAN

El agente RMAN es un proceso separado que debe correr **en el mismo servidor donde está instalado Oracle**.
Se encarga de recibir el script del backend y ejecutarlo con `rman target /`.

**No se levanta en tu Mac de desarrollo.** Se necesita cuando se va a ejecutar respaldos reales contra una instancia Oracle on-premise.

Para compilarlo en el servidor Oracle:

```bash
cd consultores-backend-master/agente
mvn package -DskipTests
```

Configura las variables de entorno en el servidor:

```bash
export AGENTE_INSTANCIA_ID=mi-oracle-01
export AGENTE_NOMBRE="Oracle XE Lab"
export AGENTE_EMPRESA="UNA - EIF402"
export BACKEND_URL=http://<IP-del-backend>:8080/api/monitoreo/agente/push
export AGENTE_CLAVE=0215c0bf2dac95d6f9c6a295caed7f78e52f39ff1c3c001fb93468379376f972
export ORACLE_SID=XE
export ORACLE_HOME=/u01/app/oracle/product/19.0.0/dbhome_1
export RMAN_PATH=$ORACLE_HOME/bin/rman
export ORACLE_MONITOR_PASSWORD=mon123
```

Arrancarlo:

```bash
java -jar target/consultores-agente-0.0.1-SNAPSHOT.jar
```

---

## Variables de entorno del backend (producción / Render)

Si se despliega en la nube, configurar estas variables en el panel de Render.
A continuación se explica de dónde obtener cada una:

---

### `DATABASE_URL`
**De dónde:** El panel de tu base de datos en Render (o Neon, Supabase, etc.).
Ve a **Dashboard → tu base de datos → Connect → External Connection String**.
Formato: `jdbc:postgresql://<host>:<puerto>/<nombre_bd>`

### `DB_USER` y `DB_PASSWORD`
**De dónde:** Los definiste al crear la base de datos en Render/Neon.
En local son `root` / `root` (ver Paso 2 de esta guía).

### `JWT_SECRET`
**De dónde:** Lo generas tú. Debe ser una cadena aleatoria de **mínimo 32 caracteres**.
Genera una con este comando en la terminal:
```bash
openssl rand -hex 32
```
Copia el resultado y pégalo como valor de esta variable.

### `ORACLE_WALLET_B64`
**De dónde:** Oracle Cloud Infrastructure (OCI) → tu instancia Autonomous Database → **DB Connection → Download Wallet**.
Descarga el ZIP del wallet, luego conviértelo a base64:
```bash
base64 -i Wallet_nombreDB.zip | tr -d '\n'
```
Copia el resultado (texto largo) y pégalo como valor.
> Si no usas Oracle Autonomous (solo usas una instancia local/XE), **deja esta variable vacía** — el monitor Oracle simplemente no se activará.

### `ORACLE_USERNAME`
**De dónde:** El usuario administrador de tu instancia Oracle Autonomous.
Por defecto es `ADMIN`. Puedes verlo en OCI → Autonomous Database → **Database Users**.

### `ORACLE_PASSWORD`
**De dónde:** La contraseña que pusiste al crear la instancia Autonomous en OCI,
o la que configuraste para el usuario `ADMIN`.

### `AGENTE_CLAVE`
**De dónde:** La defines tú — es una contraseña compartida entre el backend y los agentes.
Usa el mismo comando que para el JWT:
```bash
openssl rand -hex 32
```
**Importante:** el mismo valor debe estar configurado tanto en el backend (esta variable)
como en el agente (`AGENTE_CLAVE` en su `application.properties`).
El valor que ya viene por defecto en el código es:
`0215c0bf2dac95d6f9c6a295caed7f78e52f39ff1c3c001fb93468379376f972`
(cámbialo en producción).

---

### Resumen en tabla

| Variable | De dónde se obtiene | Obligatoria |
|---|---|---|
| `DATABASE_URL` | Panel de Render/Neon → Connection String | ✅ Sí |
| `DB_USER` | Panel de Render/Neon → usuario de BD | ✅ Sí |
| `DB_PASSWORD` | Panel de Render/Neon → contraseña de BD | ✅ Sí |
| `JWT_SECRET` | Tú la generas: `openssl rand -hex 32` | ✅ Sí |
| `ORACLE_WALLET_B64` | OCI → Autonomous DB → Download Wallet → base64 | ❌ Opcional |
| `ORACLE_USERNAME` | OCI → Autonomous DB → Database Users (default: `ADMIN`) | Solo si usas Autonomous |
| `ORACLE_PASSWORD` | OCI → contraseña del usuario `ADMIN` | Solo si usas Autonomous |
| `AGENTE_CLAVE` | Tú la generas: `openssl rand -hex 32` | ✅ Sí (si usas agentes) |

---

## Solución de problemas comunes

### `Connection refused` al arrancar el backend
PostgreSQL no está corriendo. Ejecutar:
```bash
brew services restart postgresql@16
```

### `relation "auditoria" does not exist`
Las tablas no fueron creadas. Usar `ddl-auto=update` la primera vez o correr el script SQL manualmente.

### Puerto 8080 ocupado
```bash
lsof -i :8080
kill -9 <PID>
```

### Puerto 5173 ocupado
Vite buscará automáticamente el siguiente puerto disponible (5174, 5175…). Revisar la terminal.

### El frontend no conecta con el backend
Verificar que en `auditoria-iso27002-frontend-master/src/api/client.js` la URL sea:
```js
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
```

---

## Estructura del proyecto

```
ADMIN BASES DE DATOS/
├── consultores-backend-master/
│   ├── backend/          ← Spring Boot (Java 21) — puerto 8080
│   │   └── src/main/resources/
│   │       ├── application.properties
│   │       └── respaldo_migration.sql   ← ejecutar en PostgreSQL
│   └── agente/           ← Agente RMAN (corre en el servidor Oracle)
├── auditoria-iso27002-frontend-master/  ← React + Vite — puerto 5173
└── GUIA_ARRANQUE.md      ← este archivo
```
