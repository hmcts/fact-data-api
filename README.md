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

## Quick start (host app + Docker dependencies)

This is the most common workflow for local development.

1. Create a local `.env` file in the repository root with the values below:

| Variable | Required | Example | Purpose |
| --- | --- | --- | --- |
| `DB_HOST` | yes | `localhost` | Host app to Docker Postgres connection host |
| `DB_PORT` | yes | `5999` | Host app to Docker Postgres port |
| `DB_NAME` | yes | `fact` | Database name |
| `DB_USER` | yes | `fact` | Database username |
| `DB_PASSWORD` | yes | `fact` | Database password |
| `AZURE_TENANT_ID` | yes | `<tenant-id>` | Azure AD tenant for token validation |
| `AZURE_CLIENT_ID` | yes | `<client-id>` | Azure AD client ID |
| `APP_REG_ID` | yes | `<app-registration-id>` | Azure AD app ID URI |
| `AZURE_STORAGE_ACCOUNT_NAME` | yes | `devstoreaccount1` | Blob storage account |
| `AZURE_STORAGE_CONNECTION_STRING` | yes | `DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=;BlobEndpoint=http://localhost:10000/devstoreaccount1;` | Blob storage connection for host-run app |
| `OS_KEY` | yes | `<ordnance-survey-key>` | Ordnance Survey API key |
| `AZURE_MANAGED_IDENTITY_ENABLED` | no | `false` | Disable managed identity locally |
| `TESTING_SUPPORT_ENABLE_API` | no | `true` | Enable `/testing-support/**` endpoints |
| `RUN_DB_MIGRATION_ON_STARTUP` | no | `true` | Run Flyway migrations on startup |
| `AZURITE_ACCOUNTS` | yes | `devstoreaccount1:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=` | Local Azurite account and key |
| `DOCKER_AZURE_STORAGE_CONNECTION_STRING` | yes | `DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=;BlobEndpoint=http://fact-storage:10000/devstoreaccount1;` | Blob storage connection used by Docker services |

2. Start PostgreSQL, local blob storage, and the storage bootstrap container:

```bash
docker compose up -d fact-database fact-storage fact-storage-init
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
./deploy_local_docker.sh
```

`deploy_local_docker.sh` requires a `.env` file, runs `./gradlew clean build` first, then starts Docker with `docker compose --env-file .env up --build -d`.
It expects `DOCKER_AZURE_STORAGE_CONNECTION_STRING` and `AZURITE_ACCOUNTS` in `.env`, and runs `fact-storage-init` to create `photos` and `csv` containers during startup.

## Running from IntelliJ

1. Open the project.
2. Ensure env vars from `.env` are applied to the run configuration.
3. Start `fact-database`, `fact-storage`, and `fact-storage-init` with Docker as shown above.
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
