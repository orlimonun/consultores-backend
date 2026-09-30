# Consultores Backend — Evaluación de riesgo ISO/IEC 27002

Backend en Spring Boot para la evaluación del riesgo en administración de
bases de datos basada en ISO/IEC 27002.

## Requisitos
- Java 21
- Maven 3.9+
- PostgreSQL corriendo con la base `ConsultoresDB` (ver `schema_auditoria_iso27002.sql`)

## Configuración
Ajustá `src/main/resources/application.properties`:
- `spring.datasource.url/username/password` según tu contenedor Docker.
- `app.jwt.secret`: una clave de al menos 32 caracteres.

Si preferís que Hibernate cree las tablas en vez de correr el script SQL,
cambiá `spring.jpa.hibernate.ddl-auto` de `validate` a `update`.

## Ejecutar
    mvn spring-boot:run

Al arrancar se crea un usuario admin por defecto:
`admin@consultores.cr` / `admin123`

## Endpoints principales
- `POST /api/auth/register` — registrar usuario
- `POST /api/auth/login` — login, devuelve token JWT
- `GET  /api/organizaciones` — CRUD de organizaciones (requiere token)
- `GET  /api/controles` — catálogo de controles
- `GET  /api/resultados/auditoria/{id}` — cálculo de madurez y riesgo

Todas las rutas excepto `/api/auth/**` requieren header
`Authorization: Bearer <token>`.
