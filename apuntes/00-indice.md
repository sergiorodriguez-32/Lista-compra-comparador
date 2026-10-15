# Apuntes — Lista de la compra comparador

Manual de aprendizaje que se va ampliando durante el desarrollo, para poder consultarlo
aunque no pueda usar la IA (clase, entrevistas, trabajo). Cada archivo es un tema.
Se escribe con **código de ejemplo** y, en cuanto empecemos con Spring, con una explicación
de **para qué sirve cada anotación (`@`)**.

> Diferencia con `docs/`: `docs/` dice **qué** hace la app y cómo está diseñada.
> `apuntes/` explica **cómo funcionan las herramientas** que uso para construirla.

## Índice

1. [Docker Compose y PostgreSQL](01-docker-compose-postgres.md)
2. [Flujo de Git: ramas, commits y Pull Requests](02-git-flujo.md)
3. [Esqueleto de Spring Boot](03-esqueleto-spring-boot.md) — estructura, `pom.xml`, starters, Tomcat, primeras anotaciones
4. [Flyway y cómo escribir el DDL](04-flyway-y-ddl.md) — migraciones, `CREATE TABLE`, tipos, restricciones
5. [Entidades JPA y sus anotaciones](05-jpa-entidades-anotaciones.md) — `@Entity`, `@Id`, `@Column`, Lombok, validación
6. [Chuleta Java vs Python/JS/Kotlin](06-chuleta-java-vs-python-js-kotlin.md) — sintaxis y nombres para no liarme
7. [Spring Data JPA: repositorios](07-spring-data-repositorios.md) — `JpaRepository`, métodos derivados, `Optional`, `@Query`
8. [Capas de la API: controlador, servicio, DTO y validación](08-capas-web-servicio-dto.md) — `@RestController`, `@Service`, `@Valid`, BCrypt
9. [Receta CRUD: plantillas para un módulo nuevo](09-receta-crud.md) — migración → entidad → repositorio → DTOs → servicio → controlador
10. [Tests unitarios: JUnit 5 + Mockito + AssertJ](10-tests-unitarios.md) — mocks, Given/When/Then, errores típicos
11. [Spring Security y JWT: login y protección de la API](11-spring-security-jwt.md) — JWT, BCrypt, filtro, `SecurityConfig`, preguntas de entrevista
12. [Problemas encontrados y solución](99-problemas-encontrados.md) — se amplía siempre

## Próximos temas (se irán añadiendo)

- Tests de repositorio y de controlador (`@DataJpaTest`, `MockMvc`)
- Refresh tokens y logout
- Angular: componentes, servicios, signals, routing, HttpClient
- Apuntes de preparación de entrevistas (resumen de las preguntas típicas de cada tema)

## Dónde lo dejé

Hecho (PRs mergeados en `main`): Docker + PostgreSQL, esqueleto del backend, credenciales en
`.env`, migración `V1__crear_tabla_users.sql`, entidad `User` y `UserRepository`.

También mergeado: registro de usuarios (RF-01, `POST /api/auth/register`) con BCrypt, DTOs,
`UserService`, `AuthController`, `GlobalExceptionHandler` y los tests de `UserService`.

Hecho en la rama `feature/login-jwt` (probado con Postman y con tests): login con JWT (RF-02,
`POST /api/auth/login`), `JwtService`, `JwtAuthFilter`, `SecurityConfig` (el resto de rutas
exigen token) y tests de `login` y de `JwtService`. Pendiente: abrir el PR y mergear.

Pendiente de RF-01 (cuando exista la entidad): crear la lista de Compra por defecto "Mi compra".

Siguiente: el primer endpoint protegido de verdad (p. ej. `GET /api/users/me` o el CRUD de
Despensa) para ver el filtro JWT en acción, y después empezar el frontend con Angular.

Recordatorio: arrancar Docker (`docker compose up -d`) antes de ejecutar la app, y tener
`DB_USER`/`DB_PASSWORD` en la configuración de ejecución de IntelliJ (usar siempre el ▶ de la
barra superior con `ListacompraApplication` seleccionada).

Git: **primero merge del PR en GitHub, después** `git checkout main` + `git pull` +
`git branch -d <rama>`.
