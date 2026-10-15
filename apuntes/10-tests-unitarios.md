# 10. Tests unitarios: JUnit 5 + Mockito + AssertJ

## Qué es un test unitario

Un test unitario comprueba **una pieza aislada** (una clase, un método) sin arrancar la
aplicación, sin base de datos y sin red. Si el test pasa, esa pieza hace lo que debe; si
alguien la rompe más adelante, el test falla y avisa.

Para qué sirven (y qué se valora en entrevistas):
- Detectan errores **antes** de probar a mano con Postman.
- Permiten cambiar código sin miedo (si algo se rompe, un test lo dice).
- Documentan cómo debe comportarse el código.
- Los requisitos del proyecto (RNF-07) piden tests de la lógica de negocio crítica.

## Las tres librerías (ya vienen con `spring-boot-starter-test`)

| Librería | Para qué |
|---|---|
| **JUnit 5 (Jupiter)** | el motor de tests: `@Test`, ejecución y resultados |
| **Mockito** | crea **dobles** (mocks) de las dependencias para aislar la clase probada |
| **AssertJ** | comprobaciones legibles: `assertThat(x).isEqualTo(y)` |

Se ejecutan con `./mvnw test` o con el ▶ verde junto a la clase/método en IntelliJ.

## Estructura de un test: Given / When / Then (o Arrange / Act / Assert)

```java
@Test
void register_conDatosValidos_guardaUsuarioConPasswordCifrada() {
    // GIVEN: preparar datos y comportamiento de los mocks
    // WHEN:  ejecutar el método que se prueba
    // THEN:  comprobar el resultado y las interacciones
}
```

Convención de nombre: `metodo_condicion_resultadoEsperado`. Los tests van en
`src/test/java`, en el **mismo paquete** que la clase que prueban.

## Mock: un "doble" de una dependencia

`UserService` necesita un `UserRepository` (base de datos) y un `PasswordEncoder`. En un test
unitario **no queremos base de datos**, así que Mockito crea objetos falsos que:
- devuelven lo que yo les diga (`when(...).thenReturn(...)`)
- recuerdan cómo los han llamado (`verify(...)`)

## Anotaciones

| Anotación | Qué hace |
|---|---|
| `@Test` | marca un método como test |
| `@ExtendWith(MockitoExtension.class)` | activa Mockito en la clase de test |
| `@Mock` | crea un mock de ese tipo (ej. `UserRepository`) |
| `@InjectMocks` | crea la clase a probar e **inyecta** en ella los mocks anteriores |
| `@BeforeEach` | método que se ejecuta antes de cada test |
| `@DisplayName("...")` | nombre legible del test en los resultados |

## Mockito: lo imprescindible

```java
// Programar respuestas
when(repo.existsByEmail("a@b.com")).thenReturn(true);
when(encoder.encode("clave")).thenReturn("HASH");

// Responder según el argumento recibido (p. ej. simular el id que asigna la BD)
when(repo.save(any(Etiqueta.class))).thenAnswer(inv -> {
    Etiqueta e = inv.getArgument(0);
    e.setId(1L);
    return e;
});

// Comprobar que se llamó (o no) a un método
verify(repo).save(any());              // se llamó 1 vez
verify(repo, never()).save(any());     // nunca se llamó

// Capturar el argumento con el que se llamó, para inspeccionarlo
ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
verify(repo).save(captor.capture());
Etiqueta guardada = captor.getValue();
```

`any()`, `any(Etiqueta.class)`, `eq(...)` son *matchers*: "cualquier argumento", "uno de este
tipo", "exactamente este valor".

## AssertJ: comprobaciones

```java
import static org.assertj.core.api.Assertions.*;

assertThat(resultado.id()).isEqualTo(1L);
assertThat(resultado.email()).isEqualTo("a@b.com");
assertThat(guardada.getPasswordHash()).isNotEqualTo("clave").startsWith("$2");
assertThat(lista).hasSize(3).isNotEmpty();

// Comprobar que se lanza una excepción
assertThatThrownBy(() -> servicio.crear(request))
        .isInstanceOf(EtiquetaYaExisteException.class)
        .hasMessageContaining("a@b.com");
```

## Ejemplo completo (servicio ficticio `EtiquetaService`)

```java
@ExtendWith(MockitoExtension.class)
class EtiquetaServiceTest {

    @Mock
    private EtiquetaRepository etiquetaRepository;

    @InjectMocks
    private EtiquetaService etiquetaService;

    @Test
    void crear_conNombreLibre_guardaYDevuelveRespuesta() {
        // GIVEN
        EtiquetaRequest request = new EtiquetaRequest("oferta", null);
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> {
            Etiqueta e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });

        // WHEN
        EtiquetaResponse respuesta = etiquetaService.crear(request);

        // THEN
        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.nombre()).isEqualTo("oferta");
        verify(etiquetaRepository).save(any(Etiqueta.class));
    }

    @Test
    void obtener_siNoExiste_lanzaNotFound() {
        when(etiquetaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etiquetaService.obtener(99L))
                .isInstanceOf(EtiquetaNotFoundException.class);
    }
}
```

## Qué tests casos conviene escribir de cada método

1. **Camino feliz**: con datos correctos devuelve lo esperado.
2. **Cada regla de negocio**: lo que debe fallar (email duplicado, no existe...).
3. **Efectos secundarios**: qué se guarda y qué NO se guarda (`verify(..., never())`).
4. **Seguridad**: p. ej. que la contraseña guardada NO es la original.

## Errores típicos

| Error | Causa |
|---|---|
| `NullPointerException` dentro del test | el mock devuelve `null` porque no programé su respuesta con `when` |
| `UnnecessaryStubbingException` | programé un `when` que el código nunca usa (Mockito estricto): sobra o el camino es otro |
| `Wanted but not invoked` | `verify` espera una llamada que no ocurrió |
| El test no se ejecuta | clase o método con nombre sin `@Test`, o fuera de `src/test/java` |
| `@SpringBootTest` falla sin BD | ese test arranca toda la app: necesita PostgreSQL y las variables `DB_USER`/`DB_PASSWORD` |
