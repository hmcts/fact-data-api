 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.9
FROM hmctsprod.azurecr.io/base/java:21-distroless@sha256:e482241bc82af2ec3de1a8061b71b384d3a99628d26a2daa594435cb5b7e975d

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-data-api.jar /opt/app/

USER 65532:65532

EXPOSE 8989
CMD [ "fact-data-api.jar" ]
