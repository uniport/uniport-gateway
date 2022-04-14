FROM ghcr.io/graalvm/graalvm-ce:java11-21.1.0 AS buildEnv
RUN gu install native-image

WORKDIR /workdir
COPY ${project.artifactId}-${project.version}-fat.jar ./portal-gateway.jar
# reflection configuration so that logback can use io.reactiverse.contextual.logging.LogbackConverter
# https://www.graalvm.org/21.3/reference-manual/native-image/Reflection/#manual-configuration
COPY META-INF/native-image/reflection-config.json ./reflection-config.json

# https://www.graalvm.org/reference-manual/native-image/Options/#options-to-native-image-builder
RUN native-image \
    --no-server \
    --no-fallback \
    --allow-incomplete-classpath \
    --enable-url-protocols=http \
    --initialize-at-build-time=org.slf4j.LoggerFactory \
    --initialize-at-run-time=io.netty.bootstrap \
    --initialize-at-run-time=io.netty.buffer \
    --initialize-at-run-time=io.netty.channel \
    --initialize-at-run-time=io.netty.handler \
    --initialize-at-run-time=io.netty.resolver \
    --initialize-at-run-time=io.netty.util \
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
