package com.example.interfaces.rest;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                title = "BraSeller Billing Service API",
                version = "1.0.0",
                description = "API de planos, trial, assinaturas e eventos de cobranca do BraSeller.",
                contact = @Contact(name = "BraSeller Platform"),
                license = @License(name = "Proprietary")
        ),
        tags = {
                @Tag(name = "Billing", description = "Planos, trials, assinaturas e webhooks de cobranca.")
        }
)
@SecuritySchemes({
        @SecurityScheme(
                securitySchemeName = "bearerAuth",
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT",
                description = "JWT emitido pelo auth-service."
        ),
        @SecurityScheme(
                securitySchemeName = "billingWebhookToken",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                apiKeyName = "X-Billing-Webhook-Token",
                description = "Token usado para webhooks do provedor de cobranca."
        )
})
final class OpenApiDefinition {
    private OpenApiDefinition() {
    }
}
