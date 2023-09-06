FROM ghcr.io/graalvm/graalvm-ce:ol9-java17-22.3.2 AS buildEnv
RUN gu install native-image

WORKDIR /workdir
COPY ${project.artifactId}-${project.version}-fat.jar ./portal-gateway.jar

# copy reachability metadata and reflection configuration
# https://www.graalvm.org/22.3/reference-manual/native-image/metadata/#specifying-metadata-with-json
# https://www.graalvm.org/22.3/reference-manual/native-image/dynamic-features/Reflection/#manual-configuration
COPY native-image .

# https://www.graalvm.org/22.3/reference-manual/native-image/overview/BuildOptions/
RUN native-image \
    --verbose \
    --no-fallback \
    --enable-url-protocols=http \
    --initialize-at-build-time=org.slf4j.LoggerFactory \
    --initialize-at-build-time=ch.qos.logback \
    --initialize-at-run-time=io.netty \
    --initialize-at-run-time=org.slf4j.impl.StaticLoggerBinder \
    --initialize-at-run-time=io.grpc.netty.shaded.io.netty \
    -H:+ReportExceptionStackTraces \
    -H:IncludeResources='.*/vertx/.*$' \
    -H:ReflectionConfigurationFiles=reflection-config.json \
    -H:ConfigurationFileDirectories=com.inventage.portal.gateway/portal-gateway/reflect-config \
    -Dio.netty.noUnsafe=true \
    -Dfile.encoding=UTF-8 \
    -jar portal-gateway.jar

# https://www.graalvm.org/22.0/reference-manual/native-image/StaticImages/#what-is-the-recommended-base-docker-image-for-deploying-a-static-or-mostly-static-native-image
FROM debian:bookworm-slim

COPY --from=buildEnv /workdir/portal-gateway /usr/local/bin/portal-gateway

# In our logback configuration we use a condition based on a env var to decide if we log structured or unstructered log messages.
# For this we rely on a library called Janino.
# See server/src/main/resources/logback.xml.
#
# Unforuntately, Janino cannot be used with a native image build, hence we have two logback configurations for both cases.
# To choose between the two configurations the PORTAL_GATEWAY_LOGGING_CONFIG env var can be used.
# See: https://github.com/spring-projects/spring-boot/issues/33758
COPY logback-structured-default.xml /etc/portal-gateway/default/
COPY logback-unstructured-default.xml /etc/portal-gateway/default/

ENV TZ="Europe/Zurich"

RUN groupadd --system appuser && useradd --system --gid appuser appuser
USER appuser

ENTRYPOINT ["portal-gateway"]
