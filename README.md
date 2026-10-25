# Calculator REST API — End-to-End CI/CD Pipeline

**CSE 816 — Software Production Engineering · Mini-Project (10 marks)**
M.Tech (CSE), Term I, 2026-27 · Faculty: Prof. B. Thangaraju

A production-style Calculator REST API in Java (Spring Boot + Maven), wrapped in a fully
automated DevOps pipeline: **Git/GitHub → Jenkins → Docker → Docker Compose**. A push to
`main` builds, tests, packages, containerizes, publishes and redeploys the service with no
manual steps.

---

## 1. Architecture

```
  developer                GitHub                   Jenkins (declarative pipeline)
 +----------+   git push  +--------+   webhook     +--------------------------------------+
 |  laptop  |------------>|  repo  |-------------->| Checkout -> Build -> Test -> Docker   |
 +----------+             +--------+               | Build -> Push -> Deploy -> Verify     |
                                                   +-------+------------------+------------+
                                                           | docker push      | docker compose up -d
                                                           v                  v
                                                  +--------------+  +---------------------------+
                                                  |  Docker Hub  |  | calculator-net (bridge)   |
                                                  | calculator-  |  |  +--------+   +---------+ |
                                                  |   api:<n>    |  |  | nginx  |-->| api x2  | |
                                                  +--------------+  |  |  :80   |   |  :8080  | |
                                                       |            |  +--------+   +---------+ |
                                                       +---pull---->+-------------+-------------+
                                                                                  |
                                                                      host :8080 -+
```

Email notification of build status is sent on success, failure and unstable.

## 2. API

Base URL: `http://localhost:8080`

| Operation | Endpoint | Behaviour |
|---|---|---|
| Add | `GET /api/v1/add?a=&b=` | returns `a + b` |
| Subtract | `GET /api/v1/subtract?a=&b=` | returns `a - b` |
| Multiply | `GET /api/v1/multiply?a=&b=` | returns `a * b` |
| Divide | `GET /api/v1/divide?a=&b=` | returns `a / b`; `b=0` → HTTP 400 |
| Health | `GET /actuator/health` | Actuator probe (`UP`/`DOWN`) |
| Info / Metrics | `GET /actuator/info`, `/actuator/metrics` | build and runtime info |

### Success response

```bash
$ curl -s "http://localhost:8080/api/v1/add?a=2&b=3"
{"operation":"add","a":2.0,"b":3.0,"result":5.0}
```

### Error responses — HTTP 400 with a structured body

```bash
# division by zero
$ curl -s "http://localhost:8080/api/v1/divide?a=10&b=0"
{"timestamp":"2026-10-25T06:41:12.884Z","status":400,"error":"Division by zero",
 "message":"Division by zero is not allowed: parameter 'b' must be non-zero",
 "path":"/api/v1/divide"}

# missing parameter
$ curl -s "http://localhost:8080/api/v1/add?a=2"
{"timestamp":"...","status":400,"error":"Missing parameter",
 "message":"Required parameter 'b' of type double is missing","path":"/api/v1/add"}

# non-numeric parameter
$ curl -s "http://localhost:8080/api/v1/multiply?a=abc&b=2"
{"timestamp":"...","status":400,"error":"Invalid parameter",
 "message":"Parameter 'a' must be a valid number but was 'abc'","path":"/api/v1/multiply"}

# non-finite operand, and a result that overflows a double
$ curl -s "http://localhost:8080/api/v1/add?a=NaN&b=1"
{"timestamp":"...","status":400,"error":"Invalid parameter",
 "message":"Parameter 'a' must be a finite number but was 'NaN'","path":"/api/v1/add"}
$ curl -s "http://localhost:8080/api/v1/divide?a=1e308&b=1e-308"
{"timestamp":"...","status":400,"error":"Invalid parameter",
 "message":"divide(1.0E308, 1.0E-308) overflows the range of a finite double", ...}
```

An unknown path returns **404** and a wrong HTTP method **405**, both in the same error shape
rather than being swallowed into a 500 by the catch-all handler.

## 3. Build, test and run

### Prerequisites

Java 21 (JDK) and Docker 24+ with the Compose v2 plugin. Maven is optional — the repo ships
the Maven wrapper (`./mvnw`), so no local Maven install is required.

### Local (Maven)

```bash
./mvnw clean package        # compile + test + build target/calculator.jar
./mvnw test                 # tests only; JUnit XML -> target/surefire-reports/
./mvnw spring-boot:run      # run on http://localhost:8080
java -jar target/calculator.jar
```

Substitute `mvn` for `./mvnw` if you prefer a local Maven 3.9+ install. Note that the wrapper
downloads Maven 3.9.12 into `~/.m2/wrapper` on first use, so the very first build on a fresh
(or offline) machine needs network access to `repo.maven.apache.org`.

Coverage (JaCoCo) is written to `target/site/jacoco/index.html`.

### Single container

```bash
docker build -t calculator-api:local .
docker run --rm -p 8080:8080 --name calculator calculator-api:local
curl "http://localhost:8080/api/v1/add?a=2&b=3"
```

### Full stack (Compose — 2 API replicas + Nginx proxy)

```bash
docker compose up -d --build
docker compose ps                       # both replicas should be (healthy)
curl "http://localhost:8080/api/v1/divide?a=10&b=4"
docker compose logs -f calculator
docker compose down                     # tear down
```

Pull the published image instead of building locally:

```bash
IMAGE_TAG=latest docker compose up -d
```

## 4. Testing

| Suite | File | Covers |
|---|---|---|
| Unit (JUnit 5, parameterized) | `src/test/java/.../service/CalculatorServiceTest.java` | all four operations, divide-by-zero, `0/0`, non-finite operands, overflowing results |
| Spring MVC integration (MockMvc) | `src/test/java/.../controller/CalculatorControllerTest.java` | every endpoint, actuator health, and every error path: 400 (divide-by-zero, missing, non-numeric, non-finite, overflow), 404, 405 |
| Context | `src/test/java/.../CalculatorApplicationTests.java` | application context wiring |

35 tests, all green. Surefire emits JUnit XML that the Jenkins `junit` publisher consumes;
a failing test fails the pipeline.

## 5. Docker image

Multi-stage build (`Dockerfile`):

| Stage | Base | Purpose |
|---|---|---|
| `build` | `maven:3.9-eclipse-temurin-21` | resolve dependencies in a cached layer, then `mvn package` |
| `runtime` | `eclipse-temurin:21-jre-alpine` | copies only `calculator.jar` |

Best practices applied: slim JRE-only runtime base, **non-root** user (`appuser`, uid 10001),
explicit `EXPOSE 8080`, dependency layer caching ahead of the source `COPY`, a container
`HEALTHCHECK` on `/actuator/health`, OCI labels, and a `.dockerignore` that keeps `target/`,
`.git/` and any `.env` out of the build context.

Published image: **`jayeshpandit/calculator-api`** on Docker Hub (tagged per Jenkins build
number, plus `latest`).

## 6. Deployment (Docker Compose)

`docker-compose.yml` defines:

- **`calculator`** — the API, `deploy.replicas: 2`, no published host ports (reached only
  through the proxy), a 512M memory limit, `restart: unless-stopped`, environment-driven
  configuration (`SERVER_PORT`, `APP_VERSION`, `LOG_LEVEL`, `JAVA_OPTS`), and a health check
  that greps `"status":"UP"` from `/actuator/health`.
- **`proxy`** — `nginx:1.27-alpine` reverse proxy / load balancer on host port 8080, started
  only once the API is `service_healthy`, with its own `/health` endpoint and health check.
- **`calculator-net`** — a user-defined bridge network joining the two services.

Nginx reaches the replicas through a variable `proxy_pass` plus
`resolver 127.0.0.11 valid=10s`, so the service name is re-resolved against Docker's embedded
DNS per request. That both round-robins across the replicas and keeps working after a redeploy
gives them new IPs — an `upstream` block would have resolved the name once at startup and then
served 502s until the proxy was restarted by hand.

## 7. CI/CD pipeline (Jenkins)

`Jenkinsfile` — declarative, triggered by a GitHub webhook (`githubPush()`) with
`pollSCM('H/5 * * * *')` as a fallback.

| Stage | What it does |
|---|---|
| Checkout | `checkout scm`; records the short SHA and commit subject |
| Build | `./mvnw clean package -DskipTests`; archives `target/calculator.jar` |
| Test | `./mvnw test`; publishes JUnit XML + JaCoCo coverage; fails the build on any test failure |
| Docker Build | builds the image tagged `${BUILD_NUMBER}` and `latest` |
| Push | pushes both tags to Docker Hub via the `dockerhub-credentials` credential |
| Deploy | `docker compose up -d --remove-orphans` (in-place reconcile, so re-runs are idempotent) |
| Verify Health | polls `/actuator/health` for `UP`, then asserts add, multiply and every error path (400 ×4, 404, 405) through the proxy |

`post` blocks prune dangling images older than a week — the tag the running stack is pinned to
is deliberately left alone — and send HTML email (success / failure with the log attached /
unstable) to the student's address.

### Jenkins setup

1. **Plugins** — Pipeline, Git, GitHub, JUnit, JaCoCo, Docker Pipeline, Email Extension,
   Timestamper (required by `options { timestamps() }`).
2. **Tools** (*Manage Jenkins → Tools*) — a JDK installation named `JDK21`. Maven is not
   needed: the pipeline uses the repo's `./mvnw` wrapper.
3. **Credentials** (*Manage Jenkins → Credentials*) — Username/Password credential with ID
   `dockerhub-credentials` holding a Docker Hub access token. Nothing is hard-coded; no
   secret is committed.
4. **Email** (*Manage Jenkins → System → Extended E-mail Notification*) — SMTP host
   `smtp.gmail.com`, port 465, SSL, with an app password stored as a Jenkins credential.
5. **Job** — Pipeline job, *Pipeline script from SCM*, this repository, script path
   `Jenkinsfile`, "GitHub hook trigger for GITScm polling" enabled.
6. **Webhook** — GitHub repo → *Settings → Webhooks* → `http://<jenkins-host>/github-webhook/`,
   content type `application/json`, event: *Just the push event*.
7. The Jenkins user must be in the `docker` group so it can build, push and run Compose.

## 8. Repository layout

```
calculator-devops/
├── src/main/java/com/iiitb/spe/calculator/
│   ├── CalculatorApplication.java
│   ├── controller/CalculatorController.java
│   ├── service/CalculatorService.java
│   ├── model/{CalculationResponse,ErrorResponse}.java
│   └── exception/{DivisionByZeroException,NonFiniteValueException,
│                  GlobalExceptionHandler}.java
├── src/main/resources/application.yml
├── src/test/java/com/iiitb/spe/calculator/...   # JUnit 5 + MockMvc
├── pom.xml                                      # Maven build, Surefire, JaCoCo
├── mvnw, mvnw.cmd, .mvn/                        # Maven wrapper - no local Maven needed
├── Dockerfile                                   # multi-stage, non-root
├── .dockerignore
├── docker-compose.yml                           # 2 API replicas + Nginx
├── nginx/nginx.conf
├── Jenkinsfile                                  # declarative CI/CD pipeline
├── scripts/smoke-test.sh                        # mirrors the pipeline's health gate
├── docs/REPORT.md, docs/REPORT.pdf              # 2-3 page report
├── docs/screenshots/                            # pipeline, email, docker ps, API output
├── .gitignore
└── README.md
```

## 9. Git workflow

`main` is the integration branch. Work lands through short-lived feature branches merged with
`--no-ff` so the topology records each merge:

```bash
git switch -c feature/<topic>
# ... commits ...
git switch main
git merge --no-ff feature/<topic>
```

Branches used: `feature/calculator-api`, `feature/unit-tests`, `feature/docker`,
`feature/compose-deployment`, `feature/jenkins-pipeline`, `docs/report`, and
`fix/pre-demo-hardening` for the end-to-end testing pass that closed out the project.
`git log --graph --oneline` shows the progressive history across the work window.

## 10. Deliverables

| Deliverable | Where |
|---|---|
| Source, `Jenkinsfile`, `Dockerfile`, `docker-compose.yml` | this repository |
| Docker Hub image | `https://hub.docker.com/r/jayeshpandit/calculator-api` |
| Report (architecture, stages, challenges) | [`docs/REPORT.md`](docs/REPORT.md), exported to [`docs/REPORT.pdf`](docs/REPORT.pdf) |
| Screenshots (green pipeline, email, `docker ps`, API responses) | [`docs/screenshots/`](docs/screenshots/) — checklist in that directory; captured while preparing the demo |
| Live demo | push a change → webhook → pipeline → redeploy |

## 11. Scope

Stops at Compose-based deployment, per the problem statement. Ansible, Kubernetes and
Prometheus/Grafana are added in the end-of-semester Capstone, which extends this same
pipeline.

## License

MIT — academic coursework.
