# Sistema Crediticio Bancario — versión 1.23

Aplicación web académica para registrar clientes y solicitudes de crédito, guardar evaluaciones y consultar reportes. Esta versión conecta las pantallas con una base PostgreSQL.

## Tecnologías

- Java 21 y Spring Boot 3.3
- Spring Web MVC y Thymeleaf para rutas y páginas web
- HTML, CSS, JavaScript puntual y Bootstrap 5.3.8 por CDN
- Spring Boot Validation / Jakarta Bean Validation
- Spring Data JPA e Hibernate
- PostgreSQL
- Apache PDFBox para descargar reportes en PDF
- Maven mediante Maven Wrapper

## Organización del proyecto

```text
src/main/java/utp/edu/com/crediticio/
├── CrediticioBancarioApplication.java
├── controller/       Recibe rutas y formularios web
├── model/            Entidades y modelos de formularios
├── repository/       Busca y guarda entidades con Spring Data JPA
└── service/          Coordina las operaciones del sistema

src/main/resources/
├── templates/        Páginas Thymeleaf por módulo
├── static/css/       Estilos
├── static/img/       Logotipo
├── static/js/        Scripts del navegador
├── application.properties
└── data.sql          Registros de demostración relacionados
```

El backend es una aplicación monolítica organizada por responsabilidades. En la interfaz usa el flujo MVC de Spring: el navegador solicita una ruta, el controlador procesa la acción y devuelve una vista Thymeleaf.

### Flujo de una operación

```text
Navegador → Controller → Service → Repository → PostgreSQL
     ↑                                      ↓
     └──────── Vista Thymeleaf ←────────────┘
```

- **Controller:** recibe la solicitud web y decide qué página devolver.
- **Service:** coordina la acción y las reglas del módulo.
- **Repository:** consulta o guarda datos con Spring Data JPA.
- **Model:** representa entidades como `Cliente`, `Solicitud` y `Evaluacion`.
- **Thymeleaf:** muestra en HTML los datos preparados por el backend.

## Módulos

- Panel administrativo
- Clientes: registro, consulta, edición y eliminación
- Solicitudes: registro, detalle, actualización de estado y eliminación
- Evaluaciones: registro, detalle y decisión
- Políticas: administración de criterios
- Usuarios internos: directorio del personal
- Reportes: resumen y descarga de PDF

## Base de datos y datos de muestra

1. Inicia PostgreSQL y crea una base llamada `crediticio_db` desde pgAdmin o ejecutando `CREATE DATABASE crediticio_db;`.
2. Revisa `src/main/resources/application.properties` y configura el puerto, URL, usuario y contraseña de tu PostgreSQL.
3. `spring.jpa.hibernate.ddl-auto=update` crea o actualiza las tablas mapeadas; **no crea la base de datos**.
4. Al iniciar la aplicación, `data.sql` carga registros de demostración: clientes, solicitudes, evaluaciones, políticas y usuarios. Las inserciones comprueban si el registro ya existe para evitar duplicar la muestra al reiniciar.

Las solicitudes están relacionadas con sus clientes y las evaluaciones con sus solicitudes. Si vacías las tablas y vuelves a iniciar la aplicación, los datos de muestra se insertarán nuevamente.

## Cómo ejecutar

Requisitos: Java 21 y PostgreSQL en ejecución.

Desde la carpeta raíz del proyecto, en PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

También puedes abrir la carpeta del proyecto en NetBeans y ejecutar `CrediticioBancarioApplication.java`.

Cuando el servidor esté iniciado, abre:

```text
http://localhost:8080
```

## Validaciones y JavaScript

Los formularios validan datos en el navegador y nuevamente en el servidor con Jakarta Bean Validation. El servidor puede mostrar mensajes junto a los campos que necesitan corrección.

Los datos de clientes, solicitudes, evaluaciones y reportes se consultan desde PostgreSQL; no se utiliza `localStorage` para persistirlos. `app.js` se usa en el acceso demostrativo. `dataSeed.js` y `dataService.js` permanecen en `static/js`, pero las plantillas no los cargan como fuente activa de datos.

## Alcance actual

- El acceso es demostrativo: todavía no autentica al usuario ni protege módulos con Spring Security.
- Los registros de usuarios representan un directorio; no son cuentas de inicio de sesión.
- La sección de ofertas aún no persiste sus datos en PostgreSQL.
- Es un proyecto académico para mostrar el flujo de evaluación; no determina por sí solo la aprobación de un crédito bancario real.

## Decisiones de solicitudes y evaluaciones

- Aprobar, rechazar o enviar a revisión manual actualiza el estado de la solicitud.
- Si existe una evaluación enlazada, también se actualiza su resultado para mantener ambos registros sincronizados.
- Los cambios se guardan juntos en una transacción: si falla una escritura, no queda una mitad del cambio aplicada.
- Si ocurre un error al guardar, la pantalla vuelve al módulo y muestra un aviso; el detalle técnico se conserva en la consola del servidor.
- Los identificadores auxiliares de los formularios (`idCliente` e `idSolicitud`) son transitorios. Se comprueban al guardar el formulario desde el servicio y no se validan como columnas de la entidad al cambiar estados.

## Configuración local

Las credenciales de PostgreSQL se configuran localmente en `application.properties`. No incluyas contraseñas reales en copias públicas o repositorios compartidos.
