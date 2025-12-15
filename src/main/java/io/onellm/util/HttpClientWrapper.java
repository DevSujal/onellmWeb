package io.onellm.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.onellm.exception.LLMException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * HTTP client wrapper for making API calls to LLM providers.
 * Features connection pooling, timeouts, and retry logic.
 */
public class HttpClientWrapper implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientWrapper.class);
    private static final Gson gson = new GsonBuilder().create();
    
    private final CloseableHttpClient httpClient;
    private final int maxRetries;
    private final long retryDelayMs;
    
    public HttpClientWrapper() {
        // Increased timeouts for streaming to slower HF Spaces
        this(60000, 300000, 3, 1000);
    }
    
    public HttpClientWrapper(int connectTimeoutMs, int readTimeoutMs, int maxRetries, long retryDelayMs) {
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(20);
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(connectTimeoutMs, TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(readTimeoutMs, TimeUnit.MILLISECONDS))
                .build();
        
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    /**
     * Makes a POST request and returns the JSON response.
     */
    public JsonObject post(String url, Map<String, String> headers, Object body) throws LLMException {
        String jsonBody = gson.toJson(body);
        return postWithRetry(url, headers, jsonBody, 0);
    }
    
    /**
     * Makes a POST request and returns the raw response string.
     */
    public String postRaw(String url, Map<String, String> headers, Object body) throws LLMException {
        String jsonBody = gson.toJson(body);
        return postRawWithRetry(url, headers, jsonBody, 0);
    }
    
    /**
     * Makes a streaming POST request using HttpURLConnection.
     * Using HttpURLConnection instead of Apache HttpClient to handle chunked encoding
     * termination issues with servers like Ollama that don't properly terminate chunked responses.
     */
    public void postStream(String url, Map<String, String> headers, Object body, 
                          Consumer<String> onLine, Consumer<Throwable> onError) throws LLMException {
        String jsonBody = gson.toJson(body);
        java.net.HttpURLConnection connection = null;
        
        try {
            java.net.URL urlObj = new java.net.URI(url).toURL();
            connection = (java.net.HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(60000);  // 60 seconds connect timeout
            connection.setReadTimeout(300000);    // 5 minutes read timeout for streaming
            
            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");
            headers.forEach(connection::setRequestProperty);
            
            // Write request body
            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            
            int statusCode = connection.getResponseCode();
            
            if (statusCode >= 400) {
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorBody = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorBody.append(line);
                    }
                    onError.accept(new LLMException("HTTP " + statusCode + ": " + errorBody));
                }
                return;
            }
            
            // Read streaming response
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    logger.debug("Stream line {}: [{}]", lineCount, line.length() > 200 ? line.substring(0, 200) + "..." : line);
                    if (!line.isEmpty()) {
                        onLine.accept(line);
                    }
                }
            }
            
        } catch (java.net.URISyntaxException e) {
            logger.error("Invalid URL: {} - Error: {}", url, e.getMessage(), e);
            onError.accept(new LLMException("Invalid URL: " + e.getMessage(), e));
        } catch (IOException e) {
            // Gracefully handle connection close - this is expected for streaming
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("Premature EOF") || 
                    errorMessage.contains("Connection reset") ||
                    errorMessage.contains("stream is closed"))) {
                logger.debug("Stream ended (expected): {}", errorMessage);
                // Don't report as error - stream just ended
            } else {
                logger.error("Streaming request failed to URL: {} - Error: {}", url, e.getMessage(), e);
                onError.accept(new LLMException("Streaming request failed: " + e.getMessage(), e));
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    private JsonObject postWithRetry(String url, Map<String, String> headers, String jsonBody, int attempt) 
            throws LLMException {
        String response = postRawWithRetry(url, headers, jsonBody, attempt);
        return JsonParser.parseString(response).getAsJsonObject();
    }
    
    private String postRawWithRetry(String url, Map<String, String> headers, String jsonBody, int attempt) 
            throws LLMException {
        HttpPost request = new HttpPost(url);
        headers.forEach(request::addHeader);
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        
        try {
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (statusCode >= 400) {
                    // Check if retryable
                    if (statusCode == 429 || statusCode >= 500) {
                        if (attempt < maxRetries) {
                            logger.warn("Request failed with status {}, retrying ({}/{})", 
                                        statusCode, attempt + 1, maxRetries);
                            try {
                                Thread.sleep(retryDelayMs * (attempt + 1));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return postRawWithRetry(url, headers, jsonBody, attempt + 1);
                        }
                    }
                    throw new LLMException("API", "HTTP " + statusCode + ": " + responseBody, statusCode);
                }
                
                return responseBody;
            });
        } catch (IOException e) {
            if (attempt < maxRetries) {
                logger.warn("Request failed with IO error, retrying ({}/{})", attempt + 1, maxRetries);
                try {
                    Thread.sleep(retryDelayMs * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return postRawWithRetry(url, headers, jsonBody, attempt + 1);
            }
            throw new LLMException("Request failed after " + maxRetries + " retries", e);
        }
    }
    
    public static Gson getGson() {
        return gson;
    }
    
    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.warn("Error closing HTTP client", e);
        }
    }
}
