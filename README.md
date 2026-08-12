# _Bank Card Management System_

REST API для управления банковскими картами: создание и просмотр карт, переводы между своими картами, роли ADMIN/USER, JWT-аутентификация.

## Стек технологий

- Java 17, Spring Boot 3.2
- Spring Security + JWT (jjwt)
- Spring Data JPA + PostgreSQL
- Liquibase (YAML-миграции)
- springdoc-openapi (Swagger UI)
- Docker / Docker Compose
- JUnit 5, Mockito, MockMvc, AssertJ

## Быстрый запуск (Docker Compose)

Требуется установленный Docker и Docker Compose.

```bash
docker compose up --build
```

Приложение поднимется на `http://localhost:8080`, PostgreSQL - на `localhost:5432`.
Liquibase-миграции применяются автоматически при старте приложения.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
