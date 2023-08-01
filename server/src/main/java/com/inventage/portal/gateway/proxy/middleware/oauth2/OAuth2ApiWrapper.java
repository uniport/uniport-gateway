package com.inventage.portal.gateway.proxy.middleware.oauth2;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.ClusterSerializable;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * We require to be able to refresh a user and logout a user.
 * 
 * For this, implementations of the {@link OAuth2Auth} interface i.e. {@link OAuth2AuthProviderImpl} are used.
 * 
 * We also need to be able to synchronize an instance of {@link OAuth2AuthProviderImpl} within a cluster.
 * 
 * Unfortunately, the {@link OAuth2Auth} interface does not need to be {@link ClusterSerializable}, 
 * so this does not come out of the box.
 * 
 * Hence, we create a wrapper around {@link OAuth2AuthProviderImpl} to make it {@link ClusterSerializable} and 
 * only serialize the important {@link OAuth2Options} config.
 * </pre>
 */
public class OAuth2ApiWrapper implements ClusterSerializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2ApiWrapper.class);

    private OAuth2Options config;
    private OAuth2Auth delegate;

    public OAuth2ApiWrapper() {
        // do not remove: constructor required by AbstractSession#readDataFromBuffer for ClusterSerializable
    }

    public OAuth2ApiWrapper(JsonObject json) {
        this.config = new OAuth2Options(json);
    }

    OAuth2ApiWrapper(OAuth2Auth delegate) {
        Objects.requireNonNull(delegate);
        this.delegate = delegate;

        if (delegate instanceof OAuth2AuthProviderImpl) {
            this.config = ((OAuth2AuthProviderImpl) delegate).getConfig();
        } else {
            // this is required for testing
            this.config = new OAuth2Options();
        }
    }

    OAuth2Auth getDelegate(Vertx vertx) {
        if (this.delegate == null) {
            // in case we got synchronized between instances, we need to create the delegate
            this.delegate = OAuth2Auth.create(vertx, config);
        }
        return delegate;
    }

    public JsonObject toJson() {
        return config.toJson();
    }

    @Override
    public void writeToBuffer(Buffer buffer) {
        config.toJson().writeToBuffer(buffer);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        final JsonObject json = new JsonObject();
        final int read = json.readFromBuffer(pos, buffer);
        this.config = new OAuth2Options(json);
        return read;
    }
}