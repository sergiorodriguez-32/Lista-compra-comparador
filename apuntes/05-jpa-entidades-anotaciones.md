# 5. Entidades JPA y sus anotaciones

## Qué es una entidad

Una **entidad** es una clase Java cuyas instancias representan **filas** de una tabla.
`User` (clase) ↔ tabla `users`; un objeto `User` ↔ una fila; cada campo ↔ una columna.

Quién hace qué (ver también el esquema JPA/Hibernate/JDBC):
- **Flyway** crea la tabla (SQL en `db/migration/`).
- **JPA/Hibernate** mapea la clase a esa tabla mediante **anotaciones**.
- Con `ddl-auto=validate`, al arrancar Hibernate comprueba que la clase **encaja** con la tabla;
  si no (columna que no existe, tipo incompatible), la app no arranca.

Todas las anotaciones JPA son de `jakarta.persistence.*` (en versiones antiguas era
`javax.persistence`, ojo con tutoriales viejos).

## Anotaciones JPA que usaré

| Anotación | Dónde | Qué hace |
|---|---|---|
| `@Entity` | clase | marca la clase como entidad: Hibernate la gestiona y la mapea a una tabla |
| `@Table(name = "users")` | clase | nombre de la tabla. Sin ella usaría el de la clase (`User`), que aquí no coincide con `users` |
| `@Id` | campo | la clave primaria |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | campo | el id lo genera la BD (columna `IDENTITY`). Hibernate no lo inserta: lo recoge después del `INSERT` |
| `@Column(...)` | campo | detalles de la columna: `name`, `nullable`, `unique`, `length`, `precision`, `scale` |
| `@Enumerated(EnumType.STRING)` | campo enum | guarda el enum como texto (`"SECO"`), no como número. Siempre `STRING` |
| `@ManyToOne`, `@OneToMany`... | campo | relaciones entre entidades (llegarán con `ShoppingList`, `PantryItem`...) |

### `@Column` en detalle

```java
@Column(name = "email", nullable = false, unique = true, length = 255)
private String email;

@Column(name = "low_stock_threshold", nullable = false, precision = 10, scale = 2)
private BigDecimal lowStockThreshold;
```

| Atributo | Significado |
|---|---|
| `name` | nombre de la columna. Opcional si coincide tras la conversión de nombres |
| `nullable = false` | refleja el `NOT NULL` |
| `unique = true` | refleja el `UNIQUE` |
| `length` | longitud del `VARCHAR` (solo informativa con `validate`; la manda el SQL) |
| `precision`, `scale` | los dos números de `NUMERIC(p, s)` |

Importante: con Flyway, estos atributos **no crean nada**; solo documentan y validan. La
verdad de la estructura es el SQL.

### Nombres: `camelCase` ↔ `snake_case`

Spring Boot usa por defecto `CamelCaseToUnderscoresNamingStrategy`: el campo `passwordHash`
se mapea solo a la columna `password_hash`. Por eso `@Column(name = ...)` es opcional cuando
solo cambia el formato; lo pondré igualmente en las entidades para que se vea claro qué
columna es cada campo.

### Tipos Java ↔ SQL

| Java | SQL |
|---|---|
| `Long` | `BIGINT` |
| `String` | `VARCHAR(n)` / `TEXT` |
| `BigDecimal` | `NUMERIC(p,s)` |
| `Boolean` | `BOOLEAN` |
| `LocalDate` | `DATE` |
| `LocalDateTime` | `TIMESTAMP` |

Usar `Long` (objeto) y no `long` para el id: antes de guardar la entidad, el id es `null`.

## Reglas de una entidad JPA

1. Necesita un **constructor sin argumentos** (puede ser `protected`): Hibernate crea los
   objetos por reflexión.
2. **No** puede ser `final` ni tener campos `final` (Hibernate crea proxies).
3. Campos privados con getters/setters.
4. Cuidado con `equals`/`hashCode`/`toString` generados automáticamente (ver Lombok).

## Lombok: menos código repetitivo

Lombok genera código en tiempo de compilación a partir de anotaciones.

| Anotación | Genera |
|---|---|
| `@Getter` | getters de todos los campos |
| `@Setter` | setters de todos los campos |
| `@NoArgsConstructor` | constructor sin argumentos (lo que pide JPA) |
| `@AllArgsConstructor` | constructor con todos los campos |
| `@Builder` | patrón builder (`User.builder().email(...).build()`) |
| `@Data` | getter + setter + `equals` + `hashCode` + `toString` |

**En entidades NO se usa `@Data`**: su `equals/hashCode` basado en todos los campos y su
`toString` pueden causar problemas con Hibernate (proxies, relaciones perezosas, bucles).
Se usa `@Getter` + `@Setter` + `@NoArgsConstructor`. `@Data` sí es habitual en DTOs/records.

Si IntelliJ no reconoce los getters generados: *Settings → Build, Execution, Deployment →
Compiler → Annotation Processors → Enable annotation processing*.

## Ejemplo completo (entidad de ejemplo, no es del proyecto)

Tabla:

```sql
CREATE TABLE etiqueta_ejemplo (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre   VARCHAR(50)   NOT NULL UNIQUE,
    peso     NUMERIC(6, 2) NOT NULL DEFAULT 1,
    activa   BOOLEAN       NOT NULL DEFAULT TRUE
);
```

Entidad:

```java
package com.sergiorodriguez.listacompra.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "etiqueta_ejemplo")
@Getter
@Setter
@NoArgsConstructor
public class EtiquetaEjemplo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "peso", nullable = false, precision = 6, scale = 2)
    private BigDecimal peso = BigDecimal.ONE;      // mismo valor por defecto que el SQL

    @Column(name = "activa", nullable = false)
    private boolean activa = true;
}
```

Observa:
- El valor por defecto del SQL (`DEFAULT 1`) **también** se pone en Java (`= BigDecimal.ONE`):
  Hibernate inserta todas las columnas, y si el campo fuera `null` violaría el `NOT NULL`.
- `import jakarta.persistence.*;` importa `@Entity`, `@Table`, `@Id`, `@Column`... de golpe.

## Cómo comprobar que el mapeo es correcto

Arrancar la app con `ddl-auto=validate`. Si hay discrepancia, falla con mensajes como:

| Error | Causa |
|---|---|
| `Schema-validation: missing table [xxx]` | `@Table(name)` no coincide con ninguna tabla |
| `Schema-validation: missing column [xxx] in table [yyy]` | el campo no tiene columna con ese nombre |
| `Schema-validation: wrong column type ... found [numeric], expecting [float]` | tipo Java incompatible con el tipo SQL (ej. `double` en vez de `BigDecimal`) |
| `No default constructor for entity` | falta `@NoArgsConstructor` / constructor vacío |
| `Not a managed type` / entidad ignorada | la clase está fuera del paquete base `com.sergiorodriguez.listacompra` |
