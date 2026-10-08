# 1. Docker Compose y PostgreSQL

## Qué problema resuelve

Instalar PostgreSQL a mano en cada PC es incómodo y cada proyecto acaba pisando al otro.
Con Docker, la base de datos corre en un **contenedor** aislado, y un fichero
`docker-compose.yml` describe cómo levantarlo. Quien clone el repo ejecuta un comando y
tiene la misma base de datos.

- **Imagen**: la "plantilla" descargada (`postgres:16`).
- **Contenedor**: una instancia en marcha de esa imagen.
- **Volumen**: carpeta persistente fuera del contenedor, para no perder los datos al borrarlo.

## El fichero de este proyecto

```yaml
services:
  db:                              # nombre del servicio dentro de compose
    image: postgres:16             # imagen y versión
    container_name: listacompra-db # nombre del contenedor (único en tu PC)
    environment:                   # variables que lee la imagen la PRIMERA vez que arranca
      POSTGRES_DB: listacompracomparador
      POSTGRES_USER: listacompracomparador
      POSTGRES_PASSWORD: <contraseña de desarrollo>
    ports:
      - "5435:5432"                # puerto_de_mi_PC:puerto_del_contenedor
    volumes:
      - listacompra-data:/var/lib/postgresql/data  # datos persistentes

volumes:
  listacompra-data:                # declaración del volumen con nombre
```

### Por qué el puerto 5435
PostgreSQL escucha dentro del contenedor en el 5432. En mi PC ya hay otros contenedores de
otros proyectos usando el 5433 y el 5434, así que este usa el **5435**. Dos contenedores no
pueden publicar el mismo puerto de mi PC. Desde Spring Boot me conectaré a
`jdbc:postgresql://localhost:5435/listacompracomparador`.

## Comandos que uso

```bash
docker compose up -d      # crea y arranca en segundo plano (-d = detached)
docker compose ps         # estado de los contenedores de ESTE compose
docker compose logs db    # logs del servicio db (útil si no arranca)
docker compose stop       # para sin borrar nada
docker compose start      # vuelve a arrancar
docker compose down       # para y BORRA el contenedor (los datos siguen en el volumen)
docker compose down -v    # ¡ojo! también borra el volumen = pierdes los datos
```

Entrar a la base de datos desde dentro del contenedor:

```bash
docker exec -it listacompra-db psql -U listacompracomparador -d listacompracomparador
```

Comandos útiles dentro de `psql`:

| Comando | Qué hace |
|---|---|
| `\l` | lista las bases de datos |
| `\dt` | lista las tablas |
| `\d nombre_tabla` | describe una tabla (columnas, tipos, claves) |
| `\q` | salir |

## Ojo: las variables solo se usan la primera vez

`POSTGRES_DB`, `POSTGRES_USER` y `POSTGRES_PASSWORD` solo se aplican cuando el volumen está
vacío. Si cambio la contraseña en el yml después, **no cambia** (porque la base ya existe en
el volumen). Para empezar de cero: `docker compose down -v` y volver a `up -d`.

## Seguridad: la contraseña

Ahora está escrita en el yml solo porque es una base local de desarrollo. Más adelante la
moveré a un fichero `.env` (ya está en el `.gitignore`) y el compose la leerá con
`${POSTGRES_PASSWORD}`. Regla general: **nunca subir contraseñas reales a Git**.
