# Functional Tests

Functional tests for the Court and Photo controllers. These are end-to-end HTTP tests that run against a live application instance.

## Prerequisites

1. Application running on configured URL (default: `http://localhost:8989`)
2. PostgreSQL database accessible (default: `localhost:5999`)
3. Environment variables configured (see below)

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

Make sure the application and database are running first as per documentation.

Then run the functional tests:

```bash
./gradlew functional
```

## Pipeline Configuration

In the pipeline, Helm automatically injects environment variables.

## Structure

- `config/` - Configuration loading from environment variables
- `controllers/` - Test classes for each controller
- `data/` - Test data POJOs and builders
- `helpers/` - Helper utilities (database access, test data creation)
- `http/` - HTTP client wrapper

## Test Approach

Tests follow a simple, modular pattern:

1. **Data Builders** - Use `CourtTestDataBuilder` to create test data with sensible defaults
2. **Helpers** - Use `TestDataHelper` to create data via API endpoints
3. **No Fixtures** - Each test creates what it needs
4. **No Cleanup** - Database is wiped between runs in a pipeline
