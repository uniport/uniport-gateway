package ch.uniport.gateway.proxy.middleware;

import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.concurrent.CompletionStage;

/**
 *
 */
public class BrowserConnected {

    private final MiddlewareServer portalGateway;

    public BrowserConnected(MiddlewareServer portalGateway) {
        this.portalGateway = portalGateway;
    }

    public CompletionStage<HttpClientResponse> request(HttpMethod method, String uri) {
        return request(method, uri, new HeadersMultiMap());
    }

    public CompletionStage<HttpClientResponse> request(HttpMethod method, String uri, MultiMap headers) {
        Promise<HttpClientResponse> result = Promise.promise();
        portalGateway.incomingRequest(method, uri, new RequestOptions().setHeaders(headers), result::complete);
        return result.future().toCompletionStage();
    }

    public CompletionStage<HttpClientResponse> request(HttpMethod method, String uri, MultiMap headers, String body) {
        Promise<HttpClientResponse> result = Promise.promise();
        portalGateway.incomingRequest(method, uri, new RequestOptions().setHeaders(headers), body, result::complete);
        return result.future().toCompletionStage();
    }
}
