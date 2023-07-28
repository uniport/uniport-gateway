FROM ${docker.registry}/eclipse-temurin:17-jre

WORKDIR /app

COPY dependency-jars dependency-jars

COPY ${project.artifactId}-${project.version}-slim.jar /app/portal-gateway.jar

RUN groupadd --system appuser && useradd --system --gid appuser appuser
RUN chown -R appuser /app
USER appuser

ENV TZ="Europe/Zurich"

# for using 50% of memory limit as max heap size 
# and for printing setting out on VM startup 
ENV JAVA_OPTS="-XX:MaxRAMPercentage=50.0 -XX:+PrintFlagsFinal"

ENTRYPOINT java $JAVA_OPTS -jar /app/portal-gateway.jar run -cluster