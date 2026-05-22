package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "UserView", description = "Representacao publica de usuario e seus papeis no tenant.")
public record UserView(String id, String tenantId, String email, String fullName, String status, List<String> roles) {
}
