package io.onellm.providers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GoogleProviderTest {

    @Test
    void buildsEndpointForBareModelId() {
        GoogleProvider provider = new GoogleProvider("KEY");
        String url = provider.getEndpointForModel("gemini-2.0-flash", false);
        assertTrue(url.contains("/models/gemini-2.0-flash:generateContent?key=KEY"), url);
    }

    @Test
    void buildsEndpointForGooglePrefixedModel() {
        GoogleProvider provider = new GoogleProvider("KEY");
        String url = provider.getEndpointForModel("google/gemini-2.0-flash", false);
        assertTrue(url.contains("/models/gemini-2.0-flash:generateContent?key=KEY"), url);
    }

    @Test
    void buildsEndpointForGeminiPrefixedModel() {
        GoogleProvider provider = new GoogleProvider("KEY");
        String url = provider.getEndpointForModel("gemini/gemini-2.0-flash", false);
        assertTrue(url.contains("/models/gemini-2.0-flash:generateContent?key=KEY"), url);
    }

    @Test
    void buildsEndpointForModelsPrefixedModel() {
        GoogleProvider provider = new GoogleProvider("KEY");
        String url = provider.getEndpointForModel("models/gemini-2.0-flash", false);
        assertTrue(url.contains("/models/gemini-2.0-flash:generateContent?key=KEY"), url);
        assertFalse(url.contains("/models/models/"), url);
    }

    @Test
    void buildsStreamEndpoint() {
        GoogleProvider provider = new GoogleProvider("KEY");
        String url = provider.getEndpointForModel("google/gemini-2.0-flash", true);
        assertTrue(url.contains("/models/gemini-2.0-flash:streamGenerateContent?key=KEY"), url);
    }
}
