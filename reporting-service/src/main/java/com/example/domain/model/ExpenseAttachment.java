package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExpenseAttachment(
        @JsonProperty("public_id") String publicId,
        @JsonProperty("secure_url") String secureUrl,
        @JsonProperty("resource_type") String resourceType,
        @JsonProperty("original_filename") String originalFilename,
        @JsonProperty("content_type") String contentType,
        @JsonProperty("size_bytes") Long sizeBytes) {
}
