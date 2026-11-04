package com.example.santaceciliatesis.data.remote;
import org.junit.Test;
import static org.junit.Assert.*;
public class ApiConfigTest {
    @Test public void emptyConfigDoesNotInventBackend() { assertFalse(new ApiConfig("").isConfigured()); }
    @Test public void normalizesTrailingSlash() {
        assertEquals("https://example.org/api/v1/servicios", new ApiConfig("https://example.org/api/v1").endpoint(ApiEndpoints.SERVICIOS));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsCleartext() { new ApiConfig("http://example.org/"); }
    @Test(expected=IllegalArgumentException.class) public void rejectsCredentials() { new ApiConfig("https://user:secret@example.org/"); }
    @Test(expected=IllegalArgumentException.class) public void rejectsQuery() { new ApiConfig("https://example.org/?key=secret"); }
    @Test(expected=IllegalArgumentException.class) public void rejectsFragment() { new ApiConfig("https://example.org/#section"); }
    @Test(expected=IllegalArgumentException.class) public void rejectsForeignEndpoint() { new ApiConfig("https://example.org/").endpoint("https://other.org"); }
    @Test(expected=IllegalStateException.class) public void noRequestWithoutBackend() { new ApiConfig("").endpoint(ApiEndpoints.SERVICIOS); }
}
