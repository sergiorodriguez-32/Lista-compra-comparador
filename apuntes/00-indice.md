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
4. [Flyway y cómo escribir el DDL](04-flyway-y-ddl.md) — migraciones, `CREATE TABLE`, tipos, restricciones
5. [Entidades JPA y sus anotaciones](05-jpa-entidades-anotaciones.md) — `@Entity`, `@Id`, `@Column`, Lombok, validación
6. [Chuleta Java vs Python/JS/Kotlin](06-chuleta-java-vs-python-js-kotlin.md) — sintaxis y nombres para no liarme
7. [Spring Data JPA: repositorios](07-spring-data-repositorios.md) — `JpaRepository`, métodos derivados, `Optional`, `@Query`
8. [Problemas encontrados y solución](99-problemas-encontrados.md) — se amplía siempre

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
Hecho: esqueleto del backend, credenciales en `.env`, conexión a PostgreSQL (PRs #1-#3
mergeados) y migración `V1__crear_tabla_users.sql` aplicada (tabla `users` creada y visible en
la ventana Database de IntelliJ).

Rama activa: `feature/entidad-user`.

Siguiente paso exacto: crear `User.java` en
`backend/src/main/java/com/sergiorodriguez/listacompra/core/` (apuntes en
[05-jpa-entidades-anotaciones.md](05-jpa-entidades-anotaciones.md)): `@Entity`, `@Table(name = "users")`,
`@Id` + `@GeneratedValue(IDENTITY)`, `@Column`, Lombok `@Getter/@Setter/@NoArgsConstructor`;
arrancar la app para que Hibernate valide el mapeo. Después: `UserRepository`, commit y PR.

Recordatorio: arrancar Docker (`docker compose up -d`) antes de ejecutar la app, y tener
`DB_USER`/`DB_PASSWORD` en la configuración de ejecución de IntelliJ.
