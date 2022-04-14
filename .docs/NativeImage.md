# Native Image mit GraalVM

## Status

The native-image currently works with the File provider only.

## Build

First, a JAR with all dependencies is needed. This can be created as a Uber-JAR with `maven-shade-plugin`.

```bash
mvn clean package
```

To successfully compile a JAR to a binary, we need to configure the GraalVM native image tooling to work with reflection, incomplete classpath issues and unsafe memory access.
This configuration needs to be done for our app and all dependencies. This is the hairy part. Luckily, [GraalVM provides a native-image-agent](https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#assisted-configuration-of-native-image-builds) to help creating these configurations.

Unfortunately, this step requires
* [GraalVM](https://www.graalvm.org/docs/getting-started/#install-graalvm) and
* [Native Image](https://www.graalvm.org/22.0/docs/getting-started/#native-images)

installed locally.

The following commands assume you are in the project root folder.

The following generates several configuration files and stores them in a place where they are included in  future Uber-JARs once the process is terminated. In this stage as many paths throught the code as possible should be triggered. This way all needed configuration can be generated.
**Note**: Instead of `config-output-dir` one can use `config-merge-dir` to merge configurations of multiple runs.

```bash
export PORTAL_GATEWAY_JSON=server/portal-gateway/portal-gateway.json
${GRAALVM_HOME}/bin/java \
-agentlib:native-image-agent=config-output-dir=server/src/main/resources/META-INF/native-image/com.inventage.portal.gateway/portal-gateway/reflect-config \
-jar server/target/server-1.4.0-SNAPSHOT-fat.jar run -cluster
```

Now we can finally build the native image
**Note**: A Dockerfile is used here instaed of the `jib-maven-plugin`, because AFAIK there is no multi stage build feature in this plugin. The multi-stage feature is needed to keep the final docker images as slim as possible.

```bash
mvn clean install
```

## Run

```bash
docker-compose \
-f docker-compose/target/portal-gateway/docker-compose.yml \
-f docker-compose/target/portal-gateway/docker-compose.override.yml \
-f docker-compose/target/portal-gateway/docker-compose.native.yml \
up
```

## Helpful links

* [Proof of Concept: Vert.x Web app compiled with GraalVM](https://github.com/fbuetler/vertx-graalvm-native-image-test)
* [Native Image Build Configuration](https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/)
* [Vert.x native image with GraalVM 19.3.0+](https://blog.termian.dev/posts/vertx-native-image-graalvm/)
* [Instant Netty Startup using GraalVM Native Image Generation](https://medium.com/graalvm/instant-netty-startup-using-graalvm-native-image-generation-ed6f14ff7692)
* [Updates on Class Initialization in GraalVM Native Image Generation](https://medium.com/graalvm/updates-on-class-initialization-in-graalvm-native-image-generation-c61faca461f7)
* [GraalVM: Native Images in Containers](https://cdn.app.compendium.com/uploads/user/e7c690e8-6ff9-102a-ac6d-e4aebca50425/34d3828b-c12e-4c7e-8942-b6b7deb02e12/File/913f2b04a820c11be614de5b371ad626/graalvm__native_images_in_containers.pdf)
* [Vert.x native image awesomeness!](https://www.jetdrone.xyz/2018/08/10/Vertx-native-image-10mb.html)
* [Trust me! SSL works on native images](https://www.jetdrone.xyz/2019/04/16/Full-SSL-Trust-in-Native-Images.html)
* [Fix for io.netty.channel.socket.nio.NioSocketChannel: NioSocketChannel does not have a public non-arg constructor](https://github.com/micronaut-projects/micronaut-core/issues/2516#issuecomment-566184883)
