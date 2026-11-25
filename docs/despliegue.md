# Despliegue de ShopCloud

| Pieza | Servicio | Plan |
|---|---|---|
| Base de datos | [Neon](https://neon.tech) (PostgreSQL 17) | Gratis, no caduca |
| Backend (Spring Boot) | [Render](https://render.com), con Docker | Gratis |
| Frontend (Vite + React) | [Vercel](https://vercel.com) | Gratis |

**Orden:** base de datos → backend → frontend → actualizar CORS del backend.

---

## 1. Base de datos en Neon

1. Crea una cuenta en neon.tech y un proyecto nuevo con **PostgreSQL 17**. Elige la región más cercana a la de Render (por ejemplo, `US East (Ohio)` si en Render usas `Ohio`).
2. En **Connection Details** copia los datos de conexión. Neon da algo así:
   ```
   postgresql://usuario:clave@ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
3. Aplica los scripts desde tu PC (necesitas `psql`, que ya viene con tu PostgreSQL local). Usa la cadena de Neon **tal cual** (formato `postgresql://`):
   ```powershell
   $env:NEON_URL = "postgresql://usuario:clave@ep-xxxx.../neondb?sslmode=require"
   $psql = "C:\Program Files\PostgreSQL\17\bin\psql.exe"
   & $psql $env:NEON_URL -v ON_ERROR_STOP=1 -f database/01_shopcloud_postgresql.sql
   & $psql $env:NEON_URL -v ON_ERROR_STOP=1 -f database/02_suscripciones_prueba_gratuita.sql
   & $psql $env:NEON_URL -v ON_ERROR_STOP=1 -f database/03_datos_iniciales_planes.sql
   ```
   Los scripts 01 y 02 se aplican **una sola vez**. Si falla a mitad, borra las tablas (o crea una rama nueva en Neon) antes de reintentar.

> ¿Por qué no la base de datos de Render? Su PostgreSQL gratuito se borra a los 30 días. Neon no caduca; solo "duerme" tras unos minutos sin uso y despierta en la siguiente consulta.

## 2. Backend en Render

1. Sube los cambios a GitHub (rama `main`).
2. En Render: **New → Blueprint** y elige el repositorio. Render lee `render.yaml` y crea el servicio `shopcloud-backend` con Docker.
   (Alternativa manual: **New → Web Service**, Runtime **Docker**, Root Directory `backend`.)
3. Variables de entorno:

   | Variable | Valor |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require` (la de Neon, pero empezando por `jdbc:postgresql://` y **sin** usuario ni clave) |
   | `DB_USERNAME` | usuario de Neon |
   | `DB_PASSWORD` | clave de Neon |
   | `JWT_SECRET` | la genera Render. Si la pones tú: Base64 de al menos 32 bytes (`openssl rand -base64 48`) |
   | `CORS_ALLOWED_ORIGINS` | de momento `http://localhost:5173`; en el paso 4 pones la URL de Vercel |

4. Espera a que termine el build (la primera vez tarda unos minutos). Tendrás una URL como `https://shopcloud-backend.onrender.com`.

> El plan gratuito de Render apaga el servicio tras 15 minutos sin tráfico. La primera petición después tarda ~1 minuto en responder.

## 3. Frontend en Vercel

1. En Vercel: **Add New → Project** e importa el repositorio.
2. **Root Directory:** `frontend`. Vercel detecta Vite solo (build `npm run build`, salida `dist`).
3. Variable de entorno:

   | Variable | Valor |
   |---|---|
   | `VITE_API_URL` | `https://shopcloud-backend.onrender.com` (sin `/api` y sin barra final) |

4. **Deploy.** Tendrás una URL como `https://shopcloud.vercel.app`.

`frontend/vercel.json` redirige todas las rutas a `index.html`, para que recargar en una ruta como `/panel` no dé 404.

## 4. Conectar CORS

En Render, cambia `CORS_ALLOWED_ORIGINS` a la URL de Vercel y guarda (se redespliega solo):

```
https://shopcloud.vercel.app,http://localhost:5173
```

Admite varios orígenes separados por coma y patrones. Para aceptar también las URLs de vista previa de Vercel: `https://shopcloud-*.vercel.app`.

## Comprobar

- Abre la URL de Vercel, regístrate e inicia sesión.
- Si ves errores de CORS en la consola del navegador, revisa que `CORS_ALLOWED_ORIGINS` coincida exactamente con la URL (con `https://`, sin barra final).
- Si el backend no arranca, mira **Logs** en Render: `Schema-validation` significa que faltan tablas en Neon (paso 1.3).

## Probar la imagen en local (opcional)

```powershell
docker build -t shopcloud-backend backend
docker run --rm -p 8080:8080 --env-file backend/.env shopcloud-backend
```

Dentro del contenedor, `localhost` no es tu PC: si usas tu PostgreSQL local, pon `DB_URL=jdbc:postgresql://host.docker.internal:5432/shopcloud_test`.
