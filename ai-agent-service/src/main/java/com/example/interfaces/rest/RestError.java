package com.example.interfaces.rest;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RestError", description = "Erro padrao retornado pela API.")
public record RestError(String message) {}
