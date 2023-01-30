package com.inventage.portal.gateway.proxy.middleware;

import io.vertx.core.Promise;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

import java.util.concurrent.CompletionStage;

/**
 *
 */
public class BrowserConnected {

    private MiddlewareServer portalGateway;

    public BrowserConnected(MiddlewareServer portalGateway) {
        this.portalGateway = portalGateway;
    }

    public CompletionStage<HttpClientResponse> request(HttpMethod method, String uri) {
        Promise<HttpClientResponse> result = Promise.promise();
        portalGateway.incomingRequest(method, uri, new RequestOptions(), (outgoingResponse) -> {
            result.complete(outgoingResponse);
        });
        return result.future().toCompletionStage();
    }

    public CompletionStage<HttpClientResponse> followRedirect(HttpClientResponse redirectResponse) {
        Promise<HttpClientResponse> result = Promise.promise();
//        portalGateway.incomingRequest(method, uri, new RequestOptions(), (outgoingResponse) -> {
//            result.complete(outgoingResponse);
//        });
        return result.future().toCompletionStage();
    }
}
