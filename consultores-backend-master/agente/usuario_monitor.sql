-- =====================================================================
-- Usuario de solo lectura para el agente de monitoreo
-- ---------------------------------------------------------------------
-- EJECUTAR COMO SYS EN CDB$ROOT, en la maquina que tiene Oracle:
--   set ORACLE_SID=XE
--   sqlplus / as sysdba
--
-- POR QUE NO USAR SYS
-- El agente solo necesita leer vistas del diccionario. Conectarse como SYS
-- le daria permiso para borrar la base entera, y ademas el agente va a
-- correr desatendido con su clave escrita en una variable de entorno.
-- Principio de minimo privilegio: es un control directo de ISO 27002.
-- =====================================================================

ALTER SESSION SET CONTAINER = CDB$ROOT;
SHOW CON_NAME
-- debe decir CDB$ROOT

-- ---------------------------------------------------------------------
-- Tiene que ser un usuario COMUN (prefijo c##) y con CONTAINER=ALL,
-- porque el agente lee los dos contenedores:
--   CDB    -> bitacoras, modo de archivado, memoria, procesos
--   XEPDB1 -> tablespaces, tablas, indices del esquema
-- ---------------------------------------------------------------------
DEFINE clave_monitor = Monitor2026x

DROP USER c##monitor CASCADE;
-- si no existe, ignorar ORA-01918

CREATE USER c##monitor IDENTIFIED BY &clave_monitor CONTAINER=ALL;

GRANT CREATE SESSION      TO c##monitor CONTAINER=ALL;
GRANT SET CONTAINER       TO c##monitor CONTAINER=ALL;
GRANT SELECT_CATALOG_ROLE TO c##monitor CONTAINER=ALL;

-- SELECT_CATALOG_ROLE cubre las vistas DBA_* y V$ que el agente consulta:
--   v$session, v$process, v$resource_limit, v$sgastat, v$sysstat,
--   v$datafile, v$log, v$logfile, v$database, v$recovery_area_usage,
--   dba_tablespace_usage_metrics, dba_segments, dba_constraints,
--   dba_cons_columns, dba_ind_columns, dba_users

PROMPT ===================================================================
PROMPT Verificacion: las dos conexiones deben funcionar
PROMPT ===================================================================
PROMPT   sqlplus c##monitor/&clave_monitor@localhost:1521/XE
PROMPT   sqlplus c##monitor/&clave_monitor@localhost:1521/XEPDB1
PROMPT
PROMPT Y adentro de cada una:
PROMPT   SHOW CON_NAME
PROMPT   SELECT COUNT(*) FROM v$session;
PROMPT ===================================================================

-- ---------------------------------------------------------------------
-- Prueba rapida de que puede leer lo que necesita
-- ---------------------------------------------------------------------
SELECT username, common, account_status
FROM   dba_users WHERE username = 'C##MONITOR';

SELECT granted_role FROM dba_role_privs WHERE grantee = 'C##MONITOR';
