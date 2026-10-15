# 11. Spring Security y JWT: login y protección de la API

## El problema: HTTP no tiene memoria

Cada petición HTTP llega sin saber quién la manda. Para recordar al usuario hay dos caminos:

| | Sesión (cookie) | JWT |
|---|---|---|
| Dónde está el estado | En el **servidor** (tabla `sessionId → usuario`) | En el **cliente**: el token lleva los datos |
| Qué manda el cliente | Un id sin significado | Un token con datos y firma |
| Varios servidores | Hay que compartir las sesiones | Cualquiera valida el token con la clave |
| Revocar / cerrar sesión | Fácil (se borra la sesión) | Difícil: vale hasta que caduca |

JWT es **stateless** (sin estado): el servidor no guarda quién ha iniciado sesión.

## Qué es un JWT (JSON Web Token)

No es de Java ni de Spring: es un estándar (RFC 7519). Son tres partes en Base64URL:

```
HEADER.PAYLOAD.SIGNATURE
```

- **Header:** el algoritmo de firma, p. ej. `{"alg":"HS256","typ":"JWT"}`.
- **Payload (claims):** los datos. Los nuestros:
  `{"sub":"1","email":"a@b.com","iat":1760000000,"exp":1760003600}`
  - `sub` = a quién pertenece (id), `iat` = cuándo se emitió, `exp` = cuándo caduca.
- **Signature:** `HMAC-SHA256(header + "." + payload, claveSecreta)`.

### Lo más importante: firmado NO es cifrado

| | Cifrado | Firmado (JWT normal, llamado JWS) |
|---|---|---|
| ¿Se puede leer? | No, sin la clave | **Sí, cualquiera** (Base64; prueba en jwt.io) |
| ¿Se puede modificar sin que se note? | No | **No**: la firma se rompe |
| Garantiza | Confidencialidad | Integridad y autenticidad |

Por eso **nunca se mete nada sensible en el payload** (contraseñas, tarjetas...). Solo
identidad y caducidad.

### Por qué no se puede falsificar

Si un atacante cambia `"sub":"1"` por `"sub":"2"`, no puede recalcular la firma porque **no
conoce la clave secreta**. El servidor recalcula la firma con su clave, no coincide y rechaza
el token. Toda la seguridad depende de que **solo el servidor conoce la clave**: por eso va en
`.env`, nunca en Git.

### La caducidad no es automática

Un JWT **no caduca por defecto**: la fecha `exp` la escribe quien lo crea (nuestro
`JwtService`). Sin `exp`, el token valdría para siempre. La librería comprueba `exp` al
validar y lanza `ExpiredJwtException` si ya pasó.

Duración corta (15-60 min) = un token robado sirve poco, pero hay que iniciar sesión más a
menudo (en apps grandes se soluciona con un *refresh token*). Larga = más cómodo y más
peligroso. Nosotros: 1 hora (`jwt.expiration-ms=3600000`).

## Cómo encajan BCrypt, JWT y HTTPS

Cada uno protege un riesgo distinto y hacen falta los tres:

| Capa | Protege | Con qué |
|---|---|---|
| Contraseña guardada | Que si roban la BD no vean las contraseñas | **BCrypt** (hash con sal, de un solo sentido) |
| El token | Que no se falsifique ni modifique | **Firma** del JWT |
| El tráfico | Que no se vea ni se robe por el camino | **HTTPS** (HTTP cifrado con TLS) |

- **BCrypt no cifra, hace un hash.** No se puede deshacer. En el login no se "descifra" el
  hash: `passwordEncoder.matches(clave, hash)` recalcula con la sal que lleva el propio hash y
  compara.
- `BCryptPasswordEncoder` es de Spring Security (módulo `spring-security-crypto`), pero el
  algoritmo BCrypt (1999) existe en todos los lenguajes.
- **HTTPS:** sin él, quien esté en medio (wifi pública, proveedor) puede copiar el token y
  usarlo hasta que caduque. En local (`http://localhost`) no importa; en producción es
  obligatorio.

## El flujo completo

```
LOGIN (una vez)
Front ──POST /api/auth/login {email, password}──► Backend
        Backend: findByEmail → matches(password, hash) → generateToken(user)
Front ◄──────────── {"token": "eyJ..."} ─────────┘
        (el front lo guarda)

RESTO DE PETICIONES (todas)
Front ──GET /api/pantry + cabecera "Authorization: Bearer eyJ..."──► Backend
        Filtro JWT: extractEmail(token) → valida firma y caducidad (SIN consultar la BD)
        Controlador → servicio → BD (para los datos de la despensa)
Front ◄──────────── los datos pedidos ───────────┘
```

Matices que suelen preguntar:
- La **contraseña viaja una sola vez**, en el cuerpo del login. Luego solo el token.
- El token va en la **cabecera**; los datos de la operación van en el **cuerpo** o la URL.
- El front **nunca habla con la BD**: todo pasa por el backend.
- La BD **no se consulta para saber quién eres** (eso sale del token), pero sí para devolver
  los datos que pides.
- En el token solo va la **identidad**, no los datos de la app (pesaría en cada petición y
  se quedaría desactualizado).

## Dependencias (`pom.xml`)

| Dependencia | Para qué |
|---|---|
| `spring-boot-starter-security` | Spring Security completo (filtros, protección de rutas). Incluye `spring-security-crypto` (BCrypt) |
| `jjwt-api` | Las clases que usamos al programar (`Jwts`, `Claims`...) |
| `jjwt-impl` + `jjwt-jackson` (`runtime`) | La implementación; solo hace falta al ejecutar |
| `spring-boot-starter-security-test` (`test`) | Herramientas para probar endpoints protegidos |

**Efecto de añadir el starter:** Spring Security **bloquea todo por defecto** (401 incluso en
el registro) hasta que se configura `SecurityConfig`.

Los starters son del grupo `org.springframework.boot`; `spring-security-crypto` era de
`org.springframework.security`. Mezclar el `groupId` da error en rojo.

## Configuración (`.env` + `application.properties`)

```properties
jwt.secret=${JWT_SECRET}
jwt.expiration-ms=3600000
```

`JWT_SECRET` (mínimo 32 caracteres para HS256) va en `.env` (ignorado por Git) y también en
las variables de entorno de la configuración de ejecución de IntelliJ. `.env.example` lleva
solo un texto de ejemplo.

## Las piezas del código

### `JwtService` (paquete `security`)

```java
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)      // firma con la clave secreta
                .compact();         // cabecera.payload.firma
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)             // verifica con la MISMA clave
                .build()
                .parseSignedClaims(token)    // valida firma y caducidad; si falla, lanza JwtException
                .getPayload()
                .get("email", String.class);
    }
}
```

- `@Value("${...}")` inyecta una propiedad del `application.properties`. **Ojo con el
  import:** hay dos `@Value`; el correcto es `org.springframework.beans.factory.annotation.Value`
  (no `lombok.Value`).
- HS256 es **simétrico**: la misma clave firma y verifica. (RS256 usa clave privada para
  firmar y pública para verificar; sirve cuando varios servicios validan tokens.)

### Login en `UserService`

```java
@Transactional(readOnly = true)
public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        throw new InvalidCredentialsException();
    }
    return new LoginResponse(jwtService.generateToken(user));
}
```

- **`Optional`** es una caja que puede tener un valor o estar vacía. `orElseThrow` abre la
  caja y, si está vacía, lanza la excepción. No elimina el error: lo vuelve **explícito y
  controlado** (un 401 limpio en vez de un `NullPointerException` y un 500).
- **Mismo error para "email no existe" y "contraseña incorrecta"** (mismo mensaje, mismo
  401): si no, un atacante podría descubrir qué emails están registrados (*enumeración de
  usuarios*).
- `readOnly = true`: solo lee de la BD; es una pista de optimización y evita escribir por error.
- `InvalidCredentialsException` → `GlobalExceptionHandler` la traduce a **401 Unauthorized**.

### DTOs: por qué `record`

`LoginRequest(email, password)` y `LoginResponse(token)` son records: un DTO solo transporta
datos y no debe cambiar, y el record da constructor, getters, `equals`... sin Lombok. Las
**entidades** (`User`) siguen siendo clases con Lombok porque JPA necesita mutarlas.
En el login la contraseña solo lleva `@NotBlank`, sin `@Size(min = 8)`: si algún día cambia la
regla de longitud, los usuarios antiguos deben poder seguir entrando.

### Endpoint

```java
@PostMapping("/login")   // sin @ResponseStatus: 200 por defecto (no crea nada)
public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return userService.login(request);
}
```

Es `POST` y no `GET` porque en un GET los datos irían en la URL, que queda en el historial,
los logs y los proxies.

### `JwtAuthFilter`: se ejecuta en cada petición

```
¿Cabecera "Authorization: Bearer <token>"?
   no → sigue sin autenticar
   sí → extractEmail(token)
          válido → "este usuario está autenticado" (SecurityContextHolder)
          falla  → sigue sin autenticar
→ la petición continúa hacia el controlador
```

- Extiende `OncePerRequestFilter`: se ejecuta **una vez** por petición.
- `SecurityContextHolder` es donde Spring apunta quién está autenticado **en esta petición**;
  se borra al terminar (por eso es stateless).
- `new UsernamePasswordAuthenticationToken(email, null, List.of())`: el usuario autenticado,
  sin contraseña (ya no hace falta) y sin roles.
- **El filtro no responde 401 él mismo**: solo marca al usuario si el token es bueno. Quien
  rechaza es `SecurityConfig` con `anyRequest().authenticated()`.

### `SecurityConfig`: las reglas

| Línea | Qué hace |
|---|---|
| `csrf(disable)` | CSRF protege apps con cookies de sesión; con JWT en cabecera no aplica |
| `SessionCreationPolicy.STATELESS` | Sin sesiones: cada petición se identifica solo con su token |
| `requestMatchers("/api/auth/**").permitAll()` | Registro y login públicos (aún no hay token) |
| `anyRequest().authenticated()` | Todo lo demás exige token válido |
| `HttpStatusEntryPoint(UNAUTHORIZED)` | Sin autenticar responde 401 (por defecto sería 403) |
| `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` | Coloca nuestro filtro en la cadena |

## Tests

### `UserServiceTest`: login con Mockito (3 casos)

- **Credenciales válidas:** `findByEmail` → `Optional.of(user)`, `matches` → `true`,
  `generateToken` → `"TOKEN"`; se comprueba que se devuelve ese token.
- **Email inexistente:** `findByEmail` → `Optional.empty()`; lanza `InvalidCredentialsException`
  y `verify(..., never())` en `matches` y `generateToken`.
- **Contraseña incorrecta:** `matches` → `false`; lanza la excepción y **no se genera token**.

`@Mock JwtService` + `@InjectMocks UserService`: sin el mock, Mockito inyectaría `null`.

### `JwtServiceTest`: sin Mockito

`JwtService` no tiene dependencias que simular, así que se construye de verdad
(`new JwtService(SECRET, UNA_HORA_MS)`) y se prueba con tokens reales. `@BeforeEach` prepara el
servicio y el usuario antes de cada test.

| Test | Cómo | Demuestra |
|---|---|---|
| Generar y leer | `extractEmail(generateToken(user))` | El ciclo funciona; el token tiene 3 partes |
| Firma alterada | Cambia un carácter de la firma | Modificar un token lo invalida |
| Otra clave | Otro `JwtService` con distinto secreto firma el token | Sin el secreto no se fabrican tokens válidos |
| Caducado | `expirationMs = -1000` | La caducidad la comprueba la librería (`ExpiredJwtException`) |

## Probar a mano (Postman o `backend/http/auth.http`)

| Petición | Esperado |
|---|---|
| `POST /api/auth/register` (email nuevo, **Body → raw → JSON**) | 201 |
| `POST /api/auth/login` correcto | 200 con `{"token": "..."}` |
| Login con contraseña mala | 401 "Credenciales incorrectas" |
| Login con email inexistente | 401, **el mismo mensaje** |
| Login con cuerpo inválido | 400 |
| Ruta protegida sin token | 401 |

Pega el token en **jwt.io** para ver el payload legible (y comprobar que modificar cualquier
carácter invalida la firma).

## Preguntas típicas de entrevista

- **¿Qué es un JWT?** Un token firmado y autocontenido, con tres partes, que permite
  autenticar sin guardar sesión en el servidor.
- **¿Está cifrado?** No, está firmado: se puede leer pero no modificar sin que se detecte.
- **¿Ventaja frente a sesiones?** Stateless: escala mejor con varios servidores.
- **¿Desventaja?** Difícil de revocar antes de que caduque; si lo roban, sirve hasta entonces.
- **¿Dónde guardarlo en el navegador?** `localStorage` (cómodo, vulnerable a XSS) o cookie
  `HttpOnly` (JavaScript no la lee, pero necesita protección CSRF).
- **¿Qué es el ataque `alg: none`?** Tokens sin firma aceptados por librerías antiguas; las
  actuales (jjwt) lo rechazan.
- **¿Por qué BCrypt y no cifrar la contraseña?** Un hash no se puede deshacer; el cifrado sí
  con la clave. Además BCrypt lleva sal (dos usuarios con la misma clave tienen hashes
  distintos) y es lento a propósito (encarece los ataques de fuerza bruta).
- **¿Cómo lo has implementado?** Login con BCrypt, emisión del JWT con jjwt y un filtro de
  Spring Security que valida el token en cada petición.

## Errores típicos

| Error | Causa |
|---|---|
| `Could not resolve placeholder 'JWT_SECRET'` | Falta `JWT_SECRET` en las variables de entorno de la configuración de ejecución de IntelliJ |
| Todo da 401 tras añadir el starter | Spring Security bloquea por defecto: falta `SecurityConfig` con `/api/auth/**` público |
| `pom.xml` en rojo en el `artifactId` | `groupId` equivocado: los starters son `org.springframework.boot` |
| `@Value` no inyecta nada | Import equivocado (`lombok.Value` en vez de `org.springframework.beans.factory.annotation.Value`) |
| `WeakKeyException` | El secreto mide menos de 32 caracteres |
| 415 en Postman | Body en `Text` en vez de `JSON` |
| `git commit "mensaje"` da `pathspec ... did not match` | Falta `-m` antes del mensaje |
