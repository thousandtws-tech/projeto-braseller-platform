package com.example.interfaces.rest;

import com.example.application.exception.AccountingPeriodClosedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AccountingPeriodClosedExceptionMapper implements ExceptionMapper<AccountingPeriodClosedException> {
    @Override
    public Response toResponse(AccountingPeriodClosedException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new RestError(exception.getMessage()))
                .build();
    }
}
