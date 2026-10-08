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
3. [Problemas encontrados y solución](99-problemas-encontrados.md) — se amplía siempre

## Próximos temas (se irán añadiendo)

- Esqueleto Spring Boot (Initializr, `pom.xml`, `application.properties`)
- Anotaciones de Spring: `@SpringBootApplication`, `@Entity`, `@Id`, `@Column`,
  `@RestController`, `@Service`, `@Repository`, `@Autowired`...
- Flyway: migraciones
- Spring Data JPA: repositorios
- Spring Security + JWT
- Angular: componentes, servicios, signals, routing, HttpClient

## Dónde lo dejé

Rama `chore/docker-postgres`: `docker-compose.yml` creado y PostgreSQL arrancado
(contenedor `listacompra-db`, puerto 5435). Pendiente: commit, push y Pull Request.
Después: esqueleto del backend con Spring Initializr.
