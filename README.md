# Sistema Crediticio Bancario — versión 1.20

Aplicación académica para registrar clientes y solicitudes de crédito, consultar evaluaciones y generar reportes. Esta versión usa PostgreSQL para guardar la información.

## Tecnologías

- Java 21 y Spring Boot 3.3
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

1. Inicia PostgreSQL y crea una base de datos llamada `crediticio_db`.
2. Revisa `src/main/resources/application.properties` y configura la URL, usuario y contraseña de tu PostgreSQL.
3. `spring.jpa.hibernate.ddl-auto=update` crea o actualiza las tablas del proyecto; no crea la base de datos.
4. Al iniciar, Spring Boot ejecuta `src/main/resources/data.sql` después de preparar las tablas.

`data.sql` contiene 10 clientes y 10 solicitudes de demostración. Las inserciones revisan si el DNI o código ya existe, para que reiniciar la aplicación no duplique esos datos. Las solicitudes están vinculadas con sus clientes.

## Validaciones

Los formularios verifican campos en el navegador y vuelven a validarlos en el servidor con Jakarta Bean Validation. Cuando un dato no es válido, el sistema muestra un mensaje para corregirlo.

## JavaScript y acceso

Los datos de clientes y solicitudes se gestionan desde el backend y PostgreSQL. `app.js` se usa para la navegación del acceso demostrativo; `dataSeed.js` y `dataService.js` permanecen como archivos antiguos y las plantillas no los cargan como fuente activa de datos. El acceso todavía no autentica usuarios con Spring Security.

La sección de ofertas aún no guarda información en la base de datos.

## Ejecutar el proyecto

Requisitos: Java 21 y PostgreSQL en ejecución.

Desde PowerShell, en la carpeta raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```
Conectar o poner la credencial de la contraseña en application.properties (se usa postgreSQL)
También puedes abrir el proyecto en NetBeans y ejecutar `CrediticioBancarioApplication.java`.

Luego visita:

```text
http://localhost:8080
```

## Nota sobre la configuración

Las credenciales se guardan en el archivo local `application.properties`. No las compartas en repositorios o copias públicas.
