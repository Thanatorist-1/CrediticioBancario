# Guía de arquitectura por capas, CRUD y validaciones — versión 1.21

Los datos demostrativos iniciales se encuentran en `src/main/resources/data.sql`.

## Estructura por capas

```text
controller/                              ← Recibe las rutas web y elige la vista
service/                                 ← Coordina las reglas y operaciones
├── cliente/ solicitud/ evaluacion/
├── politica/ usuario/ reporte/
model/                                   ← Entidades JPA y datos de reporte
├── cliente/ solicitud/ evaluacion/
├── politica/ usuario/ reporte/
repository/                              ← Acceso a datos con Spring Data JPA
├── cliente/ solicitud/ evaluacion/
└── politica/ usuario/
resources/templates/                     ← Vistas Thymeleaf
```

La vista y el controlador forman la parte web MVC. Alrededor de esa parte, el backend separa servicios, entidades y acceso a datos. Cada dominio mantiene su nombre de subcarpeta, por ejemplo `service/cliente`, `model/cliente` y `repository/cliente`.

## ¿Qué hace cada archivo?

- `Cliente.java`, `Solicitud.java`, `Evaluacion.java`, `Politica.java`: entidades JPA (`@Entity`), campos y relación con las tablas.
- `model/*/*.java`: entidades JPA (`@Entity`), sus campos y relaciones con las tablas. Los objetos `ReporteResumen` y `ReporteMensual` transportan resultados calculados.
- `repository/*/*Repository.java`: interfaces `JpaRepository`; Spring Data provee los métodos `findAll`, `findById`, `save` y `deleteById`.
- `service/*/*Service.java`: operaciones del dominio. Asigna códigos, busca relaciones necesarias (por ejemplo, el cliente de una solicitud) y coordina el guardado. En reportes también se encuentra `ReportePdfService`.
- `*Controller.java`: define rutas GET/POST, recibe el formulario, llama al servicio y selecciona la vista.
- `templates/`: presenta listados, formularios y detalles con Thymeleaf.
- `application.properties`: define puerto, conexión y generación/actualización automática de tablas con Hibernate.

## Flujo CRUD de ejemplo: Cliente

1. `GET /clientes` ejecuta `ClienteController.listar`.
2. El Controller llama a `service/cliente/ClienteService.listar`.
3. El Service consulta `repository/cliente/ClienteRepository.findAll()`.
4. El Repository usa JPA para consultar PostgreSQL.
5. El Controller entrega la lista a `administracion/clientes.html` y Thymeleaf la renderiza.
6. Alta y edición usan `GET /clientes/registrar`, `GET /clientes/editar/{id}` y `POST /clientes/guardar`.
7. El borrado se envía por `POST /clientes/eliminar/{id}`.

Solicitudes y evaluaciones siguen el mismo recorrido y se asocian mediante claves foráneas: Solicitud referencia Cliente, y Evaluación referencia Solicitud. Las acciones Aprobar/Rechazar/Revisión manual sincronizan el resultado de evaluación y estado de solicitud. Políticas y usuarios internos también tienen alta, listado, edición y eliminación.

## Reportes conectados a PostgreSQL

1. `GET /reportes` llama a `ReporteController` y recibe las fechas opcionales del formulario.
2. `service/reporte/ReporteService` valida el rango y solicita al `repository/solicitud/SolicitudRepository` las solicitudes del periodo, incluyendo los datos del cliente.
3. El Service calcula conteos por estado, monto total, porcentajes y agrupación mensual. Es una consulta de lectura: no agrega ni modifica registros.
4. `reportes/reportes.html` muestra los indicadores y el botón **Ver detalles** abre `GET /reportes/detalles`.
5. `reportes/detalle-reportes.html` presenta las solicitudes reales del periodo y permite abrir cada solicitud.
6. **Generar** vuelve a consultar y actualizar el resumen en pantalla. **Descargar PDF** llama a `GET /reportes/descargar` con las mismas fechas; `service/reporte/ReportePdfService` crea un documento con los indicadores, el resumen mensual y la tabla de solicitudes filtradas.
7. El navegador recibe el archivo como descarga. Si el rango de fechas no es válido, se vuelve al resumen y se muestra el mensaje de validación.

Esta pantalla no carga `dataSeed.js`, `dataService.js` ni `app.js`. `app.js` queda reducido a la navegación del acceso de demostración. `dataSeed.js` y `dataService.js` ya no contienen lógica activa ni se cargan desde las páginas. Oferta queda señalada como módulo pendiente de persistencia.

### Archivos del reporte descargable

- `ReporteController.java`: expone la ruta de descarga y devuelve el PDF al navegador.
- `service/reporte/ReporteService.java`: obtiene desde PostgreSQL las solicitudes del periodo y calcula los totales que comparte con la pantalla.
- `service/reporte/ReportePdfService.java`: da formato al documento y lo genera usando Apache PDFBox; no consulta la base por su cuenta.
- `model/reporte/ReporteResumen.java` y `ReporteMensual.java`: transportan resultados calculados hacia la vista y el generador PDF.
- `reportes.html`: mantiene el filtrado y resumen, con acciones distintas para actualizar pantalla, ver detalle o descargar PDF.

## Campos de los formularios

- **Cliente:** nombre, DNI, nacimiento, contacto, estado, empresa, cargo, antigüedad e información financiera mensual.
- **Solicitud:** cliente, tipo, monto, plazo, fecha, estado, motivo, ingresos, gastos, deudas y observaciones.
- **Evaluación:** solicitud, puntaje, riesgo, resultado, política, DTI, ingreso disponible, capacidad de cuota, monto máximo y comentario.
- **Política:** producto, versión, vigencia, estado y descripción.
- **Usuario interno:** nombre, correo institucional, cargo, rol y estado. La autenticación y gestión de contraseñas no se incluyen todavía.

Las entidades contienen los campos persistentes de esos formularios. Los identificadores de las relaciones son selecciones de Cliente o Solicitud, no texto libre.

## Configurar y ejecutar

`application.properties` contiene la configuración local solicitada (puerto, URL, usuario y contraseña). `spring.jpa.hibernate.ddl-auto=update` crea las tablas que falten y actualiza el esquema manteniendo datos existentes. La base `crediticio_db` debe crearse previamente en PostgreSQL. El driver se obtiene de la dependencia PostgreSQL en `pom.xml`; la propiedad `driver-class-name` se omitió porque Spring Boot lo detecta. No compartas el archivo con la contraseña en repositorios públicos.

Si se reutiliza una base con tablas antiguas, `update` no borra ni corrige automáticamente todos los nombres y tipos previos. Un error al listar clientes o solicitudes puede indicar una tabla incompatible; revisa el `Caused by` de NetBeans y respalda los datos antes de limpiar o migrar el esquema.

Ejecuta `mvnw.cmd spring-boot:run` con PostgreSQL activo y abre `http://localhost:8080`.

## Alcance actual

El Dashboard, Reportes y los módulos de Clientes, Solicitudes, Evaluaciones, Políticas y Usuarios consultan PostgreSQL. `app.js` solo conserva la navegación del acceso de demostración; los antiguos scripts de datos no se cargan y no contienen lógica activa. Oferta está identificada como pendiente de persistencia. Los datos de muestra se encuentran en `src/main/resources/data.sql` y se cargan automáticamente; las evaluaciones apuntan a sus solicitudes por la clave foránea `solicitud_id`.


## Datos iniciales (v1.20)

src/main/resources/data.sql contiene 10 clientes, 10 solicitudes, 10 evaluaciones, 3 políticas y 2 usuarios de demostración. Spring Boot lo ejecuta al iniciar gracias a spring.sql.init.mode=always; spring.jpa.defer-datasource-initialization=true espera a que Hibernate cree o actualice las tablas. Las solicitudes se buscan por código y cliente; las evaluaciones se insertan consultando el ID de su solicitud. Cada inserción comprueba DNI, correo o código, por lo que volver a iniciar la aplicación no repite los registros. La base crediticio_db debe existir previamente.
