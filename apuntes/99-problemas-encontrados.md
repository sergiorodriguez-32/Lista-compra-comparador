# Problemas encontrados y solución

Log continuo: cada vez que me atasco con algo, lo apunto aquí con la causa y la solución.

## Contraseña subida a Git por error

- **Síntoma:** `docker-compose.yml` con `POSTGRES_PASSWORD` en claro llegó a `main` (PR #2).
- **Solución:** mover las credenciales a `.env` (ignorado) + `.env.example`, **cambiar la
  contraseña** (la antigua queda en el historial, así que se da por comprometida) y recrear
  el volumen con `docker compose down -v`.
- **Prevención:** antes de cada commit, `git status` y mirar qué ficheros entran; comprobar
  contenido sensible con `git diff --staged`.

## No me sale el Pull Request en GitHub

- **Síntoma:** no aparece el botón "Compare & pull request".
- **Causa:** la rama solo existía en local; no había commits ni `git push` (cambié de rama
  llevándome cambios sin commitear).
- **Solución:** `git add` + `git commit` + `git push -u origin <rama>`. Comprobar que la rama
  existe en GitHub con `git ls-remote --heads origin`.
- **Prevención:** commitear antes de cambiar de rama; `git status` y `git branch` a menudo.

## `password authentication failed for user "${DB_USER}"`

- **Síntoma:** la app no arranca; el error muestra el usuario con las llaves, literalmente
  `"${DB_USER}"`.
- **Causa:** la variable de entorno `DB_USER` no existe en la configuración desde la que
  ejecuté la app. Spring, si no encuentra el valor del placeholder `${...}`, no da error:
  deja el texto tal cual y lo usa como usuario.
- **Solución:** definir `DB_USER` y `DB_PASSWORD` en *Run → Edit Configurations →
  ListacompraApplication → Environment variables*, y ejecutar **esa** configuración.
- **Pista general:** si en un error aparece `${...}` sin sustituir, el problema es siempre
  que esa variable/propiedad no está definida.

## Puerto de PostgreSQL ocupado por otros contenedores

- **Síntoma esperado:** `docker compose up` falla con "port is already allocated", o la app
  se conecta a la base de datos de otro proyecto sin darme cuenta.
- **Causa:** otros proyectos (`barberia-db` en el 5433, `barberia-java-db` en el 5434)
  ya publicaban esos puertos en mi PC.
- **Solución:** elegir un puerto libre distinto por proyecto (aquí `5435:5432`).
- **Cómo comprobarlo:** `docker ps --format "table {{.Names}}\t{{.Ports}}"`.
