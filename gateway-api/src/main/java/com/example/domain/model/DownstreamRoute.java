package com.example.domain.model;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public record DownstreamRoute(
        String publicSegment,
        String serviceName,
        URI baseUri,
        String downstreamPathPrefix,
        List<String> blockedPathPrefixes) {

    public DownstreamRoute(String publicSegment, String serviceName, URI baseUri, String downstreamPathPrefix) {
        this(publicSegment, serviceName, baseUri, downstreamPathPrefix, List.of());
    }

    public DownstreamRoute {
        publicSegment = normalizeSegment(publicSegment);
        serviceName = requireText(serviceName, "serviceName");
        baseUri = normalizeBaseUri(baseUri);
        downstreamPathPrefix = normalizePathPrefix(downstreamPathPrefix);
        blockedPathPrefixes = normalizeBlockedPrefixes(blockedPathPrefixes);
    }

    public String publicPath() {
        return "/api/" + publicSegment;
    }

    public String resolveDownstreamPath(String remainingPath) {
        String normalizedRemainingPath = trimSlashes(remainingPath);
        if (normalizedRemainingPath.isBlank()) {
            return downstreamPathPrefix;
        }
        if ("/".equals(downstreamPathPrefix)) {
            return "/" + normalizedRemainingPath;
        }
        return downstreamPathPrefix + "/" + normalizedRemainingPath;
    }

    public boolean blocks(String remainingPath) {
        String normalizedRemainingPath = trimSlashes(remainingPath).toLowerCase(Locale.ROOT);
        return blockedPathPrefixes.stream()
                .anyMatch(prefix -> normalizedRemainingPath.equals(prefix)
                        || normalizedRemainingPath.startsWith(prefix + "/"));
    }

    private static String normalizeSegment(String value) {
        String normalized = trimSlashes(value).toLowerCase(Locale.ROOT);
        return requireText(normalized, "publicSegment");
    }

    private static URI normalizeBaseUri(URI value) {
        if (value == null || !value.isAbsolute()) {
            throw new IllegalArgumentException("baseUri must be absolute");
        }
        String normalized = value.toString();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return URI.create(normalized);
    }

    private static String normalizePathPrefix(String value) {
        String normalized = trimSlashes(value);
        if (normalized.isBlank()) {
            return "/";
        }
        return "/" + normalized;
    }

    private static List<String> normalizeBlockedPrefixes(List<String> value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return value.stream()
                .map(DownstreamRoute::trimSlashes)
                .map(prefix -> prefix.toLowerCase(Locale.ROOT))
                .filter(prefix -> !prefix.isBlank())
                .distinct()
                .toList();
    }

    private static String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
