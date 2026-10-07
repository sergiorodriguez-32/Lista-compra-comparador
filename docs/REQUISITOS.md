# Requisitos

> **Estado:** cerrado el 07/10/2026 (decisiones finales en la sección 6).
> Complementa a [FUNCIONALIDADES.md](FUNCIONALIDADES.md) (el qué, en lenguaje natural) y a
> [ARQUITECTURA.md](ARQUITECTURA.md) (el cómo). Aquí está el detalle verificable: requisitos
> numerados, reglas de negocio, prioridades y criterios de aceptación.

Prioridad: **M** = debe (MVP) · **S** = debería · **C** = podría (después del MVP).

---

## 1. Requisitos funcionales

### Cuenta y ajustes
| ID | Requisito | Prio |
|---|---|---|
| RF-01 | El usuario se registra con email, contraseña y nombre. Al registrarse se crea su lista de Compra por defecto ("Mi compra"). | M |
| RF-02 | El usuario inicia sesión y recibe un JWT con caducidad. | M |
| RF-03 | El usuario edita su umbral global de stock bajo. | S |
| RF-04 | Cada usuario solo ve y modifica **sus propios** datos (Despensa, listas, productos manuales, historial). | M |

### Productos
| ID | Requisito | Prio |
|---|---|---|
| RF-10 | Buscar productos por texto en Mercadona. | M |
| RF-11 | Buscar también en Alcampo. | S |
| RF-12 | Buscar también en Ahorramas (scraping). | C |
| RF-13 | Si una tienda falla, la búsqueda devuelve las demás e indica cuál ha fallado. | S |
| RF-14 | Cada resultado muestra nombre, marca, precio del envase, precio por kg/litro/unidad, contenido del envase (ej. "pack de 12 unidades"), ingredientes y valores nutricionales cuando existan. | M |
| RF-15 | Dar de alta un producto manual (nombre, marca, precio, formato, categoría). Es **privado del usuario**. | M |
| RF-16 | Comparar varios resultados lado a lado por precio unitario, ingredientes y formato. | C |
| RF-17 | Desde un producto, botón **"A Despensa"** y botón **"A Compra"** (un clic). | M |
| RF-18 | Cada producto tiene una **categoría** de un listado simple y fijo (ver RN-14): Seco, Congelado, Fresco, Fruta o Droguería. En productos de tienda se deduce de la categoría de la tienda; en manuales la elige el usuario. | S |
| RF-19 | Los **ingredientes** y valores nutricionales se guardan con el producto al añadirlo, y se muestran siempre que existan (ver RF-26 y RF-39). Si no hay dato, se indica "Ingredientes no disponibles". | M |

### Despensa
| ID | Requisito | Prio |
|---|---|---|
| RF-20 | Ver la Despensa: producto, marca y cantidad en unidad base. | M |
| RF-21 | Botones **+** y **−** para sumar/restar cantidad en un clic, y edición directa del número. | M |
| RF-22 | Botón **"A Compra"** en cada item: lo añade a la lista elegida (por defecto, la lista por defecto) con **1 envase**. | M |
| RF-23 | Eliminar un item de Despensa. | M |
| RF-24 | Aviso visual de stock bajo cuando `cantidad <= umbral` (umbral del item si lo tiene; si no, el global). | S |
| RF-25 | El usuario puede fijar un umbral propio en un item concreto (opcional). | S |
| RF-26 | Cada item de Despensa muestra **todos sus ingredientes** (y valores nutricionales si existen), desplegables desde el propio item sin salir de la pantalla. | M |
| RF-27 | **Consumo programado:** el usuario programa en un item cuánto se gasta y con qué frecuencia (ej. 2 huevos cada día; opcionalmente solo ciertos días de la semana). La app resta esa cantidad automáticamente. | S |
| RF-28 | Un consumo programado se puede pausar, editar y eliminar. Pausarlo no borra la programación. | S |
| RF-29 | Cuando un consumo programado deja un item en **cantidad <= umbral** (RN-09), se añade **automáticamente** a la lista de Compra por defecto (1 envase) y el usuario ve un aviso en la app. El item sigue en Despensa con su cantidad y su programación. | S |

### Compra
| ID | Requisito | Prio |
|---|---|---|
| RF-30 | Existe siempre una lista por defecto. | M |
| RF-31 | Crear, renombrar y borrar listas adicionales con nombre (la lista por defecto no se borra). | C |
| RF-32 | Añadir items desde Productos, desde Despensa o a mano; cantidad en **envases**. | M |
| RF-33 | Cada item muestra el desglose del envase: "1 paquete (12 unidades)", "2 botellas (1 L)". | M |
| RF-34 | Marcar un item con casilla (solo visual: "ya lo tengo en el carro"). | S |
| RF-35 | Botón **"A Despensa"** en cada item: suma a Despensa, registra la compra en el historial y quita el item de la lista. | M |
| RF-36 | En la parte inferior de la lista se muestra el **total estimado**: suma de `precio × envases` de todos los items. | M |
| RF-37 | Descargar la lista en PDF. | S |
| RF-38 | Refrescar precios de una lista bajo demanda (consulta a las tiendas); nunca cambian solos. | C |
| RF-39 | Cada item de Compra muestra **todos sus ingredientes** (y valores nutricionales si existen), desplegables desde el propio item. En items manuales rápidos sin producto asociado se muestra "Ingredientes no disponibles". | M |

### Estadísticas (fase posterior)
| ID | Requisito | Prio |
|---|---|---|
| RF-40 | Gasto aproximado por mes, a partir del historial de compras (RF-35). | C |
| RF-41 | Desglose del gasto por categoría (seco, congelado, fresco, fruta, droguería) y total de alimentación frente a droguería. | C |
| RF-42 | Ranking de productos en los que más se gasta. | C |

> RF-35 ya guarda el historial desde el MVP aunque la pantalla de estadísticas llegue después,
> para no perder datos de los primeros meses.

---

## 2. Reglas de negocio

| ID | Regla |
|---|---|
| RN-01 | **Cantidades.** Despensa cuenta en **unidad base** (unidades, kg, litros). Compra cuenta en **envases**. |
| RN-02 | **Conversión al pasar a Despensa:** `cantidad_despensa += envases × contenido_del_envase`. Ej.: 2 packs de 12 huevos → +24. |
| RN-03 | **Sin duplicados en Despensa.** Si el usuario ya tiene el mismo producto, se **suma** a ese item. "Mismo producto" = misma **marca** + mismo **nombre** (normalizados: sin mayúsculas ni tildes) + misma unidad base. Misma marca y distinto producto, o distinta marca, **conviven** como items separados (leche entera Pascual y leche entera Alipende son dos items). |
| RN-04 | RN-03 es independiente de la tienda: la misma marca y producto comprado en Mercadona y en Alcampo se suma en un único item. |
| RN-05 | **Despensa → Compra** añade **1 envase** por defecto, editable. Si el producto ya está en esa lista, suma 1 al item existente. |
| RN-06 | **Compra → Despensa** (botón) es una sola acción: suma, registra historial, quita el item. No hay ajuste manual implícito; después el usuario ajusta la Despensa a mano cuando quiera. |
| RN-07 | **Precios.** El precio viene de la tienda en el momento de buscar/añadir y se **copia** al item de Compra y al historial. No cambia solo; el historial conserva el precio pagado aunque luego cambie. |
| RN-08 | **Total estimado** = suma de `precio_copiado × envases` de los items de la lista. |
| RN-09 | **Umbral de stock bajo:** el del item si existe; si no, el global del usuario. Se compara contra la cantidad en unidad base. |
| RN-10 | **Productos de tienda** son compartidos y se guardan en BD solo al enviarlos a Despensa o a Compra (se reutilizan por `source + externalId`). **Productos manuales** pertenecen a un usuario. |
| RN-11 | **Packs.** Un pack se guarda como `packageAmount = N` y `packageUnit = UNIDAD` (pack de 12 latas → 12 UNIDAD). En Compra se ve "1 pack (12 unidades)" y su precio; en Despensa entran las 12 unidades y se van restando. No hay lógica especial más allá de esto. |
| RN-12 | **Productos por peso o formato libre** (almendras, fruta a granel...): no se intenta deducir cómo los vende cada tienda. El producto se copia de la API y el usuario escribe la cantidad (y la unidad si hace falta) que quiera; o lo crea como producto personal (RF-15). |
| RN-13 | **La cantidad siempre es editable** (sumar, restar o escribir el valor) en todos los productos de Despensa y de Compra, con decimales. |
| RN-14 | **Categorías.** Cinco valores: **SECO** (tomate frito, pasta, conservas...), **CONGELADO** (gambas congeladas, helados...), **FRESCO** (yogures, pollo, ternera...), **FRUTA** (plátanos, manzanas...) y **DROGUERIA** (todo lo demás). Los cuatro primeros forman el grupo **Alimentación**. Es la organización habitual de los supermercados. |
| RN-15 | **Consumo programado.** Un proceso automático diario resta de cada item con programación activa la cantidad indicada. La cantidad **nunca baja de 0**. Si la app estuvo parada varios días, al arrancar aplica los días que faltaban (se guarda la fecha de la última aplicación). Cada día se aplica **una sola vez**. |
| RN-16 | **Paso automático a Compra.** Cuando un consumo programado deja un item con `cantidad <= umbral` (RN-09), se añade a la lista por defecto con 1 envase y se muestra el aviso: así se repone antes de que se acabe. Si el producto ya está en esa lista, **no se vuelve a añadir** (evita acumular un envase más cada día). Solo lo dispara el consumo programado; si el usuario edita a mano la cantidad, solo ve el aviso de stock bajo (RF-24). Umbral global por defecto: 1. |
| RN-17 | **Ajuste manual prevalece.** El usuario puede editar la cantidad en cualquier momento (ej. un día no se consumió); la programación continúa desde el nuevo valor sin recalcular el pasado. |
| RN-18 | **Ingredientes.** Se muestran todos los ingredientes tal cual los da la tienda, sin recortar. Se guardan en el `Product` al añadirlo (RN-10), así Despensa y Compra los muestran sin volver a consultar a la tienda. |

---

## 3. Requisitos no funcionales

| ID | Requisito |
|---|---|
| RNF-01 | Contraseñas con BCrypt. Nunca se devuelve el hash. |
| RNF-02 | JWT con caducidad (1 h). Sin refresh token en el MVP. |
| RNF-03 | Todos los endpoints de datos exigen autenticación y filtran por usuario (RF-04). Acceder a un recurso ajeno devuelve 404. |
| RNF-04 | Las tiendas se consultan **en paralelo** con timeout de ~5 s por tienda. Un fallo o timeout no afecta a las demás. |
| RNF-05 | Resultados de búsqueda en caché en memoria (Caffeine, TTL ~1 h). |
| RNF-06 | Validación de entrada (Bean Validation) y formato de error único en toda la API (código HTTP + mensaje). |
| RNF-07 | Tests unitarios de la lógica de negocio crítica: conversión envase → unidad base (RN-02), fusión en Despensa (RN-03), total estimado (RN-08), umbral (RN-09). |
| RNF-08 | Migraciones de BD con Flyway. Nada de `ddl-auto=update`. |
| RNF-09 | Interfaz usable en móvil (se usará en el supermercado). |
| RNF-10 | Uso personal/educativo de las integraciones; sin scraping masivo ni redistribución de datos. |
| RNF-11 | El consumo programado se ejecuta con una tarea programada (`@Scheduled`) diaria, idempotente (RN-15) y con test de los casos límite: cruzar el umbral, llegar a 0, varios días sin ejecutar, programación pausada, item ya en la lista de Compra. |
| RNF-12 | Los ingredientes se muestran completos y legibles (también en móvil), con desplegable si son largos. |

---

## 4. Modelo de datos: cambios sobre ARQUITECTURA.md

- `Product.owner → User` (nullable). `null` = producto de tienda compartido.
- `Product.category` (enum: `SECO | CONGELADO | FRESCO | FRUTA | DROGUERIA`) — necesario para estadísticas.
- `PantryItem.lowStockThreshold` (nullable) — sobrescribe el global.
- Nueva entidad **`ConsumptionPlan`**: `id, pantryItem, amountPerDay (o por ocasión), daysOfWeek (nullable = todos), active, lastAppliedOn`. Una por item de Despensa (MVP).
- Endpoints de consumo programado: `GET/PUT/DELETE /api/pantry/{id}/consumption-plan`.
- `PantryItem`: unicidad por `(owner, marca normalizada, nombre normalizado, unidad base)`.
- Nueva entidad **`PurchaseRecord`** (historial): `id, owner, product (nullable), name, brand, category, packages, unitPriceAtPurchase, total, purchasedAt`. Se crea en RF-35.
- `ShoppingListItem.checked` pasa a ser solo visual; ya no dispara el movimiento a Despensa.
- Endpoint `POST /api/shopping-lists/{id}/items/{itemId}/mark-bought` pasa a llamarse `.../move-to-pantry` (es un botón explícito).
- Endpoints nuevos: `PATCH /api/pantry/{id}/adjust?delta=-1`, `GET /api/stats/monthly`, `GET /api/stats/by-category?month=`.

---

## 5. Plan por fases

| Fase | Contenido | Prioridad |
|---|---|---|
| **MVP** | Auth, Despensa, Compra con lista por defecto, productos manuales, búsqueda Mercadona, botones entre apartados, total estimado, historial | M |
| **v1** | Alcampo, stock bajo (global + por item), **consumo programado y paso automático a Compra**, PDF, categorías, caché y paralelismo | S |
| **v2** | Ahorramas, comparador lado a lado, listas con nombre, refrescar precios, estadísticas | C |

---

## 6. Decisiones cerradas sobre packs, nombres y categorías (07/10/2026)

- **Packs y unidades:** sin complicarse. Se distingue sobre todo el pack (refrescos, cervezas...)
  como N unidades (RN-11). Lo que se vende por peso o formato raro se resuelve con cantidad
  editable (RN-13) y productos personales (RN-12).
- **Marcas y nombres:** conviven todos los productos distintos (hay miles de marcas). Solo se
  suman o restan automáticamente si son **idénticos en marca y nombre** (RN-03). No hay
  fusión manual ni emparejamiento inteligente.
- **Categorías:** simplificadas a cinco, como las organizan los supermercados (RN-14). Las
  categorías de cada tienda se mapean a ellas al implementar el provider; lo que no encaje
  como alimentación cae en Droguería. En productos manuales las elige el usuario.
- **Consumo programado (07/10/2026):** restar a mano es tedioso, así que se programa el gasto
  (ej. 2 huevos al día) y la app lo resta sola; al llegar al umbral de stock bajo pasa a Compra (RN-15, RN-16).
  El ajuste manual sigue siendo posible en todo momento (RN-17).
- **Ingredientes:** visibles completos en Despensa y en Compra (RF-26, RF-39), no solo al buscar.
- **Pendiente solo:** despliegue (ver ARQUITECTURA.md).
