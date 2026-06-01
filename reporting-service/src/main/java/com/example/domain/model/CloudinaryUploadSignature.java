package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CloudinaryUploadSignature(
        @JsonProperty("cloud_name") String cloudName,
        @JsonProperty("api_key") String apiKey,
        @JsonProperty("upload_url") String uploadUrl,
        @JsonProperty("resource_type") String resourceType,
        @JsonProperty("folder") String folder,
        @JsonProperty("timestamp") long timestamp,
        @JsonProperty("use_filename") boolean useFilename,
        @JsonProperty("unique_filename") boolean uniqueFilename,
        @JsonProperty("signature") String signature) {
}
