# 12. Angular: componentes, servicios, formularios y tests

Todo lo de este apunte está hecho de verdad en el proyecto (login, registro y pantalla de
productos). Los ejemplos salen del código real, en `frontend/src/app/`.

## Cómo se organiza el frontend

```
src/app/
  core/                  lo que no es una pantalla: servicios, guard, interceptor
    auth.ts              login, registro, token
    products.ts          modelo del producto y llamadas a /api/products
    auth-guard.ts        protege las rutas privadas
    auth-interceptor.ts  añade el token a cada petición
  pages/                 una carpeta por pantalla
    login/  register/  home/  product-list/  product-form/
  app.routes.ts          qué URL abre qué pantalla
  app.config.ts          configuración global (HttpClient, rutas, idioma)
```

## Los 4 archivos de un componente

| Archivo | Qué es |
|---|---|
| `product-list.ts` | La lógica: la clase, el estado (signals) y los métodos |
| `product-list.html` | Lo que se ve (la plantilla) |
| `product-list.scss` | Los estilos, solo de ese componente |
| `product-list.spec.ts` | Los tests (`spec` = *specification*: se leen como frases) |

## Equivalencias con lo que ya sé de Java

| Java / Spring | Angular |
|---|---|
| DTO (`ProductResponse`, `ProductRequest`) | `interface` de TypeScript |
| `@Service` con `@RequiredArgsConstructor` | `@Injectable` + `inject(...)` |
| `RestTemplate` / cliente HTTP | `HttpClient` |
| `Optional` / excepciones | `Observable` con `next` y `error` |
| `@RestController` y sus rutas | las rutas de `app.routes.ts` + el componente |
| `SecurityConfig` + `JwtAuthFilter` | `authGuard` + `authInterceptor` |
| `@Valid` y Bean Validation | `Validators` del formulario (y el backend sigue validando) |
| JUnit + Mockito + AssertJ | Vitest + `HttpTestingController` + `expect(...)` |
| `ProductServiceTest.java` en `src/test` | `products.spec.ts` al lado del archivo |

## Componente standalone

```ts
@Component({
  selector: 'app-product-list',
  imports: [RouterLink, MatButtonModule, MatCardModule, CurrencyPipe, DecimalPipe],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit { ... }
```

- `@Component` convierte la clase en un componente.
- `selector`: la etiqueta HTML con la que se usaría (`<app-product-list>`).
- `imports`: **todo lo que usa la plantilla** (pipes, botones de Material, `routerLink`...) hay
  que importarlo aquí. Si falta uno, la plantilla da error.
- `implements OnInit` + `ngOnInit()`: Angular lo llama una vez, justo después de crear el
  componente. Es donde se cargan los datos.

## Signals: el estado de la pantalla

Un **signal** es una "caja" con un valor que **avisa a la plantilla cuando cambia**.

```ts
lista = signal<ProductResponse[]>([]);   // crear con valor inicial
this.lista();                            // leer: se llama como una función
this.lista.set(productos);               // sustituir el valor
this.lista.update(l => l.filter(p => p.id !== id));  // cambiarlo a partir del anterior
readonly isLoggedIn = computed(() => this.token() !== null);  // valor calculado, se recalcula solo
```

En este proyecto no hay `zone.js`: la pantalla se actualiza porque la plantilla lee signals.
Por eso el estado va en signals y no en variables normales (una variable normal no avisa).

## La plantilla (HTML)

| Sintaxis | Para qué | Ejemplo real |
|---|---|---|
| `{{ ... }}` | Mostrar un valor | `{{ p.name }}` |
| `@if / @else if / @else` | Pintar una parte u otra | cargando / error / vacía / lista |
| `@for (x of lista; track x.id)` | Repetir elementos | una tarjeta por producto |
| `{{ valor \| pipe }}` | Formatear al mostrar | `{{ p.price \| currency: 'EUR' }}` → `2,50 €` |
| `[propiedad]="..."` | Pasar un valor a un elemento | `[disabled]="guardando()"` |
| `(evento)="..."` | Reaccionar a un evento | `(click)="borrar(p)"` |
| `[routerLink]="[...]"` | Enlace interno | `['/productos', p.id, 'editar']` → `/productos/7/editar` |

`track` le dice a Angular cómo identificar cada elemento, para no repintar toda la lista
cuando solo cambia uno.

## Servicios e inyección de dependencias

```ts
@Injectable({ providedIn: 'root' })   // una única instancia para toda la app
export class Products {
  private http = inject(HttpClient);  // Angular entrega la dependencia (como el constructor de Spring)
  list() { return this.http.get<ProductResponse[]>('/api/products'); }
}
```

## HttpClient y Observables

Cada método de `HttpClient` devuelve un **Observable**. La petición **no se envía hasta que
alguien hace `subscribe`**:

```ts
this.products.list().subscribe({
  next: productos => this.lista.set(productos),     // llegó la respuesta
  error: () => this.error.set('No se pudieron cargar'),  // falló: sin conexión, 401, 500...
});
```

Si escribo `products.list()` y nunca hago `subscribe`, no pasa nada: no se envía ninguna petición.

## Interceptor y guard (ya los tenía del login)

- **`authInterceptor`**: se ejecuta en **todas** las peticiones y, si hay token, clona la
  petición (son inmutables) y le añade `Authorization: Bearer <token>`. Por eso `Products` no
  sabe nada del token.
- **`authGuard`**: antes de abrir una ruta privada comprueba `isLoggedIn()`; si no hay sesión,
  redirige a `/login`. Se aplica con `canActivate: [authGuard]` en la ruta.

## Rutas con parámetros

```ts
{ path: 'productos/nuevo', component: ProductForm, canActivate: [authGuard] },
{ path: 'productos/:id/editar', component: ProductForm, canActivate: [authGuard] },
```

`:id` es un parámetro. El mismo componente sirve para crear y para editar: en `ngOnInit`
lee `route.snapshot.paramMap.get('id')`; si hay id, carga el producto; si no, es uno nuevo.

## Formularios reactivos

```ts
form = this.fb.group({
  name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
  price: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
  ...
});
```

- `Validators` son las reglas (equivalen a `@NotBlank`, `@Size`, `@DecimalMin` del backend).
  El formulario avisa antes de enviar, pero **el backend sigue validando siempre**.
- Los números empiezan en `null` (campo vacío) y no en `0`: con `0`, `required` no avisaría.
- `patchValue({...})` rellena el formulario (al editar). La API devuelve `null` en los campos
  vacíos pero un `<input>` necesita texto, de ahí `p.brand ?? ''`.
- `getRawValue()` devuelve los valores actuales para construir lo que se envía.
- `markAllAsTouched()` hace que se vean los errores de todos los campos al intentar enviar.
- `<mat-error>` solo se ve cuando el campo es inválido y el usuario lo ha tocado.
- Un texto vacío se envía como `null` (`v.brand.trim() || null`), que es "sin dato" en la BD.

## Tipos de TypeScript que se usan

```ts
export type ProductCategory = 'SECO' | 'CONGELADO' | 'FRESCO' | 'FRUTA' | 'DROGUERIA';
export const CATEGORIAS: Record<ProductCategory, string> = { SECO: 'Seco', ... };
```

- Un **tipo unión** (`'A' | 'B'`) equivale a un `enum` de Java: escribir `'PIZZA'` no compila.
- `Record<ProductCategory, string>` obliga a tener una entrada por cada categoría: si se añade
  una al tipo y se olvida en el mapa, no compila.
- El `!` de `v.price!` dice "sé que no es null" (el formulario ya validó `required`).

## Tests con Vitest (`*.spec.ts`)

```ts
describe('ProductList', () => {          // agrupa los tests de una pieza
  it('si no hay productos lo dice', async () => {   // un test
    http.expectOne('/api/products').flush([]);      // responde la petición pendiente
    await fixture.whenStable();                     // espera a que la pantalla se actualice
    expect(pantalla()).toContain('Todavía no tienes productos');
  });
});
```

| Pieza | Para qué |
|---|---|
| `TestBed` | Monta el componente o servicio en un entorno de prueba |
| `provideHttpClientTesting()` + `HttpTestingController` | Un "servidor falso": captura la petición (`expectOne`) y responde (`flush`) |
| `afterEach(() => http.verify())` | Falla el test si salió una petición que no esperaba |
| `vi.spyOn(window, 'confirm')` | Sustituye una función (aquí el cuadro "¿Borrar?") por una respuesta fija |
| `fixture.detectChanges()` | Hace el primer ciclo de pantalla (ejecuta `ngOnInit`) |

Se ejecutan con `npx ng test --watch=false` (desde `frontend/`).

## Receta para una pantalla nueva en Angular

1. [ ] Rama: `git checkout -b feature/...`
2. [ ] Modelo y servicio en `core/` (interfaces + llamadas a la API) y su `.spec.ts`
3. [ ] Componente en `pages/<nombre>/` (`.ts`, `.html`, `.scss`, `.spec.ts`)
4. [ ] Estado en signals: `cargando`, `error` y los datos
5. [ ] Plantilla con `@if` para los estados (cargando / error / vacío / datos)
6. [ ] Ruta en `app.routes.ts`, con `authGuard` si es privada
7. [ ] Enlace desde otra pantalla
8. [ ] `npx ng test --watch=false` y `npx ng build`
9. [ ] Probar con el backend arrancado (Docker + `spring-boot:run` + `ng serve`)
10. [ ] Commits pequeños, push, PR

## Problemas encontrados y cómo se resolvieron

- **Un valor de enum inválido daba 401 en vez de 400** (backend). Spring reenviaba el error a
  `/error`, que está protegido. Solución: un `@ExceptionHandler(HttpMessageNotReadableException)`
  en `GlobalExceptionHandler` que responde 400.
- **`2,50 €` salía como `€2.50`**: el idioma por defecto es inglés. Solución: registrar el
  español con `registerLocaleData(localeEs)` y `{ provide: LOCALE_ID, useValue: 'es-ES' }`.
- **El `ng build` avisa de que el bundle inicial supera los 500 kB.** Es un aviso, no un error;
  se puede ajustar el presupuesto en `angular.json` o cargar las pantallas bajo demanda.
- **`ng test` se queda esperando cambios**: añadir `--watch=false` para que ejecute y termine.

## Preguntas de repaso (para entrevistas)

1. ¿Qué es un signal y en qué se diferencia de una variable normal?
2. ¿Qué pasa si hago `products.list()` y no llamo a `subscribe`?
3. ¿Para qué sirve el `authInterceptor`? ¿Y el `authGuard`?
4. ¿Por qué el formulario valida si el backend ya valida?
5. ¿Para qué sirve el `track` en un `@for`?
6. ¿Cómo comprueba un test que un componente hizo la petición correcta sin tener backend?
7. ¿Por qué los números del formulario empiezan en `null` y no en `0`?
8. ¿Qué es un tipo unión y qué ventaja tiene sobre un `string` normal?
