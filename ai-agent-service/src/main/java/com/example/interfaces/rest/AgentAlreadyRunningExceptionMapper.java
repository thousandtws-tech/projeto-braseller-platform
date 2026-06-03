package com.example.interfaces.rest;

import com.example.application.exception.AgentAlreadyRunningException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AgentAlreadyRunningExceptionMapper implements ExceptionMapper<AgentAlreadyRunningException> {
    @Override
    public Response toResponse(AgentAlreadyRunningException ex) {
        return Response.status(Response.Status.CONFLICT).entity(new RestError(ex.getMessage())).build();
    }
}
