package com.example.interfaces.rest;

import com.example.application.exception.ConnectorValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConnectorValidationExceptionMapper implements ExceptionMapper<ConnectorValidationException> {
    @Override
    public Response toResponse(ConnectorValidationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new RestError(exception.getMessage()))
                .build();
    }
}
