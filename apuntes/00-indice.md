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
3. [Esqueleto de Spring Boot](03-esqueleto-spring-boot.md) — estructura, `pom.xml`, starters, primeras anotaciones
4. [Problemas encontrados y solución](99-problemas-encontrados.md) — se amplía siempre

## Próximos temas (se irán añadiendo)

- Esqueleto Spring Boot (Initializr, `pom.xml`, `application.properties`)
- Anotaciones de Spring: `@SpringBootApplication`, `@Entity`, `@Id`, `@Column`,
  `@RestController`, `@Service`, `@Repository`, `@Autowired`...
- Flyway: migraciones
- Spring Data JPA: repositorios
- Spring Security + JWT
- Angular: componentes, servicios, signals, routing, HttpClient

## Dónde lo dejé

Hecho: `docker-compose.yml` con PostgreSQL (PR #2 mergeado).
Rama activa: `feature/backend-esqueleto`. Proyecto generado con Spring Initializr (Boot 4.1.1).
Hecho: conexión a PostgreSQL verificada (la app arranca, Flyway crea `flyway_schema_history`).
Siguiente: commit + PR del esqueleto; luego rama `feature/entidad-user` con la primera
migración (`V1__crear_tabla_users.sql`) y la entidad `User`.
