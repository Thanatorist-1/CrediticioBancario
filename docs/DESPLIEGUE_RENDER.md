# Despliegue en Render

El proyecto se publica como un solo servicio web: Spring Boot procesa las vistas Thymeleaf y las reglas del sistema; Supabase almacena los datos.

## Antes de crear el servicio

1. Sube al repositorio los cambios, incluido `render.yaml`.
2. En Render, selecciona **New +** > **Blueprint** y conecta el repositorio `Thanatorist-1/CrediticioBancario`.
3. Render detectará un servicio llamado `crediticio-bancario` y construirá el `Dockerfile` incluido en el repositorio.

## Variables de entorno

Durante la creación del Blueprint, ingresa estos valores en Render. Nunca los copies al repositorio:

| Variable | Uso |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC de Supabase con `sslmode=require`. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la conexión de base de datos de Supabase. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de esa conexión. |
| `SUPABASE_URL` | URL pública del proyecto de Supabase. |
| `SUPABASE_SERVICE_ROLE_KEY` | Clave secreta usada solo por Spring Boot para consultar perfiles simulados. |

## Después del despliegue

1. Espera a que el estado del servicio sea **Live**.
2. Abre la URL `https://<nombre-del-servicio>.onrender.com/login`.
3. Confirma que aparece el inicio de sesión y que un registro o una evaluación con DNI ficticio consulta los perfiles de Supabase.

El chequeo de salud apunta a `/login`. Render entrega `PORT`; la aplicación ya lo usa con `server.port=${PORT:8080}`.
