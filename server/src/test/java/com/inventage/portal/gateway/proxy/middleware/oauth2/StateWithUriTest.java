package com.inventage.portal.gateway.proxy.middleware.oauth2;

import com.inventage.portal.gateway.proxy.middleware.oauth2.relyingParty.StateWithUri;
import io.vertx.core.http.HttpMethod;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StateWithUriTest {

    @Test
    public void fromTwoValues() {
        // given
        final String state = "AbCd12";
        final String uri = "/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1";
        // when
        final StateWithUri stateWithUri = new StateWithUri(state, uri);
        // then
        Assertions.assertEquals(state, stateWithUri.state());
        Assertions.assertEquals(uri, stateWithUri.uri().orElse(null));
        Assertions.assertEquals(
            "QWJDZDEyOi9zZWdtZW50L3N1YnNlZ21lbnQvc3Vic3Vic2VnbWVudD9wYXJhbTE9dmFsdWUxJnBhcmFtMj12YWx1ZTIjZnJhZ21lbnQx",
            stateWithUri.toStateParameter());
    }

    @Test
    public void fromThreeValues() {
        // given
        final String state = "AbCd12";
        final String uri = "/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1";
        // when
        final StateWithUri stateWithUri = new StateWithUri(state, uri, HttpMethod.POST);
        // then
        Assertions.assertEquals(state, stateWithUri.state());
        Assertions.assertEquals(uri, stateWithUri.uri().orElse(null));
        Assertions.assertEquals(
            "QWJDZDEyOlBPU1RATDNObFoyMWxiblF2YzNWaWMyVm5iV1Z1ZEM5emRXSnpkV0p6WldkdFpXNTBQM0JoY21GdE1UMTJZV3gxWlRFbWNHRnlZVzB5UFhaaGJIVmxNaU5tY21GbmJXVnVkREU9",
            stateWithUri.toStateParameter());
    }

    @Test
    public void fromValuesWithoutUri() {
        // given
        final String state = "AbCd12";
        final String uri = null;
        // when
        final StateWithUri stateWithUri = new StateWithUri(state, uri);
        // then
        Assertions.assertEquals(state, stateWithUri.state());
        Assertions.assertNull(stateWithUri.uri().orElse(null));
        Assertions.assertEquals("QWJDZDEy", stateWithUri.toStateParameter());
    }

    @Test
    public void fromValuesNull() {
        // given
        // when
        try {
            new StateWithUri(null, null);
            Assertions.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // then
            Assertions.assertEquals("Null is not a valid state value!", e.getMessage());
        }
    }

    @Test
    public void fromValuesNull3() {
        // given
        // when
        try {
            new StateWithUri(null, null, null);
            Assertions.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // then
            Assertions.assertEquals("Null is not a valid state value!", e.getMessage());
        }
    }

    @Test
    public void fromEncoded() {
        // given
        final String stateParameterBase64Encoded = "QWJDZDEyOlBPU1RATDNObFoyMWxiblF2YzNWaWMyVm5iV1Z1ZEM5emRXSnpkV0p6WldkdFpXNTBQM0JoY21GdE1UMTJZV3gxWlRFbWNHRnlZVzB5UFhaaGJIVmxNaU5tY21GbmJXVnVkREU9"; // state=AbCd12; uri=/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1
        // when
        final StateWithUri stateWithUri = new StateWithUri(stateParameterBase64Encoded);
        // then
        Assertions.assertEquals("AbCd12", stateWithUri.state());
        Assertions.assertEquals("/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1",
            stateWithUri.uri().orElse(null));
        Assertions.assertEquals(stateParameterBase64Encoded, stateWithUri.toStateParameter());
    }

    @Test
    public void fromEncodedWithoutMethod() {
        // given
        final String stateParameterBase64Encoded = "QWJDZDEyOi9zZWdtZW50L3N1YnNlZ21lbnQvc3Vic3Vic2VnbWVudD9wYXJhbTE9dmFsdWUxJnBhcmFtMj12YWx1ZTIjZnJhZ21lbnQx"; // state=AbCd12; uri=/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1
        // when
        final StateWithUri stateWithUri = new StateWithUri(stateParameterBase64Encoded);
        // then
        Assertions.assertEquals("AbCd12", stateWithUri.state());
        Assertions.assertEquals("/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1",
            stateWithUri.uri().orElse(null));
        Assertions.assertEquals(stateParameterBase64Encoded, stateWithUri.toStateParameter());
    }

    @Test
    public void fromEncodedWithoutUri() {
        // given
        final String stateParameterBase64Encoded = "QWJDZDEy"; // state=AbCd12
        // when
        final StateWithUri stateWithUri = new StateWithUri(stateParameterBase64Encoded);
        // then
        Assertions.assertEquals("AbCd12", stateWithUri.state());
        Assertions.assertNull(stateWithUri.uri().orElse(null));
        Assertions.assertEquals(stateParameterBase64Encoded, stateWithUri.toStateParameter());
    }

    @Test
    public void fromEncodedWithEmptyUri() {
        // given
        final String stateParameterBase64Encoded = "QWJDZDEyOg=="; // state=AbCd12
        // when
        final StateWithUri stateWithUri = new StateWithUri(stateParameterBase64Encoded);
        // then
        Assertions.assertEquals("AbCd12", stateWithUri.state());
        Assertions.assertEquals("", stateWithUri.uri().get());
        Assertions.assertEquals(stateParameterBase64Encoded, stateWithUri.toStateParameter());
    }

    @Test
    public void fromInvalidEncodedAll() {
        // given
        final String stateParameter = "a-not-encoded-string"; // state=AbCd12; uri=/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1
        // when
        final StateWithUri stateWithUri = new StateWithUri(stateParameter);
        // then
        Assertions.assertEquals("a-not-encoded-string", stateWithUri.state());
        Assertions.assertNull(stateWithUri.uri().orElse(null));
        Assertions.assertEquals(stateParameter, stateWithUri.toStateParameter());

    }

    @Test
    public void fromInvalidEncodedURI() {
        // given
        final String stateParameter = "QWJDZDEyOlBPU1RAYS1ub3QtZW5jb2RlZC1zdHJpbmc="; // AbCd12:POST@a-not-encoded-string
        // when
        final StateWithUri stateWithUri = new StateWithUri(stateParameter);
        // then
        Assertions.assertEquals("AbCd12", stateWithUri.state());
        Assertions.assertNull(stateWithUri.uri().orElse(null));
        Assertions.assertEquals("QWJDZDEy", stateWithUri.toStateParameter());
    }

    @Test
    public void fromEncodedNull() {
        // given
        // when
        try {
            new StateWithUri(null);
            Assertions.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // then
            Assertions.assertEquals("Null is not a valid state parameter value!", e.getMessage());
        }
    }

    @Test
    public void learningURI() throws URISyntaxException {
        // given
        String aURI = "/";
        // when
        URI uri = new URI(aURI);
        // then
        Assertions.assertNotNull(uri);
        Assertions.assertEquals(aURI, uri.getPath());

        // given
        aURI = "";
        // when
        uri = new URI(aURI);
        // then
        Assertions.assertNotNull(uri);
        Assertions.assertEquals(aURI, uri.getPath());

        // given
        aURI = "";
        uri = new URI(aURI);
        // when
        URI relativeURI = new URI(null, null, uri.getPath(), uri.getQuery(), uri.getFragment());
        // then
        Assertions.assertNotNull(relativeURI);
        Assertions.assertEquals("", relativeURI.toString());

        // given
        aURI = "https://issue.inventage.com/secure/RapidBoard.jspa?projectKey=PORTAL&rapidView=105#fragment";
        uri = new URI(aURI);
        // when
        relativeURI = new URI(null, null, uri.getPath(), uri.getQuery(), uri.getFragment());
        // then
        Assertions.assertNotNull(relativeURI);
        Assertions.assertEquals("/secure/RapidBoard.jspa?projectKey=PORTAL&rapidView=105#fragment",
            relativeURI.toString());
    }

}
