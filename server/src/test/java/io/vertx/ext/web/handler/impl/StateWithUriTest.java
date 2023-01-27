package io.vertx.ext.web.handler.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StateWithUriTest {

    @Test
    public void fromValues() {
        // given
        final String state = "AbCd12";
        final String uri = "/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1";
        // when
        StateWithUri stateWithUri = new StateWithUri(state, uri);
        // then
        Assertions.assertEquals(state, stateWithUri.state());
        Assertions.assertEquals(uri, stateWithUri.uri().orElse(null));
        Assertions.assertEquals("QWJDZDEyOi9zZWdtZW50L3N1YnNlZ21lbnQvc3Vic3Vic2VnbWVudD9wYXJhbTE9dmFsdWUxJnBhcmFtMj12YWx1ZTIjZnJhZ21lbnQx", stateWithUri.toStateParameter());
    }
    @Test
    public void fromValuesWithoutUri() {
        // given
        final String state = "AbCd12";
        final String uri = null;
        // when
        StateWithUri stateWithUri = new StateWithUri(state, uri);
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
            StateWithUri stateWithUri = new StateWithUri(null, null);
            Assertions.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException e) {
            // then
            Assertions.assertEquals("Null is not a valid state value!", e.getMessage());
        }
    }

    @Test
    public void fromEncoded() {
        // given
        final String stateParameterBase64Encoded = "QWJDZDEyOi9zZWdtZW50L3N1YnNlZ21lbnQvc3Vic3Vic2VnbWVudD9wYXJhbTE9dmFsdWUxJnBhcmFtMj12YWx1ZTIjZnJhZ21lbnQx"; // state=AbCd12; uri=/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1
        // when
        StateWithUri stateWithUri = new StateWithUri(stateParameterBase64Encoded);
        // then
        Assertions.assertEquals("AbCd12", stateWithUri.state());
        Assertions.assertEquals("/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1", stateWithUri.uri().orElse(null));
        Assertions.assertEquals(stateParameterBase64Encoded, stateWithUri.toStateParameter());
    }
    @Test
    public void fromEncodedWithoutUri() {
        // given
        final String stateParameterBase64Encoded = "QWJDZDEy"; // state=AbCd12; uri=/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1
        // when
        StateWithUri stateWithUri = new StateWithUri(stateParameterBase64Encoded);
        // then
        Assertions.assertEquals("AbCd12", stateWithUri.state());
        Assertions.assertNull(stateWithUri.uri().orElse(null));
        Assertions.assertEquals(stateParameterBase64Encoded, stateWithUri.toStateParameter());
    }
    @Test
    public void fromInvalidEncoded() {
        // given
        final String stateParameter = "a-not-encoded-string"; // state=AbCd12; uri=/segment/subsegment/subsubsegment?param1=value1&param2=value2#fragment1
        // when
        StateWithUri stateWithUri = new StateWithUri(stateParameter);
        // then
        Assertions.assertEquals("a-not-encoded-string", stateWithUri.state());
        Assertions.assertNull(stateWithUri.uri().orElse(null));
        Assertions.assertEquals(stateParameter, stateWithUri.toStateParameter());
    }
    @Test
    public void fromEncodedNull() {
        // given
        // when
        try {
            StateWithUri stateWithUri = new StateWithUri(null);
            Assertions.fail("IllegalArgumentException expected");
        }
        catch (IllegalArgumentException e) {
            // then
            Assertions.assertEquals("Null is not a valid state parameter value!", e.getMessage());
        }
    }

}
