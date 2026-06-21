package com.example.interfaces.rest;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

@Path("/integrations")
@Tag(name = "Marketplace callbacks", description = "Browser callbacks from marketplace OAuth providers.")
public class MarketplaceCallbackResource {
    private final URI mercadoLivreFrontendCallbackUrl;

    @Context
    UriInfo uriInfo;

    public MarketplaceCallbackResource(
            @ConfigProperty(name = "gateway.marketplace-callback.mercado-livre.frontend-url")
            URI mercadoLivreFrontendCallbackUrl) {
        this.mercadoLivreFrontendCallbackUrl = mercadoLivreFrontendCallbackUrl;
    }

    @GET
    @Path("/mercado-livre/callback")
    @Operation(summary = "Mercado Livre OAuth callback", description = "Redirects the browser back to the Brasaller app callback screen.")
    @APIResponse(responseCode = "303", description = "Browser redirected to the frontend callback page.")
    public Response mercadoLivreCallback() {
        URI target = frontendCallback("mercado-livre");
        return Response.seeOther(target)
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    private URI frontendCallback(String connectorName) {
        UriBuilder builder = UriBuilder.fromUri(mercadoLivreFrontendCallbackUrl)
                .queryParam("connector", connectorName);

        uriInfo.getQueryParameters().forEach((name, values) ->
                values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .forEach(value -> builder.queryParam(name, value)));

        return builder.build();
    }
}
