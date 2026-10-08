# Problemas encontrados y solución

Log continuo: cada vez que me atasco con algo, lo apunto aquí con la causa y la solución.

## Puerto de PostgreSQL ocupado por otros contenedores

- **Síntoma esperado:** `docker compose up` falla con "port is already allocated", o la app
  se conecta a la base de datos de otro proyecto sin darme cuenta.
- **Causa:** otros proyectos (`barberia-db` en el 5433, `barberia-java-db` en el 5434)
  ya publicaban esos puertos en mi PC.
- **Solución:** elegir un puerto libre distinto por proyecto (aquí `5435:5432`).
- **Cómo comprobarlo:** `docker ps --format "table {{.Names}}\t{{.Ports}}"`.
