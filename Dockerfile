 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.10
FROM hmctsprod.azurecr.io/base/java:21-distroless@sha256:2d1ea898f78a89b3a4dd0cb64349072c08c3b987bf5aa8798d749d467a59aa78

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-data-api.jar /opt/app/

USER 65532:65532

EXPOSE 8989
CMD [ "fact-data-api.jar" ]
