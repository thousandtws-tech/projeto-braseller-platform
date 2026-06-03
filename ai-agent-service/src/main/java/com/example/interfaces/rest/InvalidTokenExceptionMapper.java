package com.example.interfaces.rest;

import com.example.application.exception.InvalidTokenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidTokenExceptionMapper implements ExceptionMapper<InvalidTokenException> {
    @Override
    public Response toResponse(InvalidTokenException ex) {
        return Response.status(Response.Status.UNAUTHORIZED).entity(new RestError(ex.getMessage())).build();
    }
}
