# FaCT Data API
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Afact-data-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Afact-data-api)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Afact-data-api&metric=coverage)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Afact-data-api)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Afact-data-api&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Afact-data-api)

This is a Java springboot API used in the Find a Court or Tribunal service.

## Deploying the application locally

There are two supported ways to deploy the application locally

### Using IntelliJ

1. Ensure you have Java 21 installed on your machine.
2. Clone the repository to your local machine.
3. Open the project in IntelliJ.
4. Start the local database with `docker-compose up fact-database -d` from the terminal in the project root directory. Postgres will be exposed on port `5999`
5. Run the `Application` class in [Application.java](src/main/java/uk/gov/hmcts/reform/fact/data/api/Application.java).
6. You may need environment variables set up for the application to run correctly. You can set these in Intellij by going to `Run > Edit Configurations > Environment Variables`.
7. The application should now be running and accessible at `http://localhost:8989`.

### Using Docker
1. Ensure you have Docker installed and running on your machine.
2. Clone the repository to your local machine.
3. Navigate to the project directory in your terminal.
4. Run the following command to build the local code into a Docker image and deploy the database
   ```bash
   ./deploy_local_docker.sh
   ```
5. The application should now be running and accessible at `http://localhost:8989`.

## Legacy data migration

The `migration` sub-module exposes a helper endpoint that imports data from the legacy FaCT application.

- Configure the legacy API base URL via the `FACT_MIGRATION_SOURCE_BASE_URL` environment variable (defaults to `http://localhost:8080`). Values can be stored in a local `.env` file.
- Execute the migration by calling `POST /migration/import`. The endpoint deserialises the legacy export payload and stores it through the new JPA entities.
- The migration is designed to run once. Subsequent calls return `409 Conflict`, ensuring the legacy endpoint is not invoked again unless the database is reset manually.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
