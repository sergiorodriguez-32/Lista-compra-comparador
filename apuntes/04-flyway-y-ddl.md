# 4. Flyway y cómo escribir el DDL (PostgreSQL)

## Para qué sirve Flyway

La estructura de la base de datos (tablas, columnas, restricciones) también es parte del
código y cambia con el tiempo. Flyway la **versiona**: cada cambio es un fichero `.sql`
numerado, y Flyway los ejecuta **en orden y una sola vez** sobre cualquier base de datos.

Así, quien clone el repo y arranque la app tiene exactamente el mismo esquema que yo, y en
producción se aplican solo las migraciones que falten.

### Cómo funciona por dentro

1. Al arrancar Spring Boot, Flyway se conecta a la BD (mismo `spring.datasource.*`).
2. Busca ficheros en `src/main/resources/db/migration/` (ruta por defecto).
3. Mira su tabla de control **`flyway_schema_history`** (la crea él) para saber cuáles ya se
   aplicaron.
4. Ejecuta las pendientes, en orden de versión, y las anota con su **checksum**.
5. Solo después de eso arranca Hibernate, que con `ddl-auto=validate` comprueba que las
   entidades Java encajan con las tablas.

Por eso en el log sale Flyway **antes** que Hibernate.

### Reglas de oro

- Nombre: `V<versión>__<descripción>.sql` → **dos** guiones bajos. Ej.: `V1__crear_tabla_users.sql`,
  `V2__crear_tabla_product.sql`.
- Las versiones son números crecientes (1, 2, 3...). Una migración nueva = un número nuevo.
- **Una migración ya aplicada no se edita jamás.** Flyway guarda su checksum; si cambia, la
  app falla al arrancar con "checksum mismatch". Para corregir algo se crea una `V` nueva
  (`ALTER TABLE ...`).
- Excepción en desarrollo local: si aún no se ha mergeado y no hay datos que perder, se puede
  tirar la base (`docker compose down -v`) y reescribir la migración.
- Una migración = un cambio con sentido (una tabla, un índice...).
- Hibernate no crea tablas (`ddl-auto=validate`): **todo el esquema sale de las migraciones**.

### Ver qué ha aplicado Flyway

```bash
docker exec -it listacompra-db psql -U <POSTGRES_USER> -d <POSTGRES_DB>
```
```sql
SELECT installed_rank, version, description, success FROM flyway_schema_history;
```

---

## DDL: CREATE TABLE en PostgreSQL

DDL (*Data Definition Language*) = las sentencias que definen estructura: `CREATE`, `ALTER`, `DROP`.

### Estructura básica

```sql
CREATE TABLE nombre_tabla (
    columna1  TIPO  [restricciones],
    columna2  TIPO  [restricciones],
    [restricciones de tabla]
);
```

Cada columna se separa con coma; **la última no lleva coma**; la sentencia acaba en `;`.

### Tipos de datos que usaré

| Tipo | Para qué | Java equivalente |
|---|---|---|
| `BIGINT` | enteros grandes (ids) | `Long` |
| `INTEGER` | enteros normales | `Integer` |
| `VARCHAR(n)` | texto con longitud máxima | `String` |
| `TEXT` | texto sin límite (ingredientes, descripciones) | `String` |
| `NUMERIC(p,s)` | decimales exactos (dinero, cantidades). `p` = dígitos totales, `s` = decimales. `NUMERIC(10,2)` → hasta 99999999.99 | `BigDecimal` |
| `BOOLEAN` | verdadero/falso | `Boolean` |
| `DATE` | fecha | `LocalDate` |
| `TIMESTAMP` | fecha y hora | `LocalDateTime` |

Para dinero y cantidades nunca `FLOAT`/`DOUBLE` (errores de redondeo); sí `NUMERIC`.

### Restricciones (constraints)

| Restricción | Significado |
|---|---|
| `PRIMARY KEY` | identifica la fila; implica `NOT NULL` y `UNIQUE` |
| `NOT NULL` | la columna no admite nulos |
| `UNIQUE` | no se repiten valores en la tabla |
| `DEFAULT valor` | valor si no se indica al insertar |
| `CHECK (condicion)` | la fila solo es válida si se cumple la condición |
| `REFERENCES tabla(col)` | clave foránea: el valor debe existir en la otra tabla |

### Id autogenerado

```sql
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

La BD asigna 1, 2, 3... sola. En JPA se corresponderá con
`@GeneratedValue(strategy = GenerationType.IDENTITY)`.

### Ejemplo completo (tabla de ejemplo, no es del proyecto)

```sql
CREATE TABLE categoria_ejemplo (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(50)    NOT NULL UNIQUE,
    descripcion TEXT,                                   -- admite nulos (no lleva NOT NULL)
    precio_base NUMERIC(10,2)  NOT NULL DEFAULT 0,
    activa      BOOLEAN        NOT NULL DEFAULT TRUE,
    creada_en   TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT ck_categoria_precio CHECK (precio_base >= 0)
);
```

### Dos formas de poner una restricción

```sql
-- En la propia columna (forma corta)
email VARCHAR(255) NOT NULL UNIQUE,

-- A nivel de tabla, con nombre (mejor para mensajes de error y para ALTER)
CONSTRAINT uq_users_email UNIQUE (email),
CONSTRAINT ck_users_umbral CHECK (low_stock_threshold >= 0)
```

Poner nombre (`CONSTRAINT nombre ...`) ayuda: el error de la BD lo cita, y permite
borrarla después con `ALTER TABLE ... DROP CONSTRAINT nombre`.

### Clave foránea (la usaremos en `shopping_list` → `users`)

```sql
CREATE TABLE pedido_ejemplo (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES users(id),
    total      NUMERIC(10,2) NOT NULL
);
```

### Cambios posteriores con ALTER (en una migración nueva)

```sql
ALTER TABLE users ADD COLUMN telefono VARCHAR(20);
ALTER TABLE users DROP COLUMN telefono;
ALTER TABLE users ALTER COLUMN display_name SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_algo UNIQUE (columna);
```

### Convenciones del proyecto

- Nombres de tablas y columnas en **snake_case** y en inglés (`password_hash`, `display_name`).
- Tabla de usuarios = **`users`** (plural), porque `user` es palabra reservada en PostgreSQL.
- En Java los campos serán `camelCase` (`passwordHash`); Spring Boot traduce solo
  `passwordHash` ↔ `password_hash` (estrategia de nombres por defecto).
- Comentarios en SQL: `-- una línea` o `/* varias */`.

---

## Verificar una migración

1. Guardar el `.sql` en `db/migration/`.
2. Arrancar la app y mirar el log: `Migrating schema "public" to version "1 - crear tabla users"`
   y `Successfully applied 1 migration`.
3. Comprobar la tabla en `psql`:

```sql
\dt                 -- lista las tablas (deben salir users y flyway_schema_history)
\d users            -- describe columnas, tipos, restricciones e índices
```

### Errores típicos

| Error | Causa |
|---|---|
| `Migration checksum mismatch` | edité una migración ya aplicada → crear una nueva, o `docker compose down -v` en local |
| `syntax error at or near ...` | coma de más al final, falta una coma, o palabra reservada como nombre |
| `Detected resolved migration not applied` / `Found more than one migration with version` | número de versión repetido o fuera de orden |
| La app arranca pero Flyway no hace nada y avisa `No migrations found` | fichero en carpeta equivocada o nombre mal escrito (un solo `_`, mayúscula/minúscula en la `V`) |
