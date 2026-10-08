# 2. Flujo de Git: ramas, commits y Pull Requests

Seguimos **GitHub Flow**: `main` siempre está estable y no se trabaja directamente en ella.
Cada cambio va en una rama corta, se sube, se abre un Pull Request (PR) y se mergea.

## Ramas

Formato `tipo/descripcion-corta`:

| Prefijo | Para qué |
|---|---|
| `feature/` | funcionalidad nueva |
| `fix/` | corregir un error |
| `chore/` | infraestructura, configuración, tareas sin lógica de negocio |
| `docs/` | solo documentación |

```bash
git checkout -b chore/docker-postgres   # crear y cambiar a la rama
git branch --show-current               # ver en qué rama estoy
```

## Conventional Commits

Formato: `tipo: descripción en minúscula y en imperativo`.

```
feat: crear entidad User con migración Flyway
fix: corregir cálculo del total estimado
chore: añadir docker-compose con PostgreSQL
docs: añadir apuntes de Docker
refactor: extraer lógica de conversión a servicio
test: añadir tests de fusión en despensa
```

Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`. Un commit = un cambio con
sentido completo (commits pequeños y claros).

## Ciclo completo

```bash
git status                          # ¿qué ha cambiado?
git add docker-compose.yml          # preparo ficheros concretos (staging)
git commit -m "chore: añadir docker-compose con PostgreSQL"
git push -u origin chore/docker-postgres   # primer push de la rama (-u la enlaza con origin)
```

Después, en GitHub: **Compare & pull request** → base `main` ← compare tu rama → crear PR →
revisar los cambios → **Merge** → borrar la rama. Y en local:

```bash
git checkout main
git pull                             # traer el merge
git branch -d chore/docker-postgres  # borrar la rama local ya mergeada
```

## Comandos de apoyo

```bash
git log --oneline -5      # últimos 5 commits resumidos
git diff                  # cambios sin preparar
git diff --staged         # cambios ya preparados con add
git restore <fichero>     # descartar cambios de un fichero (¡no se pueden recuperar!)
```

## Qué no se commitea

Lo que está en `.gitignore`: `target/`, `node_modules/`, `.idea/`, `.env`... Ficheros
generados, del IDE o con secretos.
