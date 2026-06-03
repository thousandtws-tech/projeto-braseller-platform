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
                title = "Brasaller AI Agent Service API",
                version = "1.0.0",
                description = "Servico de agentes autonomos de IA para a plataforma Brasaller.",
                contact = @Contact(name = "Clarituz - Brasaller Platform"),
                license = @License(name = "Proprietary")
        ),
        tags = {
                @Tag(name = "Agents", description = "Gerenciamento de agentes autonomos"),
                @Tag(name = "Goals", description = "Objetivos e metas dos agentes"),
                @Tag(name = "Executions", description = "Historico e status de execucoes"),
                @Tag(name = "Memory", description = "Memoria persistente dos agentes"),
                @Tag(name = "Decisions", description = "Decisoes tomadas pelos agentes"),
                @Tag(name = "Feedback", description = "Feedback e aprendizado")
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
                securitySchemeName = "internalToken",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                apiKeyName = "X-Internal-Token",
                description = "Token interno para chamadas service-to-service."
        )
})
final class OpenApiDefinition {
    private OpenApiDefinition() {}
}
