package com.inventage.portal.gateway.proxy.service;

import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoService implements Service {

    private static Logger LOGGER = LoggerFactory.getLogger(EchoService.class);

    @Override
    public void handle(HttpServerRequest outboundRequest) {
        LOGGER.debug("handle: request uri '{}'", outboundRequest.uri());
        outboundRequest.response().end();
    }
}

//<pre style="word-wrap: break-word; white-space: pre-wrap;">{
//        "headers": {
//        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
//        "Accept-Encoding": "gzip, deflate, br",
//        "Accept-Language": "en-GB,en;q=0.9",
//        "Authorization": "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ1Y0p4dWNXRDFWZnFSNU56QmtKZng2RnNZYmJHeEcxOHk5bVZrazFYYWJZIn0.eyJleHAiOjE2MTQwMzMwMzIsImlhdCI6MTYxNDAxNjkzOCwiYXV0aF90aW1lIjoxNjEzOTg5ODMyLCJqdGkiOiI3Zjc2NDQyZi04NzM4LTRlYmItYWQ3YS01ZjM2NzY3MmQyODUiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwMDAvYXV0aC9yZWFsbXMvcG9ydGFsIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImY6OWY3Zjk1YzEtMWYwZS00NjhkLTg2MzUtYWRlYzljZTYyODUxOjMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJPcmdhbmlzYXRpb24iLCJzZXNzaW9uX3N0YXRlIjoiNmFhOTI3NjItODM0Ni00NjMyLWIzNmMtNjllY2M3Y2JlYTdmIiwiYWNyIjoiMCIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJodHRwczovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLXVzZXItaWQiOiJ1c2VyMSIsIngtaGFzdXJhLWRlZmF1bHQtcm9sZSI6IiIsIngtaGFzdXJhLWFsbG93ZWQtcm9sZXMiOltdfSwibmFtZSI6Ik9uZSBVc2VyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlcjEiLCJnaXZlbl9uYW1lIjoiT25lIiwiZmFtaWx5X25hbWUiOiJVc2VyIiwiZW1haWwiOiJ1c2VyMUBpbnZlbnRhZ2UuY29tIn0.KbUq51FOB9PbfRTpCeuzCKbONIK30ArQ-TJ0LPn_6a-JxQoKe4NDfa9ACBdxoEI6PrP0rpHvmA4P0TzntqE8nY2A_Ytro3u6xlXx7bjwOVGJwHuom1Fk-3dVAeAaDAmZpVBURoGgqHP5zdvN3Xpc7SXzjau7OnS8TyzFuGAusD7R5XG00pRKZCGF65tRdHNMOXgQMeCwKFQ54MzYirx0IpAGyc-QRM8ducOuS7Gvg5bEbYXO_OMYQJp4txXKSloAlmr9iwOSo6qfGeUp2w6PiVAy1bd8F-03PysD3HFGZHdl2kRahtWmCDQiYGpOC9l93O_rs7eUNawaTtrMnqbCNw",
//        "Cache-Control": "no-cache",
//        "Connection": "keep-alive",
//        "Cookie": "vertx-web.session=752e6debe651cf776f9ecfe39afbc671",
//        "Host": "localhost:8888",
//        "Portalgateway": "1978875445",
//        "Pragma": "no-cache",
//        "Sec-Ch-Ua": "\"Chromium\";v=\"88\", \"Google Chrome\";v=\"88\", \";Not A Brand\";v=\"99\"",
//        "Sec-Ch-Ua-Mobile": "?0",
//        "Sec-Fetch-Dest": "document",
//        "Sec-Fetch-Mode": "navigate",
//        "Sec-Fetch-Site": "none",
//        "Sec-Fetch-User": "?1",
//        "Transfer-Encoding": "chunked",
//        "Upgrade-Insecure-Requests": "1",
//        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36",
//        "X-Forwarded-Host": "localhost:8000"
//        }
//        }
//</pre>