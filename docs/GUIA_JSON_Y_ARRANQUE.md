> **Actualización 24/11/2026:** Esta versión usa el flujo descrito en `GUIA_CAMBIO_FLUJO_AUTOMATICO.md` y NO incluye cuentas de cliente precargadas.

# CrediticioBancario v1.33 — intercambio JSON en las operaciones del frontend

## Qué se cambió
Las pantallas Thymeleaf **siguen siendo HTML**, pero todos los formularios que crean, editan, eliminan o deciden datos envían `fetch()` con `Content-Type: application/json` hacia `/api/v1/...`:

| Pantalla | Método/ruta REST | Cuerpo |
|---|---|---|
| Login | POST `/api/v1/auth/login` | JSON |
| Registro de cliente (cuenta) | POST `/api/v1/auth/registro-cliente` | JSON |
| Clientes internos | POST/PUT/DELETE `/api/v1/clientes[/id]` | JSON |
| Solicitud interna y adjuntos | POST `/api/v1/solicitudes/con-documentos` / PUT `.../{id}` | JSON con `solicitud` y `documentos` |
| Solicitud del cliente y adjuntos | POST `/api/v1/portal/solicitudes` | JSON con `solicitud` y `documentos` |
| Evaluaciones | POST/PUT `/api/v1/evaluaciones[/id]` | JSON |
| Decisión de revisión manual | POST `/api/v1/evaluaciones/{id}/decision` | JSON |
| Personal | POST/PUT/DELETE `/api/v1/personal[/id]` | JSON |
| Políticas | POST/PUT/DELETE `/api/v1/politicas[/id]` | JSON |
| Logout | POST `/api/v1/auth/logout` | JSON `{}` |
| Documentos opcionales | POST `/api/v1/solicitudes/{id}/documentos` | JSON con Base64 |

Los datos de los adjuntos van como `contenidoBase64` en JSON, sin `multipart/form-data` en los formularios migrados. Los límites son 5 MB por archivo, 20 MB en total. No se modifican documentos anteriores al editar solicitudes; los nuevos se agregan.

**Excepciones técnicas correctas:** GET de páginas HTML (`/login`, `/dashboard`, etc.), filtros GET por URL y descargas XLSX/PDF/JPG/PNG **no** son envíos de datos de formulario JSON. Las descargas usan contenido binario por naturaleza. Los datos de consulta API GET se reciben en JSON. Se mantienen controladores HTML heredados de formulario para compatibilidad, pero las pantallas migradas ya no los invocan.

## Iniciar en Windows 11
1. Instala Java JDK 21 y PostgreSQL. En PowerShell: `java -version` y `Get-Service *postgres*`.
2. Abre **pgAdmin 4**, selecciona BD `postgres`, Query Tool, ejecuta `CREATE DATABASE crediticio_db;` solo si no existe.
3. Cambia la contraseña del usuario `postgres` en `src/main/resources/application.properties` (`spring.datasource.password=...`).
4. Abre PowerShell dentro de la carpeta que contiene `pom.xml` y `mvnw.cmd`.
5. Ejecuta `./mvnw.cmd clean spring-boot:run` (en PowerShell: `.\mvnw.cmd clean spring-boot:run`).
6. Abre `http://localhost:8080/login`. El frontend está servido por Spring Boot, no necesita Node.js.
7. Usa las cuentas de demostración si la inicialización las creó: `andrea.salazar@banco.com` / `Banco123!` ; los clientes deben crear una nueva cuenta con DNI ficticio de la API.

## Cómo verificar JSON
- Navegador Chrome/Edge: **F12 → Red/Network → Fetch/XHR**.
- Registra un cliente o crea una solicitud (incluyendo PDF si deseas).
- Selecciona el POST/PUT de `/api/v1/...` y comprueba `Content-Type: application/json` y la pestaña *Payload* con un objeto JSON.
- Para los documentos verás `contenidoBase64`; para consultar solicitudes verás JSON en *Response*.
- Si la página no funciona, revisa en PowerShell el error del servidor. El proyecto requiere BD real y no se ha ensayado la ejecución completa con PostgreSQL en este entorno.

## Prueba de ejemplo desde PowerShell
```powershell
$s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$cred = @{ usuario='andrea.salazar@banco.com'; password='Banco123!' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType application/json -WebSession $s -Body $cred
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/v1/solicitudes -WebSession $s
```

## Advertencias
- JSON es un formato de transporte, **no cifra datos**. Usa HTTPS para exponerlo fuera de tu equipo.
- Las cuentas demo son solo para entorno académico. No publicar el proyecto sin seguridad adicional: protección CSRF, autorización por función/rol, auditoría y pruebas.
- Base64 aumenta el tamaño del JSON aprox. 33%; para producción se prefiere `multipart/form-data` para archivos aunque la información de negocio use JSON.
