package com.example.application.service;

import com.example.application.exception.ValidationException;
import com.example.domain.model.CloudinaryUploadSignature;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class CloudinaryUploadSignatureService {
    @ConfigProperty(name = "reporting.cloudinary.cloud-name")
    String cloudName;

    @ConfigProperty(name = "reporting.cloudinary.api-key")
    String apiKey;

    @ConfigProperty(name = "reporting.cloudinary.api-secret")
    String apiSecret;

    @ConfigProperty(name = "reporting.cloudinary.expense-folder")
    String expenseFolder;

    @ConfigProperty(name = "reporting.cloudinary.resource-type")
    String resourceType;

    public CloudinaryUploadSignature expenseUploadSignature(String tenantId) {
        requireConfigured();
        String resolvedResourceType = defaultIfBlank(resourceType, "auto");
        String folder = folderForTenant(tenantId);
        long timestamp = Instant.now().getEpochSecond();

        Map<String, String> params = new TreeMap<>();
        params.put("folder", folder);
        params.put("timestamp", Long.toString(timestamp));
        params.put("unique_filename", "true");
        params.put("use_filename", "true");

        return new CloudinaryUploadSignature(
                cloudName.trim(),
                apiKey.trim(),
                "https://api.cloudinary.com/v1_1/" + cloudName.trim() + "/" + resolvedResourceType + "/upload",
                resolvedResourceType,
                folder,
                timestamp,
                true,
                true,
                sign(params)
        );
    }

    private void requireConfigured() {
        if (isNotConfigured(cloudName) || isNotConfigured(apiKey) || isNotConfigured(apiSecret)) {
            throw new ValidationException("cloudinary_not_configured");
        }
    }

    private String folderForTenant(String tenantId) {
        String safeTenantId = sanitizeTenantId(tenantId);
        String baseFolder = trimSlashes(defaultIfBlank(expenseFolder, "brasaller/despesas"));
        return baseFolder + "/" + safeTenantId;
    }

    private String sanitizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new ValidationException("tenant_id_required");
        }
        String sanitized = tenantId.trim().replaceAll("[^A-Za-z0-9_.-]", "-");
        if (sanitized.isBlank()) {
            throw new ValidationException("tenant_id_required");
        }
        return sanitized;
    }

    private String sign(Map<String, String> params) {
        String payload = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"))
                + apiSecret.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("cloudinary_signature_failed", exception);
        }
    }

    private String trimSlashes(String value) {
        String trimmed = value.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean isNotConfigured(String value) {
        return value == null || value.isBlank() || "not-configured".equalsIgnoreCase(value.trim());
    }
}
