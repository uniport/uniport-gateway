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
    --initialize-at-build-time=org.slf4j.simple.SimpleLogger \
    --initialize-at-run-time=io.netty.bootstrap.ServerBootstrap \
    --initialize-at-run-time=io.netty.buffer.AbstractByteBuf \
    --initialize-at-run-time=io.netty.buffer.AbstractByteBufAllocator \
    --initialize-at-run-time=io.netty.buffer.AdvancedLeakAwareByteBuf \
    --initialize-at-run-time=io.netty.buffer.ByteBufUtil$HexUtil \
    --initialize-at-run-time=io.netty.buffer.ByteBufUtil$HexUtil \
    --initialize-at-run-time=io.netty.buffer.Unpooled \
    --initialize-at-run-time=io.netty.channel.AbstractChannel \
    --initialize-at-run-time=io.netty.channel.AbstractChannelHandlerContext \
    --initialize-at-run-time=io.netty.channel.ChannelHandlerMask \
    --initialize-at-run-time=io.netty.channel.ChannelInitializer \
    --initialize-at-run-time=io.netty.channel.ChannelOutboundBuffer \
    --initialize-at-run-time=io.netty.channel.CombinedChannelDuplexHandler \
    --initialize-at-run-time=io.netty.channel.DefaultChannelId \
    --initialize-at-run-time=io.netty.channel.DefaultChannelPipeline \
    --initialize-at-run-time=io.netty.channel.epoll.Epoll \
    --initialize-at-run-time=io.netty.channel.epoll.Native \
    --initialize-at-run-time=io.netty.channel.epoll.EpollEventLoop \
    --initialize-at-run-time=io.netty.channel.epoll.EpollEventArray \
    --initialize-at-run-time=io.netty.channel.DefaultFileRegion \
    --initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray \
    --initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop \
    --initialize-at-run-time=io.netty.channel.kqueue.Native \
    --initialize-at-run-time=io.netty.channel.unix.Errors \
    --initialize-at-run-time=io.netty.channel.unix.IovArray \
    --initialize-at-run-time=io.netty.channel.unix.Limits \
    --initialize-at-run-time=io.netty.handler.codec.http.HttpObjectAggregator \ 
    --initialize-at-run-time=io.netty.handler.codec.http.HttpObjectEncoder \
    --initialize-at-run-time=io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder \
    --initialize-at-run-time=io.netty.handler.codec.http2.Http2CodecUtil \
    --initialize-at-run-time=io.netty.handler.codec.http2.DefaultHttp2FrameWriter \
    --initialize-at-run-time=io.netty.handler.ssl.ConscryptAlpnSslEngine \
    --initialize-at-run-time=io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator \
    --initialize-at-run-time=io.netty.handler.ssl.JettyAlpnSslEngine$ClientEngine \
    --initialize-at-run-time=io.netty.handler.ssl.JettyAlpnSslEngine$ServerEngine \
    --initialize-at-run-time=io.netty.handler.ssl.JettyNpnSslEngine \
    --initialize-at-run-time=io.netty.handler.ssl.OpenSslSessionContext \
    --initialize-at-run-time=io.netty.handler.ssl.ReferenceCountedOpenSslContext \
    --initialize-at-run-time=io.netty.handler.ssl.ReferenceCountedOpenSslEngine \
    --initialize-at-run-time=io.netty.util.concurrent.DefaultPromise \
    --initialize-at-run-time=io.netty.util.internal.CleanerJava9 \
    --initialize-at-run-time=io.netty.util.internal.CleanerJava6 \
    --initialize-at-run-time=io.netty.util.internal.logging.Log4JLogger \
    --initialize-at-run-time=io.netty.util.internal.MacAddressUtil \
    --initialize-at-run-time=io.netty.util.internal.PlatformDependent \
    --initialize-at-run-time=io.netty.util.internal.PlatformDependent0 \
    --initialize-at-run-time=io.netty.util.internal.SystemPropertyUtil \
    --initialize-at-run-time=io.netty.util.internal.StringUtil \
    --initialize-at-run-time=io.netty.util.ReferenceCountUtil \
    --initialize-at-run-time=io.netty.util.ResourceLeakDetector \
    --initialize-at-run-time=io.netty.util.ResourceLeakDetectorFactory \
    --initialize-at-run-time=io.netty.util.ThreadDeathWatcher \
    --initialize-at-run-time=org.slf4j.impl.StaticLoggerBinder \
    --trace-class-initialization=ch.qos.logback.classic.Logger \
    -H:+ReportExceptionStackTraces \
    # -H:+ReportUnsupportedElementsAtRuntime \
    -Dio.netty.noUnsafe=true \
    -Dfile.encoding=UTF-8 \
    -jar portal-gateway.jar 

FROM debian:buster-slim
COPY --from=buildEnv /workdir/portal-gateway portal-gateway
RUN ldd portal-gateway

EXPOSE 20000
CMD ["./portal-gateway"]
# CMD ["./portal-gateway", "run", "com.inventage.portal.gateway.core.PortalGatewayVerticle", "cluster"]