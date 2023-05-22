FROM ghcr.io/graalvm/graalvm-ce:ol9-java17-22.3.2 AS buildEnv
RUN gu install native-image

WORKDIR /workdir
COPY ${project.artifactId}-${project.version}-fat.jar ./portal-gateway.jar

# copy reachability metadata and reflection configuration
# https://www.graalvm.org/22.3/reference-manual/native-image/metadata/#specifying-metadata-with-json
# https://www.graalvm.org/22.3/reference-manual/native-image/dynamic-features/Reflection/#manual-configuration
COPY META-INF/native-image .

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
    -Dio.netty.noUnsafe=true \
    -Dfile.encoding=UTF-8 \
    -jar portal-gateway.jar

FROM ${docker.pull.registry}/debian:buster-slim

COPY --from=buildEnv /workdir/portal-gateway /usr/local/bin/portal-gateway

COPY portal-gateway /etc/portal-gateway/default/

ENV TZ="Europe/Zurich"

ENTRYPOINT ["portal-gateway", "run", "-cluster"]
