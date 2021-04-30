FROM ghcr.io/graalvm/graalvm-ce:java11-21.1.0 AS buildEnv
RUN gu install native-image

WORKDIR /workdir
COPY server/target/server-1.1.0-SNAPSHOT-jar-with-dependencies.jar ./fat-jar.jar
COPY reflectconfig.json reflectconfig.json
RUN native-image -cp fat-jar.jar \
    --no-server \
    --no-fallback \
    --enable-all-security-services \
    --allow-incomplete-classpath \
    --report-unsupported-elements-at-runtime \
    --initialize-at-run-time=io.netty \
    -H:ReflectionConfigurationFiles=reflectconfig.json \
    -H:+ReportExceptionStackTraces \
    -H:Name="portal-gateway" \
    com.inventage.portal.gateway.PortalGatewayLauncher

# Here we can take advantage of the multi-stage build and boot from an optimized docker image.
# Depending on how much slimmed down image we choose, we may need to transfer several missing libraries.
# Generally, you can find the required libraries using ldd.
RUN ldd portal-gateway

FROM debian:buster-slim
# FROM gcr.io/distroless/base
# COPY --from=buildEnv /usr/lib64/libz.so.1 /lib/x86_64-linux-gnu/libz.so.1
# COPY --from=buildEnv "/usr/lib64/libstdc++.so.6" "/lib/x86_64-linux-gnu/libstdc++.so.6"
# COPY --from=buildEnv "/usr/lib64/libgcc_s.so.1" "/lib/x86_64-linux-gnu/libgcc_s.so.1"
COPY --from=buildEnv /workdir/portal-gateway portal-gateway

EXPOSE 20000
CMD ["./portal-gateway", "run", "com.inventage.portal.gateway.core.PortalGatewayVerticle", "cluster"]