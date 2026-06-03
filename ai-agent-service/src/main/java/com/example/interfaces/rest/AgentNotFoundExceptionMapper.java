package com.example.interfaces.rest;

import com.example.application.exception.AgentNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AgentNotFoundExceptionMapper implements ExceptionMapper<AgentNotFoundException> {
    @Override
    public Response toResponse(AgentNotFoundException ex) {
        return Response.status(Response.Status.NOT_FOUND).entity(new RestError(ex.getMessage())).build();
    }
}
