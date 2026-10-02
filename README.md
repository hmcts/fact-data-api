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

## IntelliJ Dev Container

The repository includes an IntelliJ-first [Dev Container](https://containers.dev/) with Java 21, Gradle, Docker for Testcontainers, Terraform formatting support, GitHub Copilot CLI, PostgreSQL 17, and Azurite. Docker must be running and the local `.env` file described below must exist before the container is created.

To open the existing checkout in the container:

1. Open the repository in IntelliJ IDEA.
2. Open `.devcontainer/devcontainer.json`.
3. Click the Dev Container gutter action and select **Create Dev Container and Mount Sources**.
4. Select the `fact-data-api` configuration, then choose **Build Container and Continue**.
5. Wait for IntelliJ to start its backend in the container and reconnect through JetBrains Client.

Use **Mount Sources** for normal development so changes remain in the existing local checkout. The **Clone Sources** option creates a separate checkout and is not needed for this configuration.

The Gradle wrapper and Java 21 toolchain run inside the container. PostgreSQL and Azurite start automatically and are available to the application at `fact-database:5432` and `fact-storage:10000`. Common commands can be run from IntelliJ's terminal or Gradle tool window:

```bash
./gradlew test
./gradlew integration
./gradlew bootRun
```

GitHub Copilot is available both as the IntelliJ plugin and as the `copilot` terminal command. Run `copilot` and use `/login` when prompted to authenticate the CLI.

The ignored `.env` file is loaded into the development container. Its Docker storage connection is mapped to `AZURE_STORAGE_CONNECTION_STRING` inside the IntelliJ backend, while the host-run connection remains available for development outside the container. Only use Terraform locally for formatting:

```bash
terraform -chdir=infrastructure fmt -recursive
```

Run Terraform initialization, validation, and planning through Jenkins. After changing either file under `.devcontainer`, use IntelliJ's **Rebuild Dev Container** action.

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
| `AZURE_STORAGE_CONNECTION_STRING` | yes | `DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=<local-azurite-key>;BlobEndpoint=http://localhost:10000/devstoreaccount1;` | Blob storage connection for host-run app |
| `OS_KEY` | yes | `<ordnance-survey-key>` | Ordnance Survey API key |
| `AZURE_MANAGED_IDENTITY_ENABLED` | no | `false` | Disable managed identity locally |
| `TESTING_SUPPORT_ENABLE_API` | no | `true` | Enable `/testing-support/**` endpoints |
| `RUN_DB_MIGRATION_ON_STARTUP` | no | `true` | Run Flyway migrations on startup |
| `AZURITE_ACCOUNTS` | yes | `devstoreaccount1:<local-azurite-key>` | Local Azurite account and key |
| `DOCKER_AZURE_STORAGE_CONNECTION_STRING` | yes | `DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=<local-azurite-key>;BlobEndpoint=http://fact-storage:10000/devstoreaccount1;` | Blob storage connection used by Docker services |

Generate an emulator-only key and add the matching storage values to `.env`:

```bash
AZURITE_KEY="$(openssl rand -base64 32)"
cat >> .env <<EOF
AZURITE_ACCOUNTS=devstoreaccount1:${AZURITE_KEY}
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=${AZURITE_KEY};BlobEndpoint=http://localhost:10000/devstoreaccount1;
DOCKER_AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=${AZURITE_KEY};BlobEndpoint=http://fact-storage:10000/devstoreaccount1;
EOF
```

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

## Azure API Management

Terraform deploys the APIM-facing operations under `/fact` in the shared CFT API Management instances for AAT, demo, ITHC, perftest, and production. The public endpoint has the form:

```text
https://cft-api-mgmt.<environment>.platform.hmcts.net/fact
```

APIM requires a valid Microsoft Entra ID bearer token for the FACT API audience with the `Role.Fact.Admin` app role. It validates the token and forwards the original `Authorization` header to `fact-data-api`, where Spring Security validates the same token again. APIM subscription keys are not required.

The API definition is published to [cnp-api-docs](https://hmcts.github.io/cnp-api-docs/specs/fact-data-api-apim.json). Terraform downloads the document content during each Jenkins deployment, so a changed published specification updates APIM on the next Terraform run. The OpenAPI publication and Jenkins deployment run independently; if publication finishes after the merge deployment, the following Jenkins run reconciles APIM.

The app registration is managed outside this repository. Each environment must provide:

- `api-app-reg-id` in `fact-kv-<environment>`, containing the FACT API audience.
- The `Role.Fact.Admin` app role and the required role assignments.
- Jenkins access to the environment's shared CFT APIM subscription.

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

### Requirements for public frontend after migration changes

When a new migration is applied, the public frontend pods may need to be restarted to pick up the new changes. This is because the frontend pods may have cached data or schema information that is no longer valid after the migration. To ensure that the frontend pods are using current data, the frontend pods should be refreshed in a controlled manner that ensures continuous service.

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
