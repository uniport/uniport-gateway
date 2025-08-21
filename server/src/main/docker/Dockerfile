FROM ${docker.registry}/eclipse-temurin:${maven.compiler.release}-jre

LABEL org.opencontainers.image.title="Uniport-Gateway"
LABEL org.opencontainers.image.description="The secure front door to all Uniport services"
LABEL org.opencontainers.image.vendor="Uniport"
LABEL org.opencontainers.image.source="https://github.com/uniport/uniport-gateway"
LABEL org.opencontainers.image.licenses="MIT"

WORKDIR /app

COPY dependency-jars dependency-jars

COPY ${project.artifactId}-${project.version}-slim.jar /app/uniport-gateway.jar

RUN groupadd --system appuser && useradd --system --gid appuser appuser
RUN chown -R appuser /app
USER appuser

# Default location by defined in GatewayLauncher.DEFAULT_LOGGING_CONFIG_FILE_PATH/DEFAULT_STRUCTURED_LOGGING_CONFIG_FILE_NAME
# To choose between the two configurations the UNIPORT_GATEWAY_LOGGING_CONFIG env var can be used.
COPY logback-structured-default.xml /etc/uniport-gateway/logback.xml
COPY logback-unstructured-default.xml /etc/uniport-gateway/logback-unstructured.xml

ENV TZ="Europe/Zurich"

# for using 50% of memory limit as max heap size 
# and for printing setting out on VM startup 
ENV JAVA_OPTS="-XX:MaxRAMPercentage=50.0 -XX:+PrintFlagsFinal"

# provide Hazelcast access to Java internal API
# See https://docs.hazelcast.com/imdg/4.0/getting-started/running-in-modular-java
ENV JAVA_OPTS="$JAVA_OPTS \
    --add-modules java.se \
    --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.nio=ALL-UNNAMED \
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens java.management/sun.management=ALL-UNNAMED \
    --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

# for enabling remote debugging
ENV JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=0.0.0.0:20009,server=y,suspend=n"
# for enabling jmx for jconsole
ENV JAVA_OPTS="$JAVA_OPTS \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.local.only=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.port=9010 \
    -Dcom.sun.management.jmxremote.rmi.port=9010 \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=0.0.0.0"

ENTRYPOINT java $JAVA_OPTS -jar /app/uniport-gateway.jar 