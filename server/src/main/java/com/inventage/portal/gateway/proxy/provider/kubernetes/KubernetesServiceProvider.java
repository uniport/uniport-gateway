package com.inventage.portal.gateway.proxy.provider.kubernetes;

import com.inventage.portal.gateway.core.config.StaticConfiguration;
import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;
import com.inventage.portal.gateway.proxy.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.kubernetes.KubernetesServiceImporter;

/**
 * Generates a complete dynamic configuration from announcements about created/removed kubernetes
 * services.
 */
public class KubernetesServiceProvider extends Provider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesServiceProvider.class);

    private Vertx vertx;

    private EventBus eb;
    private String configurationAddress;

    private ServiceDiscovery kubernetesDiscovery;

    public KubernetesServiceProvider(Vertx vertx, String configurationAddress) {
        LOGGER.trace("construcutor");
        this.vertx = vertx;
        this.configurationAddress = configurationAddress;
    }

    public void start(Promise<Void> startPromise) {
        LOGGER.trace("start");
        provide(startPromise);
    }

    @Override
    public void provide(Promise<Void> startPromise) {
        LOGGER.trace("provide");
        String announceAddress = "service-announce";

        this.kubernetesDiscovery = ServiceDiscovery.create(vertx, new ServiceDiscoveryOptions()
                .setAnnounceAddress(announceAddress).setName("kubernetes-discovery"));

        // LOCAL KUBERNETES CLUSTER RUNNING WITH MINIKUBE
        // HOST:PORT
        // cat ~/.kube/config
        // TOKEN
        // kubectl get secret default-token-ssxdw -o go-template='{{range $k,$v
        // := .data}}{{"### "}}{{$k}}{{"\n"}}{{$v|base64decode}}{{"\n\n"}}{{end}}'
        // CREATE CLUSTERROLE ALLOWING TO READ SERVICES
        // kubectl create clusterrole service-reader --verb=get,list,watch
        // --resource=services
        // GRANT DEFAULT SERVICE ACCOUNT THOSE PRIVILIGES
        // kubectl create clusterrolebinding service-reader-binding --clusterrole
        // service-reader --serviceaccount default:default

        String host = "192.168.99.102";
        int port = 8443;
        Boolean useTLS = true;
        String token =
                "eyJhbGciOiJSUzI1NiIsImtpZCI6InF4azdOWVhfT0FtME9uUE1zR1ZudUIySVBDWjh5MXgtU1ZQa01FOWxrSHMifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4tcXp0emIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImNlOWNmOTExLWIyODktNDdlYS1iMjYzLWY1NmY4ZGRjMzc0ZSIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.X39cJ6IHhq1OW_uM30vYVEUAjLTKTaVcGavRIu2P-JDlv8rqiG8XyyIqFXzjJaw2K3BT7yVuE-TtAF7cj3VDFFmau1U2EBK9ftp6PIWa5c-ioKHQ1TzQXaNCPeX6QC0z1270lr6uN1dzze5wcc2DQukNR07zTJEk_e7BFLDMuRMWHmbnBLBis1RvxYr4MOTPtG2uYvfQQ8Vo4irdU_bncH55PimG1toVsGPYsk7GyVjVg7Cct-WJQWmZABHM0Fy1TZHGpN4kTbjsmq3P5kHQdEBSC8fbeIyP_-wxoxVYOcd60jBHN-4VBBMy2JSnPs89Q-r-sQFtddWPMiNre6HI7Q";
        String namespace = "default";

        this.kubernetesDiscovery.registerServiceImporter(new KubernetesServiceImporter(),
                new JsonObject().put("host", host).put("port", port).put("ssl", useTLS)
                        .put("token", token).put("namespace", namespace));

        EventBus eb = vertx.eventBus();
        MessageConsumer<JsonObject> announceConsumer = eb.consumer(announceAddress);
        announceConsumer.handler(message -> {
            JsonObject config = this.buildConfiguration(message.body());
            validateAndPublish(config);
        });
        startPromise.complete();
    }

    private JsonObject buildConfiguration(JsonObject kubernetesService) {
        LOGGER.trace("buildConfiguration");
        return null;
    }

    private void validateAndPublish(JsonObject config) {
        LOGGER.trace("validateAndPublish");
        DynamicConfiguration.validate(this.vertx, config).onComplete(ar -> {
            if (ar.succeeded()) {
                LOGGER.info("validateAndPublish: configuration published");
                this.eb.publish(this.configurationAddress,
                        new JsonObject()
                                .put(Provider.PROVIDER_NAME,
                                        StaticConfiguration.PROVIDER_KUBERNETES)
                                .put(Provider.PROVIDER_CONFIGURATION, config));
            } else {
                LOGGER.warn("validateAndPublish: cannot publish invalid configuration");
            }
        });
    }
}
