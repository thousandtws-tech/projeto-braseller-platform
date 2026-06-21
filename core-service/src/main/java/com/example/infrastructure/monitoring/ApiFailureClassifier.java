package com.example.infrastructure.monitoring;

import com.example.application.exception.ConnectorRateLimitException;
import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class ApiFailureClassifier {
    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("api_error:\\s*(\\d{3})");

    public Classification classify(Throwable error, ApiFailureType failureTypeHint) {
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.ROOT);
        Integer httpStatus = extractHttpStatus(message);

        if (failureTypeHint == ApiFailureType.TOKEN_EXPIRED) {
            return new Classification(ApiFailureType.TOKEN_EXPIRED, ApiSeverity.CRITICAL, httpStatus,
                    "Integração ficará indisponível até reautorização do tenant.",
                    "reauthorization_required");
        }
        if (failureTypeHint == ApiFailureType.PAYLOAD_CHANGE) {
            return new Classification(ApiFailureType.PAYLOAD_CHANGE, ApiSeverity.CRITICAL, httpStatus,
                    "Resposta da API mudou de formato; sincronização pode estar incompleta.",
                    "manual_review_required");
        }
        if (error instanceof ConnectorRateLimitException || message.contains("rate_limit") || isStatus(httpStatus, 429)) {
            return new Classification(ApiFailureType.RATE_LIMIT, ApiSeverity.WARNING, httpStatus != null ? httpStatus : 429,
                    "Chamadas à API temporariamente bloqueadas por limite de requisições.",
                    "retry_scheduled_with_backoff");
        }
        if (isStatus(httpStatus, 401) || isStatus(httpStatus, 403)) {
            return new Classification(ApiFailureType.AUTH_FAILURE, ApiSeverity.CRITICAL, httpStatus,
                    "Credenciais ou token rejeitados pela integração.",
                    "token_refresh_attempted");
        }
        if (message.contains("api_unavailable") || message.contains("api_interrupted") || (httpStatus != null && httpStatus >= 500)) {
            return new Classification(ApiFailureType.UNAVAILABLE, ApiSeverity.CRITICAL, httpStatus,
                    "Serviço externo indisponível ou instável.",
                    "circuit_breaker_evaluated");
        }
        if (isTimeout(error, message)) {
            return new Classification(ApiFailureType.TIMEOUT, ApiSeverity.WARNING, httpStatus,
                    "Tempo de resposta da integração excedido.",
                    "retry_scheduled");
        }
        return new Classification(ApiFailureType.UNKNOWN, ApiSeverity.WARNING, httpStatus,
                "Falha não classificada na integração.",
                "logged_for_review");
    }

    private boolean isStatus(Integer httpStatus, int expected) {
        return httpStatus != null && httpStatus == expected;
    }

    private boolean isTimeout(Throwable error, String message) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException || cause instanceof ConnectException || cause instanceof IOException) {
                return true;
            }
            cause = cause.getCause();
        }
        return message.contains("timeout") || message.contains("timed out") || message.contains("interrupted");
    }

    private Integer extractHttpStatus(String message) {
        Matcher matcher = HTTP_STATUS_PATTERN.matcher(message);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

    public record Classification(
            ApiFailureType failureType,
            ApiSeverity severity,
            Integer httpStatus,
            String impact,
            String actionTaken) {
    }
}
