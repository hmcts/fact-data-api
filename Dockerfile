     # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.10
FROM hmctsprod.azurecr.io/base/java:25-distroless@sha256:59d16ce505aecd7071b1ba9d867f68a45653bd4afdab204a4d84345fa96c6fc4

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-data-api.jar /opt/app/

USER 65532:65532

EXPOSE 8989
CMD [ "fact-data-api.jar" ]
