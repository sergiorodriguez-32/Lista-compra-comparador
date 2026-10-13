# 8. Capas de la API: controlador, servicio, DTO y validación

## Las capas y por qué existen

```
Cliente (Angular / Postman)
   │  JSON por HTTP
   ▼
Controlador  (@RestController)  → recibe la petición, valida la entrada, devuelve la respuesta
   ▼
Servicio     (@Service)         → lógica de negocio (reglas, comprobaciones, transacciones)
   ▼
Repositorio  (JpaRepository)    → acceso a la base de datos
   ▼
Entidad      (@Entity)          → fila de una tabla
```

Cada capa tiene **una sola responsabilidad**. El controlador no toca la BD ni decide reglas
de negocio; el servicio no sabe nada de HTTP.

## DTO: por qué no se expone la entidad

Un **DTO** (*Data Transfer Object*) es una clase simple que define qué datos entran o salen
por la API. Nunca se devuelve la entidad directamente porque:

- `User` tiene `passwordHash`: **no puede salir en el JSON**.
- La entrada de un registro (`email`, `password`) no coincide con la entidad (`passwordHash`).
- Si cambia la tabla, no debe cambiar el contrato de la API.

En Java moderno los DTOs se escriben como **`record`**: clase inmutable con constructor,
getters, `equals`, `hashCode` y `toString` generados.

```java
public record EtiquetaRequest(
        @NotBlank @Size(max = 50) String nombre,
        @DecimalMin("0.0") BigDecimal peso
) {}

public record EtiquetaResponse(Long id, String nombre, BigDecimal peso) {}
```

Se usan como `request.nombre()` (sin `get`). Jackson convierte JSON ↔ record automáticamente.

## Anotaciones de validación (Bean Validation, `jakarta.validation.constraints`)

| Anotación | Comprueba |
|---|---|
| `@NotNull` | no es `null` |
| `@NotBlank` | texto no `null`, no vacío y no solo espacios |
| `@Email` | formato de email válido |
| `@Size(min = 8, max = 100)` | longitud del texto (o tamaño de lista) |
| `@Min(1)` / `@Max(10)` | valor numérico mínimo / máximo |
| `@DecimalMin("0.0")` | decimal mínimo |
| `@Pattern(regexp = "...")` | cumple una expresión regular |

Se pueden poner mensajes: `@NotBlank(message = "El nombre es obligatorio")`.
Solo se aplican si el controlador pide validar con **`@Valid`** (ver abajo).

## Anotaciones del controlador

| Anotación | Qué hace |
|---|---|
| `@RestController` | clase que atiende peticiones HTTP y devuelve JSON (= `@Controller` + `@ResponseBody`) |
| `@RequestMapping("/api/etiquetas")` | ruta base de todos los métodos de la clase |
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` | método HTTP + ruta de cada método |
| `@RequestBody` | convierte el JSON del cuerpo en el objeto Java del parámetro |
| `@Valid` | ejecuta las validaciones del DTO antes de entrar al método; si fallan → error 400 |
| `@PathVariable` | valor de la URL: `/etiquetas/{id}` → `@PathVariable Long id` |
| `@RequestParam` | parámetro de la query: `?q=leche` → `@RequestParam String q` |
| `@ResponseStatus(HttpStatus.CREATED)` | código HTTP de la respuesta (201) |

Ejemplo:

```java
@RestController
@RequestMapping("/api/etiquetas")
@RequiredArgsConstructor
public class EtiquetaController {

    private final EtiquetaService etiquetaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EtiquetaResponse crear(@Valid @RequestBody EtiquetaRequest request) {
        return etiquetaService.crear(request);
    }

    @GetMapping("/{id}")
    public EtiquetaResponse obtener(@PathVariable Long id) {
        return etiquetaService.obtener(id);
    }
}
```

Códigos HTTP habituales: `200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`
(datos inválidos), `401 Unauthorized` (sin login), `404 Not Found`, `409 Conflict`
(ej. email ya registrado).

## Servicio e inyección de dependencias

```java
@Service
@RequiredArgsConstructor
public class EtiquetaService {

    private final EtiquetaRepository etiquetaRepository;   // se inyecta solo

    @Transactional
    public EtiquetaResponse crear(EtiquetaRequest request) {
        if (etiquetaRepository.existsByNombre(request.nombre())) {
            throw new IllegalStateException("Ya existe");
        }
        EtiquetaEjemplo e = new EtiquetaEjemplo();
        e.setNombre(request.nombre());
        e = etiquetaRepository.save(e);
        return new EtiquetaResponse(e.getId(), e.getNombre(), e.getPeso());
    }
}
```

| Anotación | Qué hace |
|---|---|
| `@Service` | marca la clase como componente de lógica de negocio: Spring crea **una instancia** (un *bean*) y la guarda en su contenedor |
| `@Component` | lo mismo pero genérico (`@Service`, `@Repository`, `@RestController` son especializaciones) |
| `@RequiredArgsConstructor` (Lombok) | genera un constructor con todos los campos `final` |
| `@Transactional` | el método se ejecuta en una **transacción**: o se guarda todo o nada |
| `@Configuration` | clase que define beans con métodos |
| `@Bean` | método que devuelve un objeto que Spring debe gestionar (ej. `PasswordEncoder`) |
| `@Autowired` | pide a Spring que inyecte un bean (se evita: mejor constructor) |

### Inyección por constructor

Spring, al crear `EtiquetaController`, ve que su constructor necesita un `EtiquetaService`,
lo busca entre los beans y se lo pasa. Con `final` + `@RequiredArgsConstructor` no hace falta
escribir el constructor ni `@Autowired`. Es la forma recomendada (campos inmutables y
fáciles de testear).

## Errores: un único formato (`@RestControllerAdvice`)

Los errores de la API deben devolver siempre el mismo formato (RNF-06). Se centraliza en una
clase:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)   // falla @Valid
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail validacion(MethodArgumentNotValidException ex) { ... }

    @ExceptionHandler(EmailYaRegistradoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail emailDuplicado(EmailYaRegistradoException ex) { ... }
}
```

| Anotación | Qué hace |
|---|---|
| `@RestControllerAdvice` | clase que intercepta excepciones de **todos** los controladores |
| `@ExceptionHandler(X.class)` | método que se ejecuta cuando se lanza la excepción `X` |

## Contraseñas: BCrypt

Nunca se guarda la contraseña: se guarda su **hash** (RNF-01). BCrypt es un algoritmo de
hash lento y con *salt* aleatorio: la misma contraseña da hashes distintos cada vez.

```java
PasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("miClave123");          // $2a$10$....
boolean ok = encoder.matches("miClave123", hash);    // true
```

No se puede "descifrar": para comprobar un login se hashea lo que escribe el usuario y se
compara con `matches`.

Para tener un `PasswordEncoder` inyectable se declara un bean:

```java
@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## Probar un endpoint con Postman

Postman es un cliente de APIs: envía peticiones HTTP y muestra la respuesta. Para el registro:

1. Método **POST**, URL `http://localhost:8080/api/auth/register`.
2. Pestaña **Body → raw → JSON** (si se deja en `Text` da **415**):

```json
{
  "email": "sergio@example.com",
  "password": "Clave12345",
  "displayName": "Sergio"
}
```

3. **Send**. Respuestas obtenidas en este proyecto:

| Caso | Código | Cuerpo |
|---|---|---|
| Registro correcto | `201 Created` | `{ "id": 1, "email": "...", "displayName": "..." }` |
| Email sin formato / contraseña corta | `400 Bad Request` | `ProblemDetail` con `"errores": { campo: mensaje }` |
| Email ya registrado | `409 Conflict` | `ProblemDetail` con `"detail": "El email ya está registrado: ..."` |

Alternativa por terminal (en PowerShell, `curl.exe`):

```bash
curl -i -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"email":"a@b.com","password":"Clave12345","displayName":"A"}'
```

Comprobar que la contraseña se guardó cifrada (debe empezar por `$2a$10$`):

```sql
SELECT id, email, password_hash FROM users;
```
