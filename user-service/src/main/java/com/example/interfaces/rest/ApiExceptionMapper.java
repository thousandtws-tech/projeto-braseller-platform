package com.example.interfaces.rest;

import com.example.application.exception.TenantMismatchException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<TenantMismatchException> {
    @Override
    public Response toResponse(TenantMismatchException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new RestError(exception.getMessage()))
                .build();
    }
}
