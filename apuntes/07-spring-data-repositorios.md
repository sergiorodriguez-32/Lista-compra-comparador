# 7. Spring Data JPA: repositorios

## Qué es un repositorio

Un **repositorio** es la capa que habla con la base de datos para una entidad. Con Spring Data
JPA **no se escribe la implementación**: se declara una *interfaz* y Spring genera la clase
en tiempo de arranque (con `INSERT`, `SELECT`, `UPDATE`, `DELETE` incluidos).

```
Controlador  →  Servicio  →  Repositorio  →  Hibernate  →  JDBC  →  PostgreSQL
(web)           (lógica)      (acceso a datos)
```

## La interfaz mínima

```java
package com.sergiorodriguez.listacompra.core;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EtiquetaEjemploRepository extends JpaRepository<EtiquetaEjemplo, Long> {
}
```

`JpaRepository<Entidad, TipoDelId>`: el primer tipo es la entidad y el segundo, el tipo de su
`@Id`. Sin escribir nada más ya tienes:

| Método heredado | SQL que genera (aprox.) |
|---|---|
| `save(entidad)` | `INSERT` si el id es `null`, `UPDATE` si ya existe |
| `findById(id)` | `SELECT ... WHERE id = ?` → devuelve `Optional` |
| `findAll()` | `SELECT * FROM tabla` |
| `existsById(id)` | `SELECT count(*) ... WHERE id = ?` |
| `count()` | `SELECT count(*) FROM tabla` |
| `deleteById(id)` | `DELETE ... WHERE id = ?` |

No hace falta `@Repository`: Spring detecta cualquier interfaz que extienda `JpaRepository`
dentro del paquete base (en el log: `Found N JPA repository interfaces`).

## Consultas derivadas del nombre del método

Spring lee el **nombre** del método y construye la consulta. Se escribe solo la firma:

```java
public interface EtiquetaEjemploRepository extends JpaRepository<EtiquetaEjemplo, Long> {

    Optional<EtiquetaEjemplo> findByNombre(String nombre);      // WHERE nombre = ?

    boolean existsByNombre(String nombre);                      // ¿existe alguno?

    List<EtiquetaEjemplo> findByActivaTrue();                   // WHERE activa = true

    List<EtiquetaEjemplo> findByPesoGreaterThan(BigDecimal p);  // WHERE peso > ?

    List<EtiquetaEjemplo> findByNombreContainingIgnoreCase(String texto); // LIKE %texto%
}
```

Estructura del nombre: **`findBy` + campo (Java, en PascalCase) + condición opcional**.

| Palabra | Efecto |
|---|---|
| `findBy`, `existsBy`, `countBy`, `deleteBy` | qué se hace |
| `And` / `Or` | combinar campos: `findByNombreAndActiva` |
| `GreaterThan`, `LessThan`, `Between` | comparaciones |
| `Containing`, `StartingWith` | `LIKE` |
| `IgnoreCase` | sin distinguir mayúsculas |
| `OrderBy...Asc/Desc` | ordenar |
| `True` / `False` | booleanos |

Importante: el nombre usa los **campos de la clase Java** (`passwordHash`), no las columnas
(`password_hash`). Si escribo mal el nombre del campo, **la app no arranca** (`No property
'xxx' found for type`).

## `Optional`: devolver "puede que no haya resultado"

`findByEmail` puede no encontrar nada. En vez de devolver `null`, Java usa `Optional<User>`:

```java
Optional<User> usuario = userRepository.findByEmail("a@b.com");

if (usuario.isPresent()) { ... }

User u = userRepository.findByEmail("a@b.com")
        .orElseThrow(() -> new IllegalArgumentException("No existe"));
```

## Consultas propias con `@Query` (cuando el nombre no basta)

```java
@Query("SELECT e FROM EtiquetaEjemplo e WHERE e.peso > :minimo AND e.activa = true")
List<EtiquetaEjemplo> buscarPesadas(@Param("minimo") BigDecimal minimo);
```

Es **JPQL**: se escribe con nombres de **clases y campos**, no de tablas y columnas. Para SQL
real: `@Query(value = "SELECT ... FROM tabla ...", nativeQuery = true)`.

## Anotaciones y piezas de este tema

| Elemento | Qué es |
|---|---|
| `JpaRepository<T, ID>` | interfaz base de Spring Data con el CRUD ya hecho |
| `Optional<T>` | contenedor para "puede no existir" |
| `@Query` | consulta JPQL o SQL propia sobre un método del repositorio |
| `@Param("x")` | enlaza un parámetro del método con `:x` de la consulta |

## Ver el SQL que genera Hibernate (para aprender)

En `application.properties`:

```properties
spring.jpa.show-sql=true
```

Imprime en el log cada `insert`, `select`... que lanza Hibernate. Útil para entender qué hay
por debajo; se quita o se deja en `false` en producción.
