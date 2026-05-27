package com.example.interfaces.rest;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

@OpenAPIDefinition(
        info = @Info(
                title = "BraSeller User Service API",
                version = "1.0.0",
                description = "API de tenants, usuarios, papeis e acesso de contador do BraSeller.",
                contact = @Contact(name = "BraSeller Platform"),
                license = @License(name = "Proprietary")
        ),
        tags = {
                @Tag(name = "Users", description = "Tenants, usuarios, papeis e acesso secundario do contador.")
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
                description = "Token interno usado em chamadas service-to-service."
        )
})
final class OpenApiDefinition {
    private OpenApiDefinition() {
    }
}
