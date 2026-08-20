 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.9
FROM hmctsprod.azurecr.io/base/java:21-distroless@sha256:477522a26ab135e421e66596741fdf18ee013694173e60cb4430d34741b7a834

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-data-api.jar /opt/app/

USER 65532:65532

EXPOSE 8989
CMD [ "fact-data-api.jar" ]
