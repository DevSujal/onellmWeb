package io.onellm.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for performing web searches to provide real-time data to LLMs.
 * Uses the free-search API hosted at https://free-search.onrender.com
 */
@Service
public class WebSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSearchService.class);
    private static final String SEARCH_API_BASE_URL = "https://free-search.onrender.com/search";
    private static final int DEFAULT_RESULT_COUNT = 3;
    private static final int TIMEOUT_SECONDS = 30;
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    public WebSearchService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.gson = new Gson();
    }
    
    /**
     * Perform a web search and return results.
     * 
     * @param query The search query
     * @param resultCount Number of results to return (default: 3)
     * @param language Language code (optional, e.g., "en")
     * @param country Country code (optional, e.g., "US")
     * @return List of search results
     */
    public List<SearchResult> search(String query, Integer resultCount, String language, String country) {
        if (query == null || query.trim().isEmpty()) {
            logger.warn("Empty search query provided");
            return new ArrayList<>();
        }
        
        int k = resultCount != null && resultCount > 0 ? resultCount : DEFAULT_RESULT_COUNT;
        
        try {
            String url = buildSearchUrl(query, k, language, country);
            logger.info("Performing web search: {}", url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("Search API returned status code: {}", response.statusCode());
                return new ArrayList<>();
            }
            
            return parseSearchResponse(response.body());
            
        } catch (Exception e) {
            logger.error("Error performing web search: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Build the search API URL with query parameters.
     */
    private String buildSearchUrl(String query, int k, String language, String country) {
        StringBuilder url = new StringBuilder(SEARCH_API_BASE_URL);
        url.append("?q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        url.append("&k=").append(k);
        
        if (language != null && !language.isEmpty()) {
            url.append("&language=").append(URLEncoder.encode(language, StandardCharsets.UTF_8));
        }
        if (country != null && !country.isEmpty()) {
            url.append("&country=").append(URLEncoder.encode(country, StandardCharsets.UTF_8));
        }
        
        return url.toString();
    }
    
    /**
     * Parse the JSON response from the search API.
     */
    private List<SearchResult> parseSearchResponse(String responseBody) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            JsonArray resultsArray = json.getAsJsonArray("results");
            
            if (resultsArray != null) {
                for (int i = 0; i < resultsArray.size(); i++) {
                    JsonObject resultObj = resultsArray.get(i).getAsJsonObject();
                    SearchResult result = new SearchResult();
                    
                    if (resultObj.has("title") && !resultObj.get("title").isJsonNull()) {
                        result.setTitle(resultObj.get("title").getAsString());
                    }
                    if (resultObj.has("url") && !resultObj.get("url").isJsonNull()) {
                        result.setUrl(resultObj.get("url").getAsString());
                    }
                    if (resultObj.has("snippet") && !resultObj.get("snippet").isJsonNull()) {
                        result.setSnippet(resultObj.get("snippet").getAsString());
                    }
                    if (resultObj.has("date") && !resultObj.get("date").isJsonNull()) {
                        result.setDate(resultObj.get("date").getAsString());
                    }
                    if (resultObj.has("last_updated") && !resultObj.get("last_updated").isJsonNull()) {
                        result.setLastUpdated(resultObj.get("last_updated").getAsString());
                    }
                    
                    results.add(result);
                }
            }
            
            logger.info("Parsed {} search results", results.size());
            
        } catch (Exception e) {
            logger.error("Error parsing search response: {}", e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * Format search results as context for LLM injection.
     * 
     * @param results List of search results
     * @return Formatted string to inject as system context
     */
    public String formatResultsAsContext(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Here is current real-time information from the web to help answer the user's question:\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append("[").append(i + 1).append("] ");
            
            if (result.getTitle() != null) {
                context.append(result.getTitle()).append("\n");
            }
            if (result.getUrl() != null) {
                context.append("URL: ").append(result.getUrl()).append("\n");
            }
            if (result.getSnippet() != null) {
                context.append(result.getSnippet()).append("\n");
            }
            context.append("\n");
        }
        
        context.append("Use the above information to provide an accurate and up-to-date response. ");
        context.append("Cite sources when appropriate.");
        
        return context.toString();
    }
}
