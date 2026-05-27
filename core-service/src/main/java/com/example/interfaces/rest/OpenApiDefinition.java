package com.example.interfaces.rest;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                title = "BraSeller Core Service API",
                version = "1.0.0",
                description = "API de contexto tenant-aware e contratos compartilhados do BraSeller.",
                contact = @Contact(name = "BraSeller Platform"),
                license = @License(name = "Proprietary")
        ),
        tags = {
                @Tag(name = "Core", description = "Contexto tenant-aware compartilhado entre modulos."),
                @Tag(name = "Connectors", description = "Contrato padronizado entre Core e conectores de marketplaces.")
        }
)
@SecurityScheme(
        securitySchemeName = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT emitido pelo auth-service."
)
final class OpenApiDefinition {
    private OpenApiDefinition() {
    }
}
