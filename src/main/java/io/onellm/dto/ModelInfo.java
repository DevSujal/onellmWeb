package io.onellm.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing model information from an LLM provider.
 */
public class ModelInfo {
    
    private String id;
    private String name;
    private String provider;
    private String description;
    
    @SerializedName("free")
    private boolean free;
    
    @SerializedName("context_length")
    private Integer contextLength;
    
    private Long created;
    
    @SerializedName("owned_by")
    private String ownedBy;

    public ModelInfo() {}

    public ModelInfo(String id, String name, String provider, String description) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.description = description;
        this.free = false;
    }

    public ModelInfo(String id, String name, String provider, String description, boolean free) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.description = description;
        this.free = free;
    }

    // Builder pattern for convenience
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ModelInfo model = new ModelInfo();

        public Builder id(String id) {
            model.id = id;
            return this;
        }

        public Builder name(String name) {
            model.name = name;
            return this;
        }

        public Builder provider(String provider) {
            model.provider = provider;
            return this;
        }

        public Builder description(String description) {
            model.description = description;
            return this;
        }

        public Builder free(boolean free) {
            model.free = free;
            return this;
        }

        public Builder contextLength(Integer contextLength) {
            model.contextLength = contextLength;
            return this;
        }

        public Builder created(Long created) {
            model.created = created;
            return this;
        }

        public Builder ownedBy(String ownedBy) {
            model.ownedBy = ownedBy;
            return this;
        }

        public ModelInfo build() {
            return model;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public Integer getContextLength() {
        return contextLength;
    }

    public void setContextLength(Integer contextLength) {
        this.contextLength = contextLength;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getOwnedBy() {
        return ownedBy;
    }

    public void setOwnedBy(String ownedBy) {
        this.ownedBy = ownedBy;
    }
}
