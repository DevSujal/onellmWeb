package io.onellm.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for individual search result from the web search API.
 */
public class SearchResult {
    
    private String title;
    private String url;
    private String snippet;
    private String date;
    
    @SerializedName("last_updated")
    private String lastUpdated;
    
    public SearchResult() {}
    
    public SearchResult(String title, String url, String snippet) {
        this.title = title;
        this.url = url;
        this.snippet = snippet;
    }
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    
    @Override
    public String toString() {
        return "SearchResult{title='" + title + "', url='" + url + "'}";
    }
}
