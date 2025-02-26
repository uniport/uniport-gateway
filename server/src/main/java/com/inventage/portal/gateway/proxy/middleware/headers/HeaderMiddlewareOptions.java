package com.inventage.portal.gateway.proxy.middleware.headers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inventage.portal.gateway.proxy.model.GatewayMiddlewareOptions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeaderMiddlewareOptions implements GatewayMiddlewareOptions {

    @JsonProperty(HeaderMiddlewareFactory.HEADERS_REQUEST)
    private Map<String, Object> requestHeaders;

    @JsonProperty(HeaderMiddlewareFactory.HEADERS_RESPONSE)
    private Map<String, Object> responseHeaders;

    public HeaderMiddlewareOptions() {
    }

    public Map<String, List<String>> getRequestHeaders() {
        return requestHeaders == null ? null : copyHeaders(requestHeaders);
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders == null ? null : copyHeaders(responseHeaders);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> copyHeaders(Map<String, Object> headers) {
        final Map<String, List<String>> map = new HashMap<>();
        for (Entry<String, Object> entry : headers.entrySet()) {
            final String name = entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof String) {
                map.put(name, List.of(((String) value)));
            } else if (entry.getValue() instanceof List) {
                map.put(name, List.copyOf((List<String>) value));
            } else {
                throw new RuntimeException("invalid type");
            }
        }
        return map;
    }

    @Override
    public HeaderMiddlewareOptions clone() {
        try {
            final HeaderMiddlewareOptions options = (HeaderMiddlewareOptions) super.clone();
            options.requestHeaders = requestHeaders == null ? null : Map.copyOf(requestHeaders);
            options.responseHeaders = responseHeaders == null ? null : Map.copyOf(responseHeaders);
            return options;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
