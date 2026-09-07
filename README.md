# FaCT Data API
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Afact-data-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Afact-data-api)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Afact-data-api&metric=coverage)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Afact-data-api)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Afact-data-api&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Afact-data-api)

Spring Boot API for the Find a Court or Tribunal (FaCT) service.

This README is written for internal contributors who need to run the service locally and ship changes safely.

## What is in this repo

- Java 21 Spring Boot service (`src/main`) with PostgreSQL persistence
- Flyway schema/data migrations (`src/main/resources/db/migration`)
- Multiple test suites:
  - unit (`src/test`)
  - integration (`src/integrationTest`, Testcontainers-backed)
  - smoke (`src/smokeTest`, against a running instance)
  - functional (`src/functionalTest`, against a running instance with auth)
- Deployment config:
  - Helm chart (`charts/fact-data-api`)
  - Terraform (`infrastructure`)

## Prerequisites

- Java 21
- Docker (for local Postgres and integration tests)
- Access to required internal secrets/values (AAD, OS key, storage config)

Optional but recommended:

- IntelliJ IDEA with Lombok support enabled

## Quick start (host app + Docker database)

This is the most common workflow for local development.

1. Start PostgreSQL container:

```bash
docker compose up -d fact-database
```

2. Create a local `.env` file in the repository root. Example:

```bash
DB_HOST=localhost
DB_PORT=5999
DB_NAME=fact
DB_USER=fact
DB_PASSWORD=fact

AZURE_TENANT_ID=<tenant-id>
AZURE_CLIENT_ID=<client-id>
APP_REG_ID=<app-registration-id>

AZURE_STORAGE_ACCOUNT_NAME=<storage-account-name>
AZURE_STORAGE_CONNECTION_STRING=<storage-connection-string>

OS_KEY=<ordnance-survey-key>

# Optional local overrides
AZURE_MANAGED_IDENTITY_ENABLED=false
CATH_API_URL=<cath-api-url>
SLACK_TOKEN=
SLACK_CHANNEL_ID=
TESTING_SUPPORT_ENABLE_API=true
RUN_DB_MIGRATION_ON_STARTUP=true
```

3. Load the env file and run the app:

```bash
set -a
source .env
set +a
./gradlew bootRun
```

4. Check service health:

```bash
curl http://localhost:8989/health
```

The service listens on `http://localhost:8989`.

## Alternative local run (full Docker)

Use this if you want app + DB both in containers.

```bash
set -a
source .env
set +a
./deploy_local_docker.sh
```

`deploy_local_docker.sh` runs `./gradlew clean build` first, then `docker compose up --build -d`.

## Running from IntelliJ

1. Open the project.
2. Ensure env vars from `.env` are applied to the run configuration.
3. Start `fact-database` with Docker as shown above.
4. Run `uk.gov.hmcts.reform.fact.data.api.Application`.

## Authentication and API usage notes

- Public endpoints:
  - `/`
  - `/health` and `/health/readiness`
  - `/swagger-ui/*` and `/v3/api-docs`
  - `/testing-support/**` (only if `TESTING_SUPPORT_ENABLE_API=true`)
- Other endpoints require a valid Azure JWT.
- Admin-protected operations also require `X-User-Id` for auditing (except a small set of bootstrap/admin routes).

Useful links when running locally:

- Swagger UI: `http://localhost:8989/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8989/v3/api-docs`

## Test commands

Run from repository root.

```bash
./gradlew test
./gradlew integration
./gradlew smoke
./gradlew functional
./gradlew check
```

Notes:

- `integration` uses Testcontainers (`jdbc:tc:postgresql:17`) and needs Docker running.
- `smoke` targets `TEST_URL` (defaults to `http://localhost:8989`).
- `functional` also targets `TEST_URL` and needs extra auth env vars; see `src/functionalTest/README.md`.
- CI runs `./gradlew check` on push/PR to `master`.

## Database migrations (Flyway)

- Migration files live in `src/main/resources/db/migration`.
- Naming convention in this repo: `V1.xx__description.sql`.
- Keep version numbers unique; CI checks duplicates against `master`.

To run Flyway manually against local DB:

```bash
export FLYWAY_URL=jdbc:postgresql://localhost:5999/fact
export FLYWAY_USER=fact
export FLYWAY_PASSWORD=fact
./gradlew flywayMigrate
```

## Contribution workflow (team)

- Branch from `master`.
- Link PR to Jira ticket (ticket key in PR template).
- Update tests/docs with your change.
- Before raising PR, run at least:

```bash
./gradlew test integration
```

For API or behaviour changes, run smoke/functional as relevant.

Also check:

- `.github/PULL_REQUEST_TEMPLATE.md`
- `.github/workflows/ci.yml`
- `.github/workflows/check-duplicate-flyway.yml`

## Troubleshooting

- App fails on unresolved placeholders: check `.env` values are loaded in the same shell/process.
- DB connection refused: ensure `fact-database` is running and `DB_PORT=5999` when app runs on host.
- 401/403 responses: confirm bearer token is valid and role-appropriate.
- Admin endpoint complains about `X-User-Id`: send a valid existing user UUID in the header.

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE).
