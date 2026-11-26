# Cambio: perfil del cliente y múltiples solicitudes (24/11/2026)

## Qué se modificó
- Portal del cliente: primero muestra una **tabla compacta de datos personales y crediticios**, separando datos del registro local e indicadores consultados de la API simulada (`http://localhost:8081`).
- Formulario: solo cuatro campos: **tipo de crédito, monto solicitado, plazo en meses y motivo**. No solicita nuevamente nombre, DNI, ingresos, deudas ni gastos; se consultan del perfil/servicio.
- Envío: `POST /api/v1/portal/solicitudes`, `Content-Type: application/json`, con cuerpo `{ "solicitud": {"tipoCredito":"Personal", "montoSolicitado":10000, "plazo":24, "motivoCredito":"Compra"}, "documentos":[] }`.
- Permisos: ruta del portal autorizada explícitamente para sesiones con `tipoCuenta=CLIENTE`, mientras que sesiones de personal no pueden suplantar una cuenta de cliente. La respuesta de error es JSON UTF-8.
- Múltiples solicitudes: no existe un bloqueo por solicitudes anteriores. Cada nueva solicitud recibe código `SOL-<id real>` y su propia evaluación. Todas quedan en historial, ordenadas de la más reciente a la más antigua.
- Evaluación: cada nueva solicitud sigue consultando al servicio externo; el puntaje, los ingresos y la deuda externa no pueden alterarse desde el formulario. Si el servicio está caído, la transacción no finaliza.
- Confirmación: tras crear una solicitud se vuelve al portal con el código y la decisión devueltos por el servidor. **Una solicitud rechazada no impide registrar otra**; eso no significa aprobación garantizada.

## Instalación sin borrar datos
1. **Haz copia de seguridad** de `crediticio_db` desde pgAdmin (recomendado).
2. Detén el servidor bancario anterior (PowerShell del puerto **8080**, presiona `Ctrl+C`).
3. Reemplaza la carpeta `CrediticioBancario-v1.33-Registro-Externo` por la de este ZIP. Puedes mantener la carpeta del servicio simulado que ya funciona, ya que no requiere cambios para este ajuste.
4. Mantén PostgreSQL (5432) y la API simulada (8081) encendidos.
5. En PowerShell, dentro de la carpeta bancaria actualizada (la que contiene `pom.xml`), ejecuta:

```powershell
java -version   # Se recomienda JDK 21
$credencial = Get-Credential -UserName postgres
$env:DB_PASSWORD = $credencial.GetNetworkCredential().Password
$env:SERVICIO_DATOS_URL = 'http://localhost:8081'
.\mvnw.cmd clean spring-boot:run
```

6. Abre `http://localhost:8080/login`. Cierra la sesión previa y **vuelve a entrar con un DNI cliente** (por ejemplo, `90000003`) y su contraseña de prueba si fue creado por el semillero (`DemoCredito2026!`). Presiona `Ctrl+F5` para descartar JavaScript almacenado en caché.
7. Envía una solicitud nueva y luego otra. Ambas deben aparecer en el historial con códigos diferentes, aunque la primera haya sido rechazada.

**No ejecutes `REINICIO_CLIENTES_UNA_SOLA_VEZ.sql` para aplicar esta actualización.** Ese script borra datos, mientras que este cambio no requiere migración de la base.

## Comprobación de red en F12
En **Network > Fetch/XHR** debe aparecer:

```text
POST http://localhost:8080/api/v1/portal/solicitudes
Content-Type: application/json
201 Created
```

En caso de 401, vuelve a iniciar sesión. En caso de 403, revisa si entraste con una cuenta de personal en vez de una cuenta de cliente o si todavía estás levantando la versión anterior del servidor. Los errores de negocio o de API externa se muestran debajo del título del formulario.

## Pruebas incluidas
- `AuthInterceptorTest`: permisos de cliente para envíos sucesivos y denegación a personal.
- `SolicitudServiceMultiplesTest`: un cliente puede crear dos solicitudes con códigos diferentes y se evalúan ambas.
- Se conserva `EvaluacionAutomaticaServiceTest` para las decisiones automáticas.

El código Java y JavaScript pasó revisión de sintaxis. No se ejecutaron las pruebas Maven contra PostgreSQL en este entorno; valida el funcionamiento completo desde tu PC.
