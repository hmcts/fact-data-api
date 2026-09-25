 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.10
FROM hmctsprod.azurecr.io/base/java:25-distroless@sha256:5c2a7acb36e151a6eb889b38f4a5ceedff143c3f237fa8ffef13773a524ae9e0

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-data-api.jar /opt/app/

USER 65532:65532

EXPOSE 8989
CMD [ "fact-data-api.jar" ]
