# 9. Receta para añadir un módulo nuevo (plantillas)

Cada módulo de la app (productos, despensa, listas...) se construye siempre igual. Esta es
la receta con plantillas para copiar y adaptar. Ejemplo ficticio: `Etiqueta` (campos `nombre`
y `peso`). Sustituir nombres por los del módulo real.

## Checklist (siempre en este orden)

1. [ ] Rama: `git checkout -b feature/etiquetas`
2. [ ] **Migración** Flyway: `V<n>__crear_tabla_etiqueta.sql`
3. [ ] **Entidad** (`core/`): campos + anotaciones JPA
4. [ ] **Repositorio** (`core/`): interfaz `JpaRepository`
5. [ ] **DTOs** (`web/`): `...Request` (entrada, con validación) y `...Response` (salida)
6. [ ] **Excepciones** de negocio (`core/exception/`) y su método en `GlobalExceptionHandler`
7. [ ] **Servicio** (`core/`): lógica, `@Transactional`
8. [ ] **Controlador** (`web/`): rutas REST
9. [ ] Arrancar la app y probar con Postman (201/200, 400, 404, 409...)
10. [ ] Commits pequeños (`feat:`...), push, PR, merge, `pull`

---

## 1. Migración (`db/migration/V2__crear_tabla_etiqueta.sql`)

```sql
CREATE TABLE etiqueta
(
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(50)   NOT NULL UNIQUE,
    peso   NUMERIC(6, 2) NOT NULL DEFAULT 1 CHECK (peso >= 0)
);
```

Si la tabla pertenece a un usuario: añadir
`usuario_id BIGINT NOT NULL REFERENCES users(id)`.

## 2. Entidad (`core/Etiqueta.java`)

```java
@Entity
@Table(name = "etiqueta")
@Getter
@Setter
@NoArgsConstructor
public class Etiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "peso", nullable = false, precision = 6, scale = 2)
    private BigDecimal peso = BigDecimal.ONE;
}
```

## 3. Repositorio (`core/EtiquetaRepository.java`)

```java
public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    boolean existsByNombre(String nombre);

    Optional<Etiqueta> findByNombre(String nombre);
}
```

## 4. DTOs (`web/`)

```java
public record EtiquetaRequest(
        @NotBlank @Size(max = 50) String nombre,
        @DecimalMin("0.0") BigDecimal peso
) {}

public record EtiquetaResponse(Long id, String nombre, BigDecimal peso) {}
```

## 5. Excepción de "no encontrado" (`core/exception/`)

```java
public class EtiquetaNotFoundException extends RuntimeException {
    public EtiquetaNotFoundException(Long id) {
        super("No existe la etiqueta con id " + id);
    }
}
```

Y en `GlobalExceptionHandler`:

```java
@ExceptionHandler(EtiquetaNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ProblemDetail handleNoEncontrada(EtiquetaNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
}
```

## 6. Servicio (`core/EtiquetaService.java`)

```java
@Service
@RequiredArgsConstructor
public class EtiquetaService {

    private final EtiquetaRepository etiquetaRepository;

    @Transactional
    public EtiquetaResponse crear(EtiquetaRequest request) {
        Etiqueta e = new Etiqueta();
        e.setNombre(request.nombre());
        if (request.peso() != null) {
            e.setPeso(request.peso());
        }
        return aRespuesta(etiquetaRepository.save(e));
    }

    @Transactional(readOnly = true)           // solo lee: más eficiente
    public List<EtiquetaResponse> listar() {
        return etiquetaRepository.findAll().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EtiquetaResponse obtener(Long id) {
        Etiqueta e = etiquetaRepository.findById(id)
                .orElseThrow(() -> new EtiquetaNotFoundException(id));
        return aRespuesta(e);
    }

    @Transactional
    public void borrar(Long id) {
        if (!etiquetaRepository.existsById(id)) {
            throw new EtiquetaNotFoundException(id);
        }
        etiquetaRepository.deleteById(id);
    }

    // convierte entidad -> DTO de salida (un único sitio, sin repetir)
    private EtiquetaResponse aRespuesta(Etiqueta e) {
        return new EtiquetaResponse(e.getId(), e.getNombre(), e.getPeso());
    }
}
```

`.stream().map(...).toList()` recorre la lista y transforma cada elemento (como `map` en
JavaScript o una list comprehension en Python).

## 7. Controlador (`web/EtiquetaController.java`)

```java
@RestController
@RequestMapping("/api/etiquetas")
@RequiredArgsConstructor
public class EtiquetaController {

    private final EtiquetaService etiquetaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)                       // 201
    public EtiquetaResponse crear(@Valid @RequestBody EtiquetaRequest request) {
        return etiquetaService.crear(request);
    }

    @GetMapping                                               // 200
    public List<EtiquetaResponse> listar() {
        return etiquetaService.listar();
    }

    @GetMapping("/{id}")
    public EtiquetaResponse obtener(@PathVariable Long id) {
        return etiquetaService.obtener(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)                    // 204, sin cuerpo
    public void borrar(@PathVariable Long id) {
        etiquetaService.borrar(id);
    }
}
```

## Qué cambia en cada módulo y qué se repite

| Pieza | Se repite igual | Lo que cambia |
|---|---|---|
| Migración | estructura `CREATE TABLE` | columnas y restricciones |
| Entidad | anotaciones de clase y de `id` | campos y sus `@Column` |
| Repositorio | `extends JpaRepository<Entidad, Long>` | métodos de búsqueda |
| DTOs | `record` con validación | campos |
| Servicio | `@Service`, `@RequiredArgsConstructor`, `@Transactional` | **las reglas de negocio** (esta es la parte que cambia de verdad) |
| Controlador | anotaciones de ruta y códigos HTTP | rutas y qué servicio llama |

## Dónde se me suele ir la cabeza (y cómo resolverlo)

- **¿Qué va en el servicio y qué en el controlador?** Si es una regla ("el email no se puede
  repetir") → servicio. Si es HTTP (ruta, código de respuesta, JSON) → controlador.
- **¿Por qué falla al arrancar?** Leer la última línea `Caused by:` del log: casi siempre
  indica el nombre exacto del problema (columna que falta, método mal nombrado, bean sin
  encontrar).
- **¿Qué código HTTP devuelvo?** Crear → 201; leer/actualizar → 200; borrar → 204; no existe
  → 404; datos mal → 400; duplicado → 409.
