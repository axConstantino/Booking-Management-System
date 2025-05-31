# Booking Management System API 🏨 – Backend para Reservas con Spring Boot y Docker

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)
![Docker](https://img.shields.io/badge/Docker-blue.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue.svg)
![Redis](https://img.shields.io/badge/Redis-red.svg)
![Maven](https://img.shields.io/badge/Maven-Apache-red.svg)
![Tests](https://img.shields.io/badge/Tests-JUnit%20%26%20Testcontainers-green.svg)

API RESTful para un sistema de gestión de reservas, construido con Java y Spring Boot. Este proyecto demuestra un backend robusto con funcionalidades para la administración de habitaciones, autenticación/autorización de usuarios, y está preparado para futuras expansiones como gestión de reservas por usuarios y procesamiento de pagos.

---

## ✨ Características Principales

* **Gestión de Habitaciones (Admin):**
    * Crear, Leer (con paginación y ordenamiento), Actualizar y Eliminar habitaciones.
    * Subida y gestión de múltiples imágenes por habitación (integrado con Cloudinary).
* **Autenticación y Autorización:**
    * Seguridad basada en Spring Security.
    * Autenticación mediante JSON Web Tokens (JWT).
    * Roles de usuario (ej. `ROLE_ADMIN`, `ROLE_USER` - por implementar para usuarios).
* **Documentación de API:**
    * API documentada con Swagger (OpenAPI) accesible vía `/swagger-ui.html`.
* **Contenerización:**
    * Configuración completa con Docker y Docker Compose para un fácil despliegue y entorno de desarrollo consistente.
    * Servicios: Aplicación Spring Boot, PostgreSQL, Redis, MailHog.
* **Testing:**
    * Pruebas unitarias y de integración.
    * Uso de Testcontainers para pruebas de integración con PostgreSQL y Redis, asegurando un entorno de pruebas fiable.

## 🚧 Próximas Características (Roadmap)
- [ ] Sistema completo de reservas para usuarios
- [ ] Integración con Stripe para pagos
- [ ] Notificaciones por email automatizadas
---

## 🛠️ Tecnologías Utilizadas

* **Backend:**
    * Java 17
    * Spring Boot 3.4.4
        * Spring Web (RESTful APIs)
        * Spring Data JPA (con PostgreSQL)
        * Spring Data Redis (para caching)
        * Spring Security (Autenticación y Autorización con JWT)
        * Spring Validation
        * Spring Mail (con MailHog para desarrollo)
* **Base de Datos:** PostgreSQL 16
* **Caché:** Redis 7.2
* **Contenerización:** Docker, Docker Compose
* **Build y Gestión de Dependencias:** Apache Maven
* **Testing:**
    * JUnit 5
    * Mockito
    * Testcontainers (PostgreSQL, Redis modules)
    * Awaitility
    * Spring Boot Test, Spring Security Test
* **API Documentation:** SpringDoc OpenAPI (Swagger UI)
* **Librerías Adicionales:**
    * Lombok
    * MapStruct (para DTO mapping)
    * JJWT (JSON Web Tokens)
    * Stripe Java (para futura integración de pagos)
    * Cloudinary (para gestión de imágenes)
    * Libphonenumber (para validación de números telefónicos)
* **Entorno de Desarrollo:**
    * MailHog (para pruebas de envío de correos)

---

## 🚀 Configuración y Puesta en Marcha

Sigue estos pasos para levantar el proyecto localmente usando Docker Compose (recomendado).

### Prerrequisitos

* Git
* Docker Engine y Docker Compose
* JDK 17 (si deseas compilar y ejecutar fuera de Docker)
* Maven (si deseas compilar y ejecutar fuera de Docker - el proyecto incluye Maven Wrapper)

### 1. Clonar el Repositorio

```bash
git clone https://github.com/axConstantino/BookingManagementSystem.git

cd BookingManagementSystem
```
### 2. Configurar El Entorno
Crear archivo .env basado en el ejemplo:

```bash
# Environment profile (e.g. dev, test, prod)
SPRING_PROFILES_ACTIVE=

# Base URL of the application (e.g. http://localhost:8080)
BASE_URL=

# Database configuration
DB_URL=                             # e.g. jdbc:postgresql://localhost:5432/your_db
DB_USER=
DB_PASSWORD=

# Redis configuration
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

# JWT configuration
JWT_SECRET=
JWT_ACCESS_EXPIRATION=             # e.g. 3600000 (1 hour in milliseconds)
JWT_REFRESH_EXPIRATION=            # e.g. 604800000 (7 days in milliseconds)

# Token used for password reset (optional secret)
PASSWORD_RESET_TOKEN=

# Stripe payment integration
STRIPE_API_KEY=
STRIPE_WEBHOOK_SECRET=

# Cloudinary image storage
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_SECRET_KEY=

# Email configuration
MAIL_HOST=
MAIL_PORT=                          # e.g. 587 (for TLS), 465 (for SSL)

```

### 3. Ejecutar con Docker Compose

```bash
docker-compose up --build
```


## 🚀 Servicios Disponibles

- **API**: [http://localhost:8080](http://localhost:8080)
- **PostgreSQL**: `localhost:5432`
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **MailHog UI**: [http://localhost:8025](http://localhost:8025)

---

## 📖 Uso de la API

Accede a la documentación interactiva en Swagger UI:  
➡️ [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Endpoints Principales (Admin)

- `GET /admin/rooms`: Lista paginada de habitaciones
- `POST /admin/rooms`: Crea nueva habitación
- `DELETE /admin/rooms/{roomId}`: Elimina habitación

---

## 🧪 Ejecutar Pruebas

```bash
./mvnw clean test
```

## 💡 Futuras Mejoras

- Notificaciones por correo electrónico
- Frontend para consumir la API

---

## 👨‍💻 Autor

**Axel Constantino Olvera**  
📧 [olveraconstantinoaxel@gmail.com](mailto:olveraconstantinoaxel@gmail.com)  
🐙 [GitHub](https://github.com/axConstantino)  
💼 [LinkedIn](www.linkedin.com/in/axel-constantino-olvera-947a38368)

