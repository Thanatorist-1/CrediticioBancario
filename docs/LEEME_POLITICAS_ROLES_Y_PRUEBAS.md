# POLÍTICAS, TIPOS DE CRÉDITO Y PERMISOS — VERSIÓN ACADÉMICA

Esta versión modifica **solo el sistema bancario**. El ServicioDatosCrediticiosSimulados (8081) se conserva sin cambios.

## Qué cambió
- Roles verificados en cada petición contra `usuarios`, no solo por el menú de Thymeleaf.
- CLIENTE: portal propio, solicitudes y adjuntos de sus solicitudes.
- ANALISTA: clientes, solicitudes, reportes, políticas (lectura) y pedir documentos en revisión.
- SUPERVISOR: lo anterior y resolución manual final motivada; no puede alterar políticas.
- ADMINISTRADOR: cuenta de personal, tipos de crédito, políticas, activación/desactivación y auditoría.
- El API de políticas `/api/v1/politicas` permite GET a personal; POST/PUT/DELETE (desactivación lógica) requiere administrador. Los endpoints de tipos de crédito están reservados al administrador.
- Nuevas tablas JPA: `tipos_credito` y `auditoria_configuracion`. No se eliminan tablas existentes.
- Un tipo de crédito debe estar ACTIVO y tener una POLÍTICA ACTIVA para aparecer en el formulario del cliente.
- Cuando se activa una política nueva, las anteriores activas del mismo tipo se desactivan y los cambios se auditan; las decisiones históricas no se recalculan.
- La auditoría registra usuario interno, fecha/hora, operación, valores anteriores y posteriores (JSON).
- Opciones de política: score mínimo de aprobación / máximo de rechazo; DTI máximo de aprobación / desde rechazo; monto mínimo y máximo; plazo mínimo y máximo; tasa anual referencial; empleo actual; garantía; edad máxima para revisión humana.
- La cuota referencial incorpora la tasa fija configurada por el administrador. NO equivale a la TCEA ni incluye seguros/comisiones. Garantía y edad excepcional generan **revisión manual**, no rechazo automático por sí solas.
- Se conserva la comunicación JSON entre la interfaz de administración y las APIs REST.

## Preparación
1. Haz respaldo de `crediticio_db` antes de actualizar; NO ejecutes `REINICIO_CLIENTES_UNA_SOLA_VEZ.sql`.
2. Detén Spring Boot bancario (8080) pero conserva PostgreSQL y la API simulada (8081).
3. Instala Java 21. Mantén la base `crediticio_db` existente. En esta versión `spring.jpa.hibernate.ddl-auto=update` crea las nuevas tablas/columnas al iniciar; no borra clientes.
4. En PowerShell, dentro de la carpeta que contiene `pom.xml` ejecuta:

```powershell
java -version
$credencial = Get-Credential -UserName postgres
$env:DB_PASSWORD = $credencial.GetNetworkCredential().Password
$env:SERVICIO_DATOS_URL = "http://localhost:8081"
$env:DEMO_SEED_ENABLED = "false"

# Sólo el primer arranque, cuando aún no exista un administrador:
$env:ADMIN_BOOTSTRAP_EMAIL = "admin.creditos@banco.com"
$env:ADMIN_BOOTSTRAP_PASSWORD = Read-Host "Clave inicial de administrador (mínimo 12 caracteres)" -AsSecureString | ForEach-Object {
  $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($_)
  try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
  finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

.\mvnw.cmd clean spring-boot:run
```

Tras crear la primera cuenta de administrador, elimina `ADMIN_BOOTSTRAP_PASSWORD` de tu entorno cuando sea posible y cambia su contraseña desde administración. Si ya existe un administrador, NO se recrea ni sobrescribe.

## Prueba de creación de un producto nuevo
1. Inicia sesión como Administrador en `http://localhost:8080/login`.
2. Abre `http://localhost:8080/tipos-credito` y crea `Agrario` / `Crédito agrario`, con estado Activo.
3. En `http://localhost:8080/politicas/nueva` crea una política para `Crédito agrario`, con score/DTI/monto/plazo y estado **Activa**.
4. Cierra sesión; ingresa como cliente demo. En `http://localhost:8080/portal-cliente` debe aparecer «Crédito agrario» en tipos disponibles.
5. Envía una solicitud; el servidor evalúa con la política activa y consulta el score/deudas reales **simuladas** en el puerto 8081.
6. Vuelve a entrar como administrador: `http://localhost:8080/politicas/auditoria`. Revisa la fecha, correo, Antes/Después.
7. Entra con Andrea (Analista): puede VER políticas, pero POST a `/api/v1/politicas` debe responder HTTP 403. Roberto (Supervisor) puede decidir revisiones manuales, no editar políticas.

## Endpoints
- GET `/api/v1/politicas`: visualización personal.
- POST `/api/v1/politicas`, PUT `/api/v1/politicas/{id}`: administrador, JSON.
- DELETE `/api/v1/politicas/{id}`: administrador; **desactiva**, NO borra la política ni la auditoría.
- GET/POST/PUT `/api/v1/tipos-credito` (GET, POST), PUT `/api/v1/tipos-credito/{id}`, POST `/api/v1/tipos-credito/{id}/desactivar`: administrador.
- GET `/api/v1/auditoria-configuracion`: administrador.

## Límites conocidos
- Motor académico, sin TCEA, validación jurídica de garantías, ni desembolso bancario real.
- Las opciones de política son campos predefinidos y validados; no existe un editor universal de expresiones o reglas arbitrarias.
- El proyecto conserva su seguridad basada en sesión; para uso real requeriría CSRF, TLS, controles adicionales y pruebas de penetración.
- No fue posible ejecutar Maven ni conectarse a tu PostgreSQL local en este entorno. La sintaxis Java se comprobó con el compilador de Java 21 y se añadieron pruebas JUnit; ejecuta `./mvnw.cmd clean test` en Windows antes de presentar la entrega.
