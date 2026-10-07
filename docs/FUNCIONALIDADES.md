# Funcionalidades

> **Estado:** cerrado (29/09/2026). Idea y decisiones de modelo confirmadas por el usuario,
> listo para pasar a implementación (Hito 1 del plan).

## Idea general

La app gira en torno a tres apartados y a moverse entre ellos con comodidad: **Productos**
(el catálogo del que se parte), **Despensa** (lo que hay en casa) y **Compra** (lo que falta
por comprar). El objetivo de diseño es la sencillez: el usuario tiene que poder pasar algo de
un apartado a otro sin fricción, y saber en todo momento qué tiene y qué le falta.
**No es un checkout**: la app no compra ni paga nada online, solo planifica — la compra se
hace presencialmente en la tienda.

## 1. Productos

- Catálogo de productos identificados **por supermercado** (Mercadona, Alcampo, Ahorramas —
  ver [ARQUITECTURA.md](ARQUITECTURA.md#3-integraciones-con-supermercados)).
- Cada producto muestra: nombre, descripción, marca, **precio** (total y por
  kg/litro/unidad), **cuánto trae el envase** (pack de 12, pack de 6, suelto, por kg, por
  litro...), **ingredientes** (composición) y **valores nutricionales**.
- **Objetivo central:** poder comparar entre tiendas/marcas por **precio, ingredientes y
  cantidad que trae el envase** — ej. buscar "leche" y ver varias opciones una al lado de
  otra con su precio por litro, su composición y si es botella de 1L o pack de 6, para
  decidir cuál añadir.
- Desde la ficha de un producto, se manda directamente a **Despensa** o a **Compra**.
- **Entrada manual**: si un producto no aparece en la búsqueda, o el usuario prefiere
  introducirlo a mano, se puede dar de alta sin pasar por ninguna API.

## 2. Despensa

- Lo que el usuario tiene en casa ahora mismo, con su **cantidad** — contada en la unidad
  base del producto (huevos, kg, litros...), no en envases: un "pack de 12 huevos" comprado
  se consume huevo a huevo, así que Despensa cuenta huevos sueltos aunque se compraran en pack.
- Las cantidades se pueden **sumar o restar** libremente (ej. "me he comido 1 yogur" → resta 1).
- Al comprar algo (ver apartado 3), Despensa se actualiza automáticamente convirtiendo
  envases a unidad base (2 packs de 12 huevos comprados → +24 huevos en Despensa).
- **Aviso de stock bajo**: cuando la cantidad de un producto llega al umbral, la app avisa
  para añadirlo a Compra. El umbral es **una configuración global del usuario** (ej. "avisar
  cuando quede ≤ 1"), con la posibilidad de fijar uno propio en un producto concreto.
- **Consumo programado**: para no restar a mano cada día, se programa cuánto se gasta de un
  producto (ej. 2 huevos al día) y la app lo resta sola. Si un día no se consume, se edita la
  cantidad a mano. Cuando el producto llega al **umbral de stock bajo** (antes de acabarse), **pasa solo a Compra** (1 envase) con un aviso.
- Cada producto muestra **todos sus ingredientes** (y valores nutricionales) directamente en
  Despensa y en Compra.
- **Categorías** (como en los supermercados): Alimentación → Seco · Congelado · Fresco · Fruta;
  y Droguería (todo lo demás).

## 3. Compra

- El usuario tiene **una lista permanente por defecto** (su "carrito" habitual) y puede crear
  **listas adicionales con nombre** para compras puntuales (ej. una fiesta).
- Se llena moviendo productos desde **Despensa** (porque se están acabando), directamente
  desde **Productos**, o por entrada manual.
- Aquí la cantidad de cada item se cuenta en **envases a comprar** (ej. "2 packs de 12
  huevos", "3 botellas de leche") — es lo que tiene sentido al hacer la compra.
- Muestra el **precio total estimado** sumando todos los productos (precio del envase ×
  número de envases).
- Cada item tiene un botón **"A Despensa"** (un clic): lo suma a **Despensa**, lo registra en
  el historial de compras y lo quita de la lista. La casilla de marcado es solo visual. En
  Despensa hay botones **+ / −** y **"A Compra"** (añade 1 envase) para moverse sin fricción.
- Los items muestran el desglose del envase ("1 paquete (12 unidades)").
- Al pie de la lista se ve el **total estimado**, que alimentará el módulo de **estadísticas**
  (gasto mensual y por categoría: carne, pescado, congelados, droguería...).
- **Descargar la lista en PDF**, para no depender de tener cobertura/datos dentro del
  supermercado.

## Flujo resumen

```
Productos ──► Despensa ──► (queda poco, aviso) ──► Compra ──(comprado)──► vuelve a Despensa
     │
     └──────────────────────► Compra directamente
```

## Fuera de alcance (de momento)

- Integración con Carrefour y DIA (bloqueados por protección anti-bot — Cloudflare y Akamai
  respectivamente — a cualquier petición que no venga de un navegador real).
- Integración con LIDL (sin bloqueo, pero no tiene catálogo de alimentación online en España
  con el que trabajar — solo folletos semanales, sin ficha de producto ni precio individual).
- **Comprar o pagar dentro de la app** — es solo planificación, la compra se hace en persona.
- Listas compartidas entre varios usuarios.
- Notificaciones push de verdad (requeriría convertir el frontend en PWA) — de momento el
  aviso de stock bajo es un indicador dentro de la app.
- ~~Historial/estadísticas de gasto~~ → ahora **sí** previsto (fase v2); el historial se guarda
  desde el MVP. Ver [REQUISITOS.md](REQUISITOS.md).

## Decisiones confirmadas (antes eran preguntas abiertas)

1. **Compra = lista permanente por defecto + listas adicionales con nombre** para casos
   puntuales. Existe la entidad `ShoppingList`.
2. ~~Al marcar como comprado se devolvía automáticamente a Despensa~~ → (07/10/2026) ahora es
   un **botón "A Despensa"** explícito; después el ajuste es manual.
3. La descarga de la lista es en **PDF**.
4. El umbral de aviso de stock bajo es **una configuración global del usuario**, no por
   producto.
