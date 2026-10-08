 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.10
FROM hmctsprod.azurecr.io/base/java:25-distroless@sha256:b81b53d40e3852cce93a93336c58f3f900411d98a6eb1b3a30ede30ded163115

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-data-api.jar /opt/app/

USER 65532:65532

EXPOSE 8989
CMD [ "fact-data-api.jar" ]
