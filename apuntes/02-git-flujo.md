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

## Comandos usados hasta ahora (referencia rápida)

| Comando | Qué hace |
|---|---|
| `git status` | muestra ficheros modificados, nuevos y en staging |
| `git status -sb` | lo mismo en formato corto, con la rama y si va por delante/detrás del remoto |
| `git branch` | lista las ramas locales (`-a` también las remotas) |
| `git branch --show-current` | nombre de la rama actual |
| `git checkout -b <rama>` | crea una rama nueva y cambia a ella |
| `git checkout <rama>` | cambia a una rama existente |
| `git add <ruta>` | prepara (staging) ese fichero o carpeta para el commit |
| `git commit -m "tipo: mensaje"` | guarda los cambios preparados en un commit |
| `git push -u origin <rama>` | sube la rama a GitHub y la enlaza (`-u` solo la primera vez; luego basta `git push`) |
| `git fetch origin` | descarga novedades del remoto **sin** tocar tu código (solo actualiza `origin/...`) |
| `git pull` | `fetch` + integrar los cambios en la rama actual |
| `git branch -d <rama>` | borra una rama local **solo si ya está mergeada** (seguro) |
| `git log --oneline -5` | últimos 5 commits en una línea cada uno |

### Secuencia real de este proyecto

```bash
# 1. rama nueva desde main
git checkout -b chore/docker-postgres
# 2. trabajo + commit + subida
git add docker-compose.yml apuntes
git commit -m "chore: añadir docker-compose con PostgreSQL y apuntes iniciales"
git push -u origin chore/docker-postgres
# 3. en GitHub: Pull Request -> Merge
# 4. volver a main y limpiar
git checkout main
git pull
git branch -d chore/docker-postgres
# 5. siguiente rama
git checkout -b feature/backend-esqueleto
```

Regla: **antes de crear una rama nueva, `main` actualizada** (`git checkout main` + `git pull`),
para no partir de código desfasado.

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
