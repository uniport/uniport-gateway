FROM ghcr.io/graalvm/graalvm-ce:java11-21.1.0 AS buildEnv
RUN gu install native-image

ENV VERSION=1.2.0
WORKDIR /workdir
COPY server/target/server-${VERSION}-SNAPSHOT-fat.jar ./portal-gateway.jar
RUN native-image \
    --no-server \
    --no-fallback \
    --allow-incomplete-classpath \
    --initialize-at-build-time=org.slf4j.LoggerFactory \
    --initialize-at-run-time=io.netty.bootstrap \
    --initialize-at-run-time=io.netty.buffer \
    --initialize-at-run-time=io.netty.channel \
    --initialize-at-run-time=io.netty.handler \
    --initialize-at-run-time=io.netty.resolver \
    --initialize-at-run-time=io.netty.util \
    --initialize-at-run-time=org.slf4j.impl.StaticLoggerBinder \
    -H:+ReportExceptionStackTraces \
    # -H:+ReportUnsupportedElementsAtRuntime \
    -Dio.netty.noUnsafe=true \
    -Dfile.encoding=UTF-8 \
    -jar portal-gateway.jar 

FROM debian:buster-slim
COPY --from=buildEnv /workdir/portal-gateway portal-gateway
COPY server/portal-gateway /etc/portal-gateway
RUN ldd portal-gateway

EXPOSE 20000
CMD ["./portal-gateway", "run", "-cluster"]