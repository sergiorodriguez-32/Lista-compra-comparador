# 6. Chuleta: Java vs Python vs JavaScript vs Kotlin

Para no liarme al estudiar los cuatro a la vez. Lo que más confunde es el **orden** (tipo
antes o después del nombre), los **nombres** (snake_case o camelCase) y los `;`.

## Declarar una variable con valor inicial

| Lenguaje | Sintaxis | Notas |
|---|---|---|
| **Java** | `private BigDecimal lowStockThreshold = BigDecimal.ONE;` | **tipo primero**, luego nombre; acaba en `;` |
| **Kotlin** | `var lowStockThreshold: BigDecimal = BigDecimal.ONE` | `var`/`val`, nombre, **`:` tipo**; sin `;` |
| **Python** | `low_stock_threshold = Decimal(1)` | sin tipo obligatorio; sin `;` |
| **JavaScript** | `let lowStockThreshold = 1;` | `let`/`const`; sin tipo (en TypeScript: `let x: number = 1`) |

Regla Java: **`[visibilidad] Tipo nombre [= valor];`**. Siempre un tipo y siempre un nombre.
`private Long;` está mal: falta el nombre.

## Convención de nombres

| Qué | Java | Kotlin | Python | JavaScript |
|---|---|---|---|---|
| Variables / campos / métodos | `camelCase` | `camelCase` | `snake_case` | `camelCase` |
| Clases | `PascalCase` | `PascalCase` | `PascalCase` | `PascalCase` |
| Constantes | `MAYUSCULAS_CON_GUION` | igual | igual | igual |
| Columnas SQL | `snake_case` (aquí, no en Java) | | | |

Por eso en Java el campo es `passwordHash` y la columna es `password_hash`: el campo se
escribe en `camelCase` y la anotación dice qué columna es: `@Column(name = "password_hash")`.

## Otras diferencias que más lío dan

| Cosa | Java | Kotlin | Python | JavaScript |
|---|---|---|---|---|
| Fin de sentencia | `;` obligatorio | no | no | `;` opcional |
| Bloques | `{ }` | `{ }` | **sangría** | `{ }` |
| Cadena con variables | `"Hola " + nombre` | `"Hola $nombre"` | `f"Hola {nombre}"` | `` `Hola ${nombre}` `` |
| Comparar valores | `a.equals(b)` (texto) / `==` (números) | `a == b` | `a == b` | `a === b` |
| Nulo | `null` | `null` (con tipos `String?`) | `None` | `null` / `undefined` |
| Lista | `List<String>` | `List<String>` | `list[str]` | `string[]` / `Array` |
| Función | `void saluda() {}` (dentro de clase) | `fun saluda() {}` | `def saluda():` | `function saluda() {}` |
| Comentario | `//` y `/* */` | `//` y `/* */` | `#` | `//` y `/* */` |
| Importar | `import a.b.C;` | `import a.b.C` | `import x` / `from x import y` | `import { y } from 'x'` |

## Truco para no liarme

Cuando escribo Java, pienso: **"¿tengo [tipo] [nombre] y acabo en `;`?"**.
Y en cada campo de una entidad: anotaciones arriba, una línea de campo abajo.

```java
@Column(name = "low_stock_threshold", nullable = false, precision = 10, scale = 2)
private BigDecimal lowStockThreshold = BigDecimal.ONE;
//      └─ tipo ─┘  └──── nombre ────┘   └─ valor inicial ─┘
```
