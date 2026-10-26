# Cómo está montada la app

Resumen técnico del comparador de listas de la compra. Sirve para ponerse al día de golpe
(por ejemplo, al empezar una conversación nueva con Claude: *"lee docs/ARQUITECTURA.md,
docs/FUNCIONALIDADES.md y docs/REQUISITOS.md"*).

> **Estado:** idea y modelo de datos cerrados (29/09/2026); requisitos cerrados el 07/10/2026
> (ver [REQUISITOS.md](REQUISITOS.md), que modifica el modelo y las rutas de abajo). Listo para
> empezar el Hito 1 del plan (esqueleto de backend y frontend).

---

## 1. Qué es

Reescritura desde cero (sin generador) de un proyecto de prácticas hecho con JHipster
(`listacompra-melit`). La idea original era una lista de la compra genérica; el giro que le
da valor real y de portfolio es que la app entera gira en torno a **tres apartados**:

- **Productos** — catálogo de productos identificados por supermercado (Mercadona, Alcampo,
  Ahorramas), con marca, descripción, precio y valores nutricionales, más la opción de
  entrada manual. Es el punto de partida: desde aquí se manda un producto a Despensa o a
  Compra.
- **Despensa** — lo que el usuario tiene en casa ahora mismo, con su cantidad (que se puede
  sumar o restar libremente). Cuando queda poco de algo, la app avisa para añadirlo a Compra.
- **Compra** — lo que hay que comprar. El usuario tiene una lista "permanente" por defecto y
  puede crear listas adicionales con nombre (ej. una compra puntual para una fiesta). Se
  llena moviendo productos desde Despensa o directamente desde Productos, suma el precio
  total estimado, y se puede **descargar en PDF** para tenerla a mano sin cobertura en el
  supermercado.

**No es un checkout**: la app no compra ni paga nada por el usuario — solo planifica. La
compra real se hace presencialmente en la tienda.

La idea de diseño es la **sencillez**: mover un producto entre los tres apartados tiene que
ser cómodo, para saber en todo momento qué hay en casa y qué falta por comprar.

## 2. Las piezas

```
Navegador
    │
    ▼
Angular (standalone + signals)  ──HTTPS──►  Spring Boot (Java 21)  ──►  PostgreSQL
                                                   │
                                                   ├──► API Mercadona (JSON)
                                                   ├──► API Alcampo (JSON)
                                                   └──► Scraping Ahorramas (Jsoup)
```

| Parte | Tecnología | Dónde vive |
|---|---|---|
| Frontend | Angular 18+, standalone components, signals, Angular Material | `frontend/` |
| Backend | Spring Boot 3, Java 21, Maven, Spring Security + JWT (hecho a mano) | `backend/` |
| Base de datos | PostgreSQL 16, vía Docker Compose en desarrollo, JPA + Flyway | `docker-compose.yml` (raíz) |
| Generación de PDF | Biblioteca por decidir al implementar la exportación (OpenPDF / Apache PDFBox) | `backend/` |
| Despliegue | **Pendiente de decidir** | — |

## 3. Integraciones con supermercados

Investigado y confirmado con peticiones HTTP reales (29/09/2026):

| Tienda | Acceso | Notas |
|---|---|---|
| Mercadona | API JSON en `tienda.mercadona.es/api/...` | Sin bloqueo, sin auth. Ej. `/api/categories/`. |
| Alcampo | API JSON en `compraonline.alcampo.es/api/...` | Sin bloqueo. `/api/webproductpagews/v5/product-pages` y `/v6/products` devuelven precio, promociones, stock e imágenes. |
| Ahorramas | Sin API, HTML renderizado en servidor | Sin bloqueo. Búsqueda real en `ahorramas.com/buscador?q=<término>`; hay que parsear el HTML con Jsoup. |
| Carrefour | — | Cloudflare devuelve 403 ("Just a moment") a cualquier petición no-navegador. Fuera de alcance. |
| DIA | — | Akamai Bot Manager devuelve 403 "Access Denied" a cualquier petición no-navegador, incluso en la portada. Con navegador carga bien, pero igual que Carrefour requeriría un headless browser. Fuera de alcance (investigado 30/09/2026). |
| LIDL | — | Sin bloqueo anti-bot, pero `lidl.es` no es una tienda de alimentación online real en España: es venta general (bricolaje, hogar, moda...) con las ofertas de alimentación en formato folleto semanal, sin catálogo buscable de productos con precio individual. No hay dato que consumir. Fuera de alcance (investigado 30/09/2026). |

Patrón de integración — interfaz común para que el resto de la app no sepa si el dato viene
de una API JSON o de scraping:

```java
public record PriceQuote(String store, String externalId, String productName, String brand,
                          BigDecimal price, String unitPrice, String imageUrl,
                          String productUrl, String ingredients,
                          NutritionalInfo nutritionalInfo) {}

public interface ProductProvider {
    String storeId(); // "mercadona" | "alcampo" | "ahorramas"
    List<PriceQuote> search(String query);
}
```

Cada provider (`MercadonaProvider`, `AlcampoProvider`, `AhorramasProvider`) falla de forma
aislada: si Ahorramas cambia su HTML y el parser se rompe, Mercadona y Alcampo tienen que
seguir funcionando. Resultados de búsqueda cacheados en memoria (Caffeine, TTL ~1h) para no
golpear las tiendas en cada búsqueda.

**Pendiente de comprobar:** si las respuestas de producto individual de Mercadona/Alcampo
incluyen **ingredientes** y **valores nutricionales**, o si solo dan nombre/marca/precio (se
revisa al implementar cada provider). Es un dato importante: el objetivo declarado de
"Productos" es poder comparar no solo por precio, sino también por ingredientes.

**Aviso:** ninguna de estas integraciones es una API oficial ni documentada — son los mismos
endpoints/HTML que usan sus propias webs. Pueden cambiar sin aviso. Uso personal/educativo,
sin redistribuir datos ni hacer scraping masivo.

## 4. Estructura del código

```
lista-compra-comparador/
├── backend/                    (Spring Boot, Maven) — pendiente de crear
│   └── src/main/java/com/sergiorodriguez/listacompra/
│       ├── core/                 dominio: entidades (User, Product, PantryItem, ShoppingList,
│       │   │                     ShoppingListItem), sus repositorios y servicios (UserService...)
│       │   └── exception/          excepciones de negocio (EmailAlreadyRegisteredException...)
│       ├── config/               beans de configuración (PasswordConfig...)
│       ├── security/             Spring Security + JWT
│       ├── providers/            MercadonaProvider, AlcampoProvider, AhorramasProvider
│       ├── export/               generación del PDF de la lista de la compra
│       └── web/                  controladores REST, DTOs y GlobalExceptionHandler
├── frontend/                   (Angular CLI) — pendiente de crear
├── docs/                       ARQUITECTURA.md · FUNCIONALIDADES.md · REQUISITOS.md
├── docker-compose.yml           PostgreSQL de desarrollo — pendiente de crear
└── README.md                    pendiente (se escribe en la fase de pulido)
```

## 5. Modelo de datos (cerrado)

```
User
- id, email, passwordHash, displayName
- lowStockThreshold   (umbral GLOBAL de aviso de stock bajo, ej. 1 → avisa cuando
                        quantity <= 1 sea cual sea la unidad del producto)

Product
- id, source (MERCADONA | ALCAMPO | AHORRAMAS | MANUAL), externalId (nullable)
- name, brand, description, imageUrl, productUrl
- price                    (precio del envase tal como se vende)
- unitPrice                (precio por KG/LITRO/UNIDAD — para comparar de forma justa
                             aunque el formato de envase sea distinto entre tiendas)
- packageAmount            (cuánto trae el envase: 12, 6, 1, 2, 1.5...)
- packageUnit              (UNIDAD | KG | LITRO | GRAMO | ML)
  Ejemplos: packageAmount=12, packageUnit=UNIDAD → "pack de 12 huevos".
            packageAmount=1, packageUnit=LITRO → "botella de 1L".
            packageAmount=6, packageUnit=UNIDAD → "pack de 6 yogures".
- ingredients              (texto libre, la lista de composición tal cual la da la tienda o el usuario)
- nutritionalInfo          (kcal, proteínas, grasas, hidratos de carbono, azúcares, sal — por 100g/100ml)
  (`ingredients` y `nutritionalInfo` nullable, no siempre estarán disponibles)

PantryItem  (Despensa)
- id, owner→User, product→Product
- quantity   (en la UNIDAD BASE del producto — `Product.packageUnit` — no en envases. Ej. un
              "pack de 12 huevos" se consume huevo a huevo, no pack a pack: quantity=18
              significa 18 huevos, sea cual sea el pack en el que se compraron. Así el sumar/
              restar día a día (usarse 1 huevo, acabarse un yogur del pack...) tiene sentido.
              Para KG/LITRO ya es la unidad base de por sí)
(el aviso de stock bajo usa el umbral global de User sobre esta `quantity` en unidad base —
 ej. "avisar cuando queden ≤ 1 huevo / ≤ 1 kg / ≤ 1 litro" — no hay umbral por producto)

ShoppingList  (Compra)
- id, owner→User, name, isDefault (boolean)
  Al registrarse, se crea automáticamente una lista con isDefault=true ("Mi compra", el
  "carrito permanente"). El usuario puede crear listas adicionales con nombre para compras
  puntuales (ej. una fiesta), pero siempre hay una marcada por defecto.

ShoppingListItem
- id, list→ShoppingList, product→Product (nullable si es entrada manual rápida)
- name/price/quantity (copia propia, por si el producto cambia de precio o es manual)
- quantity   (aquí SÍ en número de ENVASES a comprar — ej. "2 packs de 12 huevos", "3
              botellas de leche". Es lo que tiene sentido al comprar y lo que multiplica al
              `price` para el total estimado de la lista)
- checked (comprado o no)
```

**Flujo de movimiento entre apartados:**
- `Producto → Despensa` o `Producto → Compra`: crea `PantryItem` o `ShoppingListItem` a partir
  de un `Product` (de catálogo o manual).
- `Despensa → Compra` (por poco stock o a petición): crea `ShoppingListItem` referenciando el
  mismo `Product`.
- `Compra → Despensa` (al marcar `checked = true`): se **crea o incrementa automáticamente**
  el `PantryItem` correspondiente, convirtiendo envases → unidad base:
  `PantryItem.quantity += ShoppingListItem.quantity × Product.packageAmount`
  (ej. comprar 2 packs de 12 huevos → +24 en la unidad base de Despensa). Esa acción termina
  ahí — no implica ningún ajuste manual en ese momento.
- **Independientemente de lo anterior**, el usuario puede editar la cantidad de cualquier
  `PantryItem` a mano **en cualquier momento** (sumar o restar), según lo vaya consumiendo.
  No es un paso "siguiente" al marcar como comprado, es una capacidad general de Despensa.

## 6. Rutas de la API

| Ruta | Qué hace |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Alta (crea también la lista por defecto) y login, devuelve JWT |
| `GET/PUT /api/users/me/settings` | Consultar/editar el umbral global de stock bajo |
| `GET /api/products/search?q=leche` | Busca en las 3 tiendas (Productos), agrupado por tienda |
| `POST /api/products/manual` | Da de alta un producto manual |
| `GET/POST /api/pantry`, `PATCH/DELETE /api/pantry/{id}` | CRUD de Despensa (incluye sumar/restar cantidad) |
| `GET /api/pantry/low-stock` | Items de Despensa por debajo del umbral global |
| `POST /api/pantry/{id}/move-to-shopping-list?listId=` | Mueve un item de Despensa a una lista de Compra |
| `GET/POST /api/shopping-lists`, `GET/PUT/DELETE /api/shopping-lists/{id}` | CRUD de listas de Compra (una de ellas `isDefault`) |
| `POST/PATCH/DELETE /api/shopping-lists/{id}/items/{itemId}` | CRUD de items de una lista de Compra |
| `POST /api/shopping-lists/{id}/items/{itemId}/mark-bought` | Marca comprado → mueve/suma automáticamente a Despensa |
| `GET /api/shopping-lists/{id}/export` | Descarga la lista de Compra en **PDF** |

## 7. Decisiones que conviene recordar

- **Sin JHipster.** Todo escrito a mano para aprender de verdad Angular y Spring Boot.
- **No es un checkout.** La app no compra ni paga nada online — solo planifica; la compra se
  hace presencialmente en la tienda.
- **Multi-usuario con JWT hecho a mano** (no generado), Spring Security.
- **PostgreSQL vía Docker Compose** en desarrollo, Flyway para migraciones.
- **Paquete base Java:** `com.sergiorodriguez.listacompra`.
- **Git:** GitHub Flow — `main` protegida, ramas `feature/*` · `fix/*` · `chore/*`, todo entra
  por Pull Request. Conventional Commits (`feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`).
- **Repo privado por ahora** (github.com/sergiorodriguez-32/Lista-compra-comparador); se hace
  público cuando el MVP esté presentable.
- **Modo de trabajo:** el usuario escribe y ejecuta todo él mismo (git, SQL, Java, Angular);
  Claude guía paso a paso, no scaffolda directamente salvo que se le pida explícitamente
  (como esta carpeta `docs/`).

## 8. Arrancar en local

Pendiente — se documenta aquí en cuanto exista el esqueleto de `backend/` y `frontend/`.

## 9. Publicar cambios

Pendiente de decidir despliegue (backend, frontend, base de datos en producción).

## 10. Pendiente

- [ ] Crear esqueleto del backend (Spring Initializr) — Hito 1 del plan.
- [ ] Crear esqueleto del frontend (Angular CLI) — Hito 1 del plan.
- [ ] `docker-compose.yml` con PostgreSQL.
- [ ] Comprobar si Mercadona/Alcampo devuelven valores nutricionales en el detalle de producto.
- [ ] Elegir librería de generación de PDF para la exportación de la lista de Compra.
- [ ] Decidir dónde se despliega (backend/frontend/BD) cuando el proyecto esté maduro.
