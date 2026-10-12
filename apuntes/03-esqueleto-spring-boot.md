# 3. Esqueleto de Spring Boot (Spring Initializr)

Generado en https://start.spring.io con: Maven, Java 21, Spring Boot **4.1.1**, grupo
`com.sergiorodriguez`, artifact `listacompra`, empaquetado Jar.

## Estructura del proyecto

```
backend/
├── pom.xml                         dependencias y build (Maven)
├── mvnw / mvnw.cmd                 Maven Wrapper (Linux-Mac / Windows)
├── .mvn/wrapper/                   configuración del wrapper (qué versión de Maven baja)
├── .gitignore  .gitattributes      ignorados de Git (target/, .idea...) y fin de línea
├── HELP.md                         enlaces de ayuda generados (no hace falta tocarlo)
└── src/
    ├── main/
    │   ├── java/com/sergiorodriguez/listacompra/
    │   │   └── ListacompraApplication.java     clase de arranque
    │   └── resources/
    │       ├── application.properties          configuración (BD, puerto, logs...)
    │       ├── db/migration/                   migraciones Flyway: V1__xxx.sql, V2__xxx.sql
    │       ├── static/                         ficheros estáticos (no los usaremos: el front es Angular)
    │       └── templates/                      plantillas HTML (tampoco las usaremos)
    └── test/java/.../ListacompraApplicationTests.java   tests
```

## `pom.xml`: qué es y qué hay dentro

Es el `package.json` de Java: declara qué librerías usa el proyecto y cómo se construye.

```xml
<parent>                                  <!-- hereda configuración y VERSIONES de Spring Boot -->
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
<properties>
    <java.version>21</java.version>        <!-- versión de Java con la que se compila -->
</properties>
```

Gracias al `<parent>`, **no pongo versión** en la mayoría de dependencias: Spring Boot ya
sabe qué versiones son compatibles entre sí.

### Dependencias (los "starters")

Un *starter* es un paquete que trae varias librerías ya compatibles entre sí.

| Dependencia | Para qué sirve en este proyecto |
|---|---|
| `spring-boot-starter-webmvc` | crear la API REST (controladores, JSON, Tomcat embebido). En Boot 4 se llama `webmvc`; en versiones antiguas era `spring-boot-starter-web` |
| `spring-boot-starter-data-jpa` | acceso a BD con JPA + Hibernate + Spring Data (repositorios) |
| `postgresql` (scope `runtime`) | driver JDBC de PostgreSQL; solo hace falta al ejecutar, no al compilar |
| `spring-boot-starter-flyway` + `flyway-database-postgresql` | migraciones de BD versionadas (soporte específico para Postgres) |
| `spring-boot-starter-validation` | Bean Validation: `@NotBlank`, `@Email`, `@Size`... en los datos de entrada |
| `lombok` (optional) | genera getters/setters/constructores con anotaciones (`@Getter`, `@Setter`...) |
| `spring-boot-devtools` (runtime) | reinicio automático al cambiar código mientras desarrollo |
| `spring-boot-starter-*-test` (scope `test`) | JUnit, Mockito y utilidades de test; solo para los tests |

Scopes: `compile` (por defecto, siempre), `runtime` (solo al ejecutar), `test` (solo en
tests), `optional` (no se propaga a quien dependa de mi proyecto).

### El bloque `maven-compiler-plugin` con Lombok

Lombok funciona como *annotation processor*: durante la compilación genera código. Ese
bloque le dice al compilador dónde encontrarlo. Sin él, `@Getter` no generaría nada.

## La clase de arranque

```java
package com.sergiorodriguez.listacompra;

@SpringBootApplication
public class ListacompraApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListacompraApplication.class, args);
    }
}
```

`SpringApplication.run(...)` arranca Spring: crea el contenedor de objetos (beans),
configura todo y levanta el servidor Tomcat embebido.

### Anotaciones vistas hasta ahora

| Anotación | Qué hace |
|---|---|
| `@SpringBootApplication` | marca la clase principal. Es un atajo de tres anotaciones: `@Configuration` (esta clase puede definir beans), `@EnableAutoConfiguration` (Spring configura solo lo que detecta en el classpath: si ve el driver de Postgres y JPA, prepara el DataSource y Hibernate) y `@ComponentScan` (busca clases anotadas, como `@RestController` o `@Service`, **en este paquete y sus subpaquetes**) |
| `@SpringBootTest` | en un test, arranca el contexto completo de Spring, como la app real |
| `@Test` | (JUnit 5) marca un método como test |

**Importante:** como `@ComponentScan` solo mira desde el paquete de la clase de arranque
hacia abajo, todas mis clases deben vivir bajo `com.sergiorodriguez.listacompra`.

## Qué es Tomcat

**Tomcat** es un *servidor web* (más exactamente, un contenedor de servlets) escrito en Java:
el programa que **escucha en un puerto** (por defecto el 8080), recibe las peticiones HTTP
(`GET /api/...`), se las pasa al código de la aplicación y devuelve la respuesta.

- Antes, las apps Java se empaquetaban en un `.war` y había que instalar Tomcat aparte y
  desplegarlas en él.
- Spring Boot lo trae **embebido** (dentro de la dependencia `spring-boot-starter-webmvc`):
  al arrancar la app, arranca Tomcat. Por eso basta con `java -jar app.jar` o el ▶ del IDE.
- Equivalencias: en Python, `uvicorn`/`gunicorn` con FastAPI/Flask; en Node, el servidor de
  Express (`app.listen(3000)`).
- Se ve en el log: `Tomcat initialized with port 8080 (http)` y `Tomcat started on port 8080`.
  La API estará en `http://localhost:8080`.
- Cambiar el puerto: `server.port=8081` en `application.properties`.

Flujo de una petición: `Navegador/Angular → Tomcat (8080) → DispatcherServlet de Spring MVC →
@RestController → Servicio → Repositorio → PostgreSQL`.

## `application.properties`

Formato `clave=valor`; Spring lee estas claves para configurarse.

```properties
spring.application.name=listacompra

# Conexión a PostgreSQL (contenedor listacompra-db, puerto 5435)
spring.datasource.url=jdbc:postgresql://localhost:5435/listacompracomparador
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# Hibernate solo valida; el esquema lo crea Flyway
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

| Propiedad | Para qué |
|---|---|
| `spring.datasource.url` | URL JDBC: `jdbc:postgresql://host:puerto/base_de_datos` |
| `${DB_USER}`, `${DB_PASSWORD}` | placeholders: Spring los sustituye por **variables de entorno**, así las credenciales no se suben a Git. En IntelliJ: *Edit Configurations → Environment variables* (`DB_USER=...;DB_PASSWORD=...`) |
| `ddl-auto=validate` | al arrancar comprueba que entidades y tablas encajan, sin crear ni cambiar nada |
| `open-in-view=false` | evita mantener la conexión de BD abierta durante toda la petición HTTP (buena práctica) |

### Qué debe aparecer en el log al arrancar bien
- `HikariPool-1 - Start completed` → conexión al pool (Hikari) hecha.
- `Successfully validated N migrations` / `Creating Schema History table "flyway_schema_history"` →
  Flyway ha entrado y creó su tabla de control.
- `Started ListacompraApplication in X seconds` → todo en marcha (Tomcat en el puerto 8080).
- Con 0 migraciones sale un `WARN No migrations found`: es normal hasta crear la primera.

## Comandos de Maven

```bash
./mvnw spring-boot:run     # arrancar la app (en Windows: mvnw.cmd spring-boot:run)
./mvnw test                # ejecutar los tests
./mvnw clean package       # limpiar target/ y generar el .jar en target/
```

`mvnw` es el *wrapper*: descarga la versión de Maven indicada en
`.mvn/wrapper/maven-wrapper.properties` y la usa, sin instalar Maven en el sistema.
