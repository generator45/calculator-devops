# Calculator REST API — End-to-End CI/CD Pipeline

**CSE 816 — Software Production Engineering · Mini-Project (10 marks)**
M.Tech (CSE), Term I, 2026-27 · Faculty: Prof. B. Thangaraju

A Calculator REST API in Java (Spring Boot + Maven), wrapped in an automated DevOps
pipeline: Git/GitHub → Jenkins → Docker → Docker Compose.

## Planned endpoints

| Operation | Endpoint |
|---|---|
| Add | `GET /api/v1/add?a=&b=` |
| Subtract | `GET /api/v1/subtract?a=&b=` |
| Multiply | `GET /api/v1/multiply?a=&b=` |
| Divide | `GET /api/v1/divide?a=&b=` (`b=0` → HTTP 400) |
| Health | `GET /actuator/health` |

## Build and run

```bash
mvn clean package
mvn spring-boot:run        # http://localhost:8080
```

## TODO

- [ ] Service + controller + validation
- [ ] JUnit 5 unit tests and MockMvc integration tests
- [ ] Multi-stage Dockerfile
- [ ] Docker Compose deployment
- [ ] Jenkins declarative pipeline
- [ ] Report and screenshots
