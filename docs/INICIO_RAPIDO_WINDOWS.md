# Inicio rápido — CrediticioBancario v1.33 JSON (Windows)

Esta edición migra los formularios que modifican datos a peticiones REST con JSON.

Consulta la guía completa: **GUIA_JSON_Y_ARRANQUE.md**.

1. Verifica Java JDK 21: `java -version`.
2. Inicia PostgreSQL y crea `crediticio_db` (si aún no existe).
3. Configura `spring.datasource.password` en `src/main/resources/application.properties`.
4. En PowerShell, desde la carpeta de `pom.xml`, ejecuta `.\mvnw.cmd clean spring-boot:run`.
5. Abre `http://localhost:8080/login`; no requiere Node.js.
6. F12 → Network: comprueba `POST /api/v1/...`, `Content-Type: application/json` y `Request Payload`.

Adjuntos enviados como Base64 en JSON; las descargas permanecen binarias.
