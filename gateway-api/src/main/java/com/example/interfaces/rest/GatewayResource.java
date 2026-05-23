package com.example.interfaces.rest;

import com.example.application.dto.GatewayRequest;
import com.example.application.dto.GatewayResponse;
import com.example.application.dto.GatewayRouteView;
import com.example.application.service.GatewayRoutingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/api")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Gateway", description = "Rotas publicas para os microservices.")
public class GatewayResource {
    private final GatewayRoutingService gatewayRoutingService;

    @Context
    UriInfo uriInfo;

    @Inject
    public GatewayResource(GatewayRoutingService gatewayRoutingService) {
        this.gatewayRoutingService = gatewayRoutingService;
    }

    @GET
    @Operation(summary = "Listar rotas do gateway", description = "Retorna as rotas publicas configuradas para os microservices.")
    @APIResponse(responseCode = "200", description = "Rotas configuradas.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GatewayOverview.class)))
    public GatewayOverview routes() {
        return new GatewayOverview("UP", gatewayRoutingService.routes());
    }

    @GET
    @Path("/{service}")
    public Response getServiceRoot(@PathParam("service") String service) {
        return forward("GET", service, "", null);
    }

    @GET
    @Path("/{service}/{path:.*}")
    public Response get(@PathParam("service") String service, @PathParam("path") String path) {
        return forward("GET", service, path, null);
    }

    @POST
    @Path("/{service}")
    public Response postServiceRoot(@PathParam("service") String service, String body) {
        return forward("POST", service, "", body);
    }

    @POST
    @Path("/{service}/{path:.*}")
    public Response post(@PathParam("service") String service, @PathParam("path") String path, String body) {
        return forward("POST", service, path, body);
    }

    @PUT
    @Path("/{service}")
    public Response putServiceRoot(@PathParam("service") String service, String body) {
        return forward("PUT", service, "", body);
    }

    @PUT
    @Path("/{service}/{path:.*}")
    public Response put(@PathParam("service") String service, @PathParam("path") String path, String body) {
        return forward("PUT", service, path, body);
    }

    @PATCH
    @Path("/{service}")
    public Response patchServiceRoot(@PathParam("service") String service, String body) {
        return forward("PATCH", service, "", body);
    }

    @PATCH
    @Path("/{service}/{path:.*}")
    public Response patch(@PathParam("service") String service, @PathParam("path") String path, String body) {
        return forward("PATCH", service, path, body);
    }

    @DELETE
    @Path("/{service}")
    public Response deleteServiceRoot(@PathParam("service") String service) {
        return forward("DELETE", service, "", null);
    }

    @DELETE
    @Path("/{service}/{path:.*}")
    public Response delete(@PathParam("service") String service, @PathParam("path") String path) {
        return forward("DELETE", service, path, null);
    }

    private Response forward(String method, String service, String path, String body) {
        GatewayResponse response = gatewayRoutingService.forward(new GatewayRequest(
                method,
                service,
                path,
                queryParameters(),
                body
        ));
        return toRestResponse(response);
    }

    private Response toRestResponse(GatewayResponse response) {
        Response.ResponseBuilder builder = Response.status(response.status());
        response.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        if (response.body().isBlank()) {
            return builder.build();
        }
        return builder.entity(response.body()).build();
    }

    private Map<String, List<String>> queryParameters() {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        uriInfo.getQueryParameters().forEach((name, values) -> parameters.put(name, List.copyOf(values)));
        return parameters;
    }

    @Schema(name = "GatewayOverview", description = "Status do gateway e rotas publicas disponiveis.")
    public record GatewayOverview(String status, List<GatewayRouteView> routes) {
    }
}
