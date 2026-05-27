package com.example.interfaces.rest;

import com.example.application.exception.ConnectorNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConnectorNotFoundExceptionMapper implements ExceptionMapper<ConnectorNotFoundException> {
    @Override
    public Response toResponse(ConnectorNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new RestError(exception.getMessage()))
                .build();
    }
}
