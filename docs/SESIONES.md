# Diario de sesiones

---

## Sesión 1 — 29/09/2026 · Diseño inicial y arranque del repo

### Decisiones tomadas
- Reescribir `listacompra-melit` (proyecto JHipster de prácticas) **desde cero, sin
  generador**, para aprender Angular y Spring Boot de verdad en vez de traducir la app de la
  barbería a otro stack.
- Darle valor real y de portfolio: las listas muestran **precios reales** de Mercadona,
  Alcampo y Ahorramas (las tiendas donde compra el usuario).
- **Multi-usuario con JWT** hecho a mano (no generado), Spring Security.
- **PostgreSQL** vía Docker Compose en desarrollo, JPA + Flyway.
- **Paquete base Java:** `com.sergiorodriguez.listacompra`.
- **Git:** GitHub Flow (`main` protegida + `feature/*`/`fix/*`/`chore/*` + PR) y Conventional
  Commits. Repo **privado** de momento, se hará público con el MVP presentable.
- **Modo de trabajo:** el usuario escribe y ejecuta él mismo todos los comandos (git, SQL,
  Java, Angular); Claude guía paso a paso salvo que se le pida hacer algo directamente.

### Idea completa de la app (explicada por el usuario)
La app gira en torno a tres apartados: **Productos** (catálogo por supermercado, con marca,
descripción, precio y valores nutricionales, más entrada manual), **Despensa** (lo que el
usuario tiene en casa, con cantidades que se suman/restan libremente) y **Compra** (lo que
falta por comprar, alimentado desde Despensa o desde Productos, con el precio total estimado).
Se quiere además: aviso cuando quede poco de algo en Despensa (ej. 1 unidad, 1 kg) para
añadirlo a Compra, y poder **descargar** la lista de Compra para no depender de tener
cobertura dentro del supermercado. La prioridad de diseño es la sencillez para moverse entre
los tres apartados. Detalle completo en [FUNCIONALIDADES.md](FUNCIONALIDADES.md); quedan
algunas decisiones de modelo de datos abiertas (ver [ARQUITECTURA.md](ARQUITECTURA.md#5-modelo-de-datos-cerrado)).

### Decisiones de modelo cerradas (respuestas a las preguntas abiertas)
1. Compra = **una lista permanente por defecto** + el usuario puede crear **listas
   adicionales con nombre** (ej. una compra puntual para una fiesta). Existe `ShoppingList`.
2. Al marcar un item como comprado, **se mueve/suma automáticamente a Despensa** — esa acción
   termina ahí, sin ajuste manual implícito. Por separado, el usuario puede editar cualquier
   cantidad de Despensa a mano **en cualquier momento** (es una capacidad general, no un paso
   "siguiente" al marcar como comprado).
3. Descarga de la lista de Compra: **PDF**.
4. Umbral de aviso de stock bajo: **configuración global del usuario** (no por producto).
5. Confirmado explícitamente: la app **no es un checkout** — no se compra ni se paga nada
   desde dentro, solo planifica; la compra se hace presencialmente en la tienda.

Modelo de datos y rutas de API ya reflejan esto en [ARQUITECTURA.md](ARQUITECTURA.md#5-modelo-de-datos-cerrado).

### Investigación hecha
Comprobado con peticiones HTTP reales qué tiendas son viables:
- **Mercadona** y **Alcampo**: APIs JSON internas, accesibles sin navegador ni autenticación.
- **Ahorramas**: sin API, pero HTML accesible sin bloqueos → scraping con Jsoup.
- **Carrefour**: descartado, Cloudflare bloquea peticiones no-navegador (403).

Detalle completo en [ARQUITECTURA.md](ARQUITECTURA.md#3-integraciones-con-supermercados).

### Hecho
1. `git init` + `.gitignore` (Java/Maven, Angular/Node, IDE, OS, secrets).
2. Repositorio creado en GitHub: https://github.com/sergiorodriguez-32/Lista-compra-comparador (privado).
3. Conectado el remoto (`origin`), rama renombrada a `main`, primer push.
4. Carpeta `docs/` con `ARQUITECTURA.md` y `FUNCIONALIDADES.md` (borrador, pendiente de
   completar con la idea detallada del usuario).

### Estado del entorno
- Repositorio: https://github.com/sergiorodriguez-32/Lista-compra-comparador (privado)
- Rama `main`, un commit (`chore: initial commit with gitignore`)
- Aún **no existe** `backend/` ni `frontend/` ni `docker-compose.yml`

### Pendiente para la próxima sesión
- [ ] Hito 1 del plan: esqueleto del backend (Spring Initializr) y del frontend (Angular CLI),
      cada uno en su propia rama `feature/` con su Pull Request.
- [ ] `docker-compose.yml` con PostgreSQL.
- [ ] Comprobar si Mercadona/Alcampo devuelven valores nutricionales en el detalle de producto.
- [ ] Elegir librería de generación de PDF para la exportación de la lista de Compra.

---

## Sesión 2 — 30/09/2026 · Investigación de DIA

### Investigación hecha
El usuario propuso añadir **DIA** al comparador. Comprobado con petición HTTP directa (sin
navegador): `www.dia.es` devuelve **403 Access Denied de Akamai** (`errors.edgesuite.net`)
incluso en la portada, y también en sus endpoints `/api/v2/...`. Con navegador sí carga bien
(hay productos y precios reales), así que el bloqueo es específicamente a peticiones que no
parecen venir de un navegador real — mismo tipo de protección que ya descartó a Carrefour
(ahí era Cloudflare, aquí es Akamai Bot Manager).

**Conclusión: DIA descartado**, por el mismo motivo que Carrefour — requeriría un navegador
headless para sortear la protección, coste de infraestructura que no compensa para este
proyecto. Detalle en [ARQUITECTURA.md](ARQUITECTURA.md#3-integraciones-con-supermercados).

El usuario también propuso **LIDL**. `lidl.es` no tiene ningún bloqueo anti-bot, pero su
"Compra Online" es venta general (bricolaje, hogar, moda...), no alimentación: las ofertas de
comida están en folletos semanales, sin catálogo buscable ni ficha de producto con precio
individual. **Descartado** por falta de dato aprovechable, no por bloqueo técnico.

### Refinamiento del modelo: envases vs. unidad base
El usuario insistió en que precio, **ingredientes** y **cantidad/formato del envase** (pack
de 12, pack de 6, suelto, por kg, por litro) son los tres criterios importantes para comparar
productos — se añadió `ingredients` y `packageAmount`/`packageUnit` a `Product` (antes solo
había un `unit` simple).

De ahí salió una distinción importante para que Despensa tenga sentido con el consumo real:
- **`ShoppingListItem.quantity`** = número de **envases** a comprar (2 packs de 12 huevos).
- **`PantryItem.quantity`** = cantidad en la **unidad base** del producto (huevos, kg, litros
  sueltos) — porque un pack de 12 huevos se consume huevo a huevo, no pack a pack.
- Al marcar como comprado: `PantryItem.quantity += ShoppingListItem.quantity × Product.packageAmount`.
- El aviso de stock bajo también es en unidad base (ej. "avisar cuando quede ≤ 1 huevo / ≤ 1 kg").

Detalle completo en [ARQUITECTURA.md](ARQUITECTURA.md#5-modelo-de-datos-cerrado).

### Pendiente para la próxima sesión
(sin cambios respecto a la Sesión 1 — sigue pendiente el Hito 1: esqueleto de backend y frontend)

---

## Sesión 3 — 07/10/2026 · Requisitos cerrados

### Decisiones tomadas
- Nuevo documento [REQUISITOS.md](REQUISITOS.md): requisitos funcionales (RF), reglas de
  negocio (RN), no funcionales (RNF), prioridades M/S/C y plan por fases (MVP, v1, v2).
- Productos manuales **privados por usuario** (`Product.owner`); productos de tienda compartidos
  y guardados en BD solo al enviarlos a Despensa/Compra.
- Umbral de stock bajo: global **más** opcional por item de Despensa.
- Despensa sin duplicados: se suma si coincide **marca + nombre + unidad base**; marcas
  distintas conviven (leche entera Pascual ≠ leche entera Alipende).
- Despensa → Compra añade **1 envase**; la lista muestra el desglose ("1 paquete (12 unidades)").
- Movimientos entre apartados con **botones de un clic** (A Despensa, A Compra, +/−), en vez de
  marcar como comprado → movimiento automático.
- Los precios vienen de las APIs y se copian al añadir; el historial guarda el precio pagado.
- **Módulo de estadísticas** (gasto mensual y por categoría) pasa de "fuera de alcance" a fase
  v2; el historial (`PurchaseRecord`) y `Product.category` se incluyen desde el MVP.

### Decisiones finales sobre packs, nombres y categorías
Ver sección 6 de [REQUISITOS.md](REQUISITOS.md): packs como N unidades, cantidad siempre editable,
suma solo si marca y nombre son idénticos.

### Funcionalidades añadidas al final de la sesión
- **Consumo programado:** se programa cuánto se gasta de un producto (ej. 2 huevos al día) y la
  app lo resta sola con una tarea diaria; cantidad nunca por debajo de 0, el ajuste manual
  prevalece. Al llegar al **umbral de stock bajo** (no a 0: mejor reponer antes de quedarse sin él) pasa solo a la lista de Compra por defecto (1 envase, sin duplicar).
  Nueva entidad `ConsumptionPlan` (RF-27 a RF-29, RN-15 a RN-17). Fase v1.
- **Categorías simplificadas** a cinco, como las organizan los supermercados: SECO, CONGELADO,
  FRESCO, FRUTA (las cuatro, grupo Alimentación) y DROGUERIA (todo lo demás). Sustituye a
  "tomar las categorías de las tiendas tal cual" (RN-14).
- **Ingredientes completos visibles en Despensa y en Compra**, no solo en la búsqueda
  (RF-26, RF-39, RN-18).

### Pendiente para la próxima sesión
- [ ] Hito 1: esqueleto de backend (Spring Initializr) y frontend (Angular CLI), cada uno en su
      rama `feature/` con su PR.
- [ ] `docker-compose.yml` con PostgreSQL.
