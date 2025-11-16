# syntax=docker/dockerfile:1.7
ARG GRADLE_IMAGE=gradle:8.11
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${GRADLE_IMAGE} AS build
WORKDIR /workspace
COPY . .
ARG SERVICE_NAME
RUN gradle --no-daemon :services:${SERVICE_NAME}:installDist

FROM ${RUNTIME_IMAGE} AS runtime
ARG SERVICE_NAME
ARG SERVICE_PORT=8080
WORKDIR /app
COPY --from=build /workspace/services/${SERVICE_NAME}/build/install/${SERVICE_NAME}/ /app/
RUN addgroup --system ktor \
    && adduser --system --ingroup ktor ktor \
    && chown -R ktor:ktor /app \
    && printf '#!/bin/sh\nexec /app/bin/%s "$@"\n' "${SERVICE_NAME}" > /entrypoint.sh \
    && chmod +x /entrypoint.sh
USER ktor
EXPOSE ${SERVICE_PORT}
ENTRYPOINT ["/entrypoint.sh"]
