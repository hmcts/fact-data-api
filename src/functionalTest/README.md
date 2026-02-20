# Functional Tests

Functional tests for the Court and Types controllers. These are end-to-end HTTP tests that run against a live application instance.

## Prerequisites

1. Application running on configured URL (default: `http://localhost:8989`)
2. Environment variables configured:
   - `TEST_URL` (optional)
   - `APP_REG_ID`
   - `AZURE_TENANT_ID`
   - `ADMIN_CLIENT_APP_REG_ID`
   - `VIEWER_CLIENT_APP_REG_ID`
   - `ADMIN_AZURE_CLIENT_SECRET`
   - `VIEWER_AZURE_CLIENT_SECRET`

## Local Development Setup

### Option 1: Create .env file

1. Create or Update values in `.env`

2. Load the .env file before running tests:
   ```bash
   export $(cat .env | xargs)
   ./gradlew functional
   ```

### Option 2: IntelliJ Run Configuration

1. Open Run/Debug Configurations
2. Select your test configuration
3. Add environment variables in "Environment variables" field.

## Running Tests

Make sure the application is running first as per documentation.

Then run the functional tests:

```bash
./gradlew functional
```

## Pipeline Configuration

In the pipeline, Helm automatically injects environment variables.

## Structure

- `config/` - Configuration loading from environment variables (TEST_URL)
- `controllers/` - Test classes for each controller
- `helpers/` - Helper utilities (TestDataHelper for fetching reference data, AssertionHelper for common assertions)
- `http/` - HTTP client wrapper

## Test Approach


1. **Real Entities** - Use actual domain entities from main source code (e.g., `Court` entity) instead of test POJOs
2. **Static Initialization** - Use `private static final` for HttpClient, ObjectMapper, and reference data
3. **ObjectMapper Configuration** - JavaTimeModule for ZonedDateTime support, disable WRITE_DATES_AS_TIMESTAMPS
4. **Type Safety** - Use strongly typed UUIDs throughout
5. **Bidirectional Mapping** - POJO → JSON for requests, JSON → POJO for response validation
6. **POST + GET Verification** - Create data then verify persistence by fetching it back
7. **Helpers** - `TestDataHelper` fetches reference data via API, `AssertionHelper` provides reusable assertions
8. **Allure Reporting** - @Feature and @DisplayName annotations for test organization
9. **No Fixtures** - Each test creates what it needs
10. **No Cleanup** - Database is wiped between runs in the pipeline
