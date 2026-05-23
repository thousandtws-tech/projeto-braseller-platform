package com.example.interfaces.rest;

import com.example.application.exception.GatewayException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GatewayExceptionMapper implements ExceptionMapper<GatewayException> {
    @Override
    public Response toResponse(GatewayException exception) {
        return Response.status(exception.status())
                .entity(new RestError(exception.getMessage()))
                .build();
    }
}
