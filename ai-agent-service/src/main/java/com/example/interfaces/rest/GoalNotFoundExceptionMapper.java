package com.example.interfaces.rest;

import com.example.application.exception.GoalNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GoalNotFoundExceptionMapper implements ExceptionMapper<GoalNotFoundException> {
    @Override
    public Response toResponse(GoalNotFoundException ex) {
        return Response.status(Response.Status.NOT_FOUND).entity(new RestError(ex.getMessage())).build();
    }
}
