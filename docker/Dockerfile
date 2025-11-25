# Copy of the default Dockerfile, but with an available shell and a wait-for-it
# script to delay java startup until the database is up and running

# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.6
# Had to switch to temurin so that I had a shell
FROM eclipse-temurin:21

RUN mkdir /opt/app

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-data-api.jar /opt/app/
COPY docker/wait-for-it.sh /opt/app/

RUN chmod +x /opt/app/wait-for-it.sh

EXPOSE 8989
CMD [ "/opt/app/wait-for-it.sh", "fact-database:5432", "-t", "180", "--", "java", "-jar", "/opt/app/fact-data-api.jar" ]
