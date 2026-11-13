     # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.10
FROM hmctsprod.azurecr.io/base/java:25-distroless@sha256:f8eccc7e4888c0194a08233cd10cbdc593283c7b707d21d1dc55f01692541372

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-data-api.jar /opt/app/

USER 65532:65532

EXPOSE 8989
CMD [ "fact-data-api.jar" ]
