# Sistema Crediticio Bancario — versión 1.20

Aplicación académica para registrar clientes y solicitudes de crédito, consultar evaluaciones y generar reportes. Esta versión usa PostgreSQL para guardar la información.

## Tecnologías

- Java 25 LTS y Spring Boot 3.5
- Spring Web MVC y Thymeleaf para las páginas web
- HTML, CSS, JavaScript y Bootstrap
- Spring Boot Validation / Jakarta Bean Validation
- Spring Data JPA e Hibernate
- PostgreSQL
- Apache PDFBox para reportes PDF
- Maven Wrapper para ejecutar el proyecto

## Estructura

```text
src/main/java/utp/edu/com/crediticio/
├── CrediticioBancarioApplication.java  Inicio de la aplicación
├── controller/                         Recibe las rutas y formularios
├── model/                              Entidades y modelos de formularios
├── repository/                         Consulta y guarda datos con Spring Data
└── service/                            Coordina las operaciones del sistema

src/main/resources/
├── templates/                          Páginas Thymeleaf por módulo
├── static/css/                         Estilos
├── static/img/                         Logotipo e imagen
├── static/js/                          JavaScript del navegador
├── application.properties              Puerto y conexión a PostgreSQL
└── data.sql                            Datos de demostración
```

La aplicación reúne los módulos en un solo proyecto y organiza el backend por responsabilidades. Spring MVC conecta las rutas de los controladores con las vistas Thymeleaf.

## Módulos principales

- Panel administrativo
- Clientes y perfiles
- Solicitudes de crédito
- Evaluaciones
- Políticas
- Usuarios internos
- Reportes y descarga de PDF

## Base de datos y muestra

La aplicación no guarda credenciales en archivos versionados. Configura estas variables de entorno antes de iniciar:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`

`SUPABASE_URL` apunta al proyecto que contiene los perfiles académicos simulados.
`SUPABASE_SERVICE_ROLE_KEY` se usa exclusivamente dentro del servidor para consultar la
función protegida `obtener_perfil_crediticio`; no debe ponerse en JavaScript, HTML, Git ni
ninguna variable pública del despliegue.

`spring.jpa.hibernate.ddl-auto=update` crea o actualiza las tablas del proyecto; no crea la base de datos. Al iniciar, Spring Boot ejecuta `src/main/resources/data.sql` después de preparar las tablas.

`data.sql` contiene 10 clientes y 10 solicitudes de demostración. Las inserciones revisan si el DNI o código ya existe, para que reiniciar la aplicación no duplique esos datos. Las solicitudes están vinculadas con sus clientes.

## Validaciones

Los formularios verifican campos en el navegador y vuelven a validarlos en el servidor con Jakarta Bean Validation. Cuando un dato no es válido, el sistema muestra un mensaje para corregirlo.

## JavaScript y acceso

Los datos de clientes y solicitudes se gestionan desde el backend y PostgreSQL. `app.js` se usa para la navegación del acceso demostrativo; `dataSeed.js` y `dataService.js` permanecen como archivos antiguos y las plantillas no los cargan como fuente activa de datos. El acceso todavía no autentica usuarios con Spring Security.

La sección de ofertas aún no guarda información en la base de datos.

## Perfil ficticio académico

En el perfil de un cliente cuyo DNI tenga el formato `9[0-9]{7}`, el sistema consulta un servicio externo académico y muestra información de referencia. Estos son datos simulados; no RENIEC ni SBS. La consulta es solo informativa y no cambia decisiones crediticias, estados ni aprobaciones.

## Ejecutar el proyecto

Requisitos: JDK 25 LTS, PostgreSQL accesible y las variables de entorno configuradas.

Desde PowerShell, en la carpeta raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```
También puedes abrir el proyecto en NetBeans y ejecutar `CrediticioBancarioApplication.java`.

Luego visita:

```text
http://localhost:8080
```

## Nota sobre la configuración

Las credenciales se guardan en el archivo local `application.properties`. No las compartas en repositorios o copias públicas.
