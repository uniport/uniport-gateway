FROM ghcr.io/graalvm/graalvm-ce:java11-21.1.0 AS buildEnv
RUN gu install native-image

WORKDIR /workdir
COPY ${project.artifactId}-${project.version}-fat.jar ./portal-gateway.jar

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
    -H:+ReportExceptionStackTraces \
    -Dio.netty.noUnsafe=true \
    -Dfile.encoding=UTF-8 \
    -jar portal-gateway.jar

FROM gcr.io/distroless/base
COPY --from=buildEnv /workdir/portal-gateway portal-gateway
COPY --from=buildEnv /usr/lib64/libz.so.1 /lib/x86_64-linux-gnu/libz.so.1
COPY --from=buildEnv /usr/lib64/libstdc++.so.6 /lib/x86_64-linux-gnu/libstdc++.so.6
COPY --from=buildEnv /usr/lib64/libgcc_s.so.1 /lib/x86_64-linux-gnu/libgcc_s.so.1

COPY portal-gateway /etc/portal-gateway/default/

CMD ["./portal-gateway", "run", "-cluster"]
