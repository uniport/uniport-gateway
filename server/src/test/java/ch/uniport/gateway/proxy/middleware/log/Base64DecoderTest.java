package ch.uniport.gateway.proxy.middleware.log;

import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Base64DecoderTest {

    @Test
    public void decodeJWTAKKP977() {
        // given
        final String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJtb2NrLlRlc3RKd3RQcm92aWRlciIsImF1ZCI6Ik9yZ2FuaXNhdGlvbiIsInN1YiI6InVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJlODI2NjUxMy02NWZkLTQ4ZTAtYmU0OS1lZDUxMTVlYTUwYzQiLCJuYW1lIjoiVMOpc3Qgw5tzZXIiLCJnaXZlbl9uYW1lIjoiTm_DqSIsInRlbmFudCI6InBvcnRhbCIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJwb3J0YWx1c2VyIl19LCJvcmdhbmlzYXRpb24iOiJpbnZlbnRhZ2UuY29tIiwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6WyJVU0VSIl19fSwiaHR0cHM6Ly9oYXN1cmEuaW8vand0L2NsYWltcyI6eyJ4LWhhc3VyYS11c2VyLWlkIjoidXNlciIsIngtaGFzdXJhLW9yZ2FuaXNhdGlvbi1pZCI6ImludmVudGFnZS5jb20iLCJ4LWhhc3VyYS10ZW5hbnQtaWQiOiJwb3J0YWwiLCJ4LWhhc3VyYS1kZWZhdWx0LXJvbGUiOiJVU0VSIiwieC1oYXN1cmEtYWxsb3dlZC1yb2xlcyI6WyJVU0VSIl19LCJpYXQiOjE2OTg0MDQ1MDgsImV4cCI6MTcyOTk0MDUwOCwianRpIjoiZWJiNDM5ZGItOTNkMy00YjBhLWIyNmUtZjM2ZGJhNmVhMWE0In0.SGQXR5552uv34XIglvUosQE1AWu0Lt9ZODMbfW88k1BK-zCfo_V_iW74pORUYmrO-Mw7S83fqBEJ9gUZPHDT8aLMpEIwDamwYnxNB4EzvKKaHcDuBk5WALmrHULo6w6MQkbtplUpmI8zK9CY7yhF2YDJ4a0hRJtdnkd3AitB8hasVbOclY_PZb5DYvR_iukoLcltrkLTBjC0YNa8aKPRUHW9TLVaRydHzBTHPeyG2tbg7QFaSRA61byrIZoL0VJiYTJQnahzr-BZaOuH3YHdrG6msnifGmHCdrf8A3SdEEksLFbI3zqHVds-jNwCY8hONO41T0GzkkbhU4xbPIUkvA";
        final String[] chunks = jwt.split("\\.");
        final Base64.Decoder decoder = Base64.getDecoder();
        // when
        try {
            decoder.decode(chunks[1]);
            Assertions.fail("Expected: java.lang.IllegalArgumentException: Illegal base64 character 5f, because decoding is not done with base64url!");
        } catch (IllegalArgumentException e) {
            // then
            Assertions.assertEquals("Illegal base64 character 5f", e.getMessage());
        }
    }

    @Test
    public void decodeJWTAKKP977fixed() {
        // given
        final String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJtb2NrLlRlc3RKd3RQcm92aWRlciIsImF1ZCI6Ik9yZ2FuaXNhdGlvbiIsInN1YiI6InVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJlODI2NjUxMy02NWZkLTQ4ZTAtYmU0OS1lZDUxMTVlYTUwYzQiLCJuYW1lIjoiVMOpc3Qgw5tzZXIiLCJnaXZlbl9uYW1lIjoiTm_DqSIsInRlbmFudCI6InBvcnRhbCIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJwb3J0YWx1c2VyIl19LCJvcmdhbmlzYXRpb24iOiJpbnZlbnRhZ2UuY29tIiwicmVzb3VyY2VfYWNjZXNzIjp7Ik9yZ2FuaXNhdGlvbiI6eyJyb2xlcyI6WyJVU0VSIl19fSwiaHR0cHM6Ly9oYXN1cmEuaW8vand0L2NsYWltcyI6eyJ4LWhhc3VyYS11c2VyLWlkIjoidXNlciIsIngtaGFzdXJhLW9yZ2FuaXNhdGlvbi1pZCI6ImludmVudGFnZS5jb20iLCJ4LWhhc3VyYS10ZW5hbnQtaWQiOiJwb3J0YWwiLCJ4LWhhc3VyYS1kZWZhdWx0LXJvbGUiOiJVU0VSIiwieC1oYXN1cmEtYWxsb3dlZC1yb2xlcyI6WyJVU0VSIl19LCJpYXQiOjE2OTg0MDQ1MDgsImV4cCI6MTcyOTk0MDUwOCwianRpIjoiZWJiNDM5ZGItOTNkMy00YjBhLWIyNmUtZjM2ZGJhNmVhMWE0In0.SGQXR5552uv34XIglvUosQE1AWu0Lt9ZODMbfW88k1BK-zCfo_V_iW74pORUYmrO-Mw7S83fqBEJ9gUZPHDT8aLMpEIwDamwYnxNB4EzvKKaHcDuBk5WALmrHULo6w6MQkbtplUpmI8zK9CY7yhF2YDJ4a0hRJtdnkd3AitB8hasVbOclY_PZb5DYvR_iukoLcltrkLTBjC0YNa8aKPRUHW9TLVaRydHzBTHPeyG2tbg7QFaSRA61byrIZoL0VJiYTJQnahzr-BZaOuH3YHdrG6msnifGmHCdrf8A3SdEEksLFbI3zqHVds-jNwCY8hONO41T0GzkkbhU4xbPIUkvA";
        final String[] chunks = jwt.split("\\.");
        final Base64.Decoder decoder = Base64.getUrlDecoder();
        // when
        final String payload = new String(decoder.decode(chunks[1]));
        // then
        Assertions.assertNotNull(payload);
    }

}