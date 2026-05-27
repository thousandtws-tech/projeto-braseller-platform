package com.example.interfaces.rest;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

public class OpenApiSecurityFilter implements OASFilter {
    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null) {
            components = OASFactory.createComponents();
            openAPI.setComponents(components);
        }
        components.addSecurityScheme("bearerAuth", OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT emitido pelo auth-service."));
    }
}
