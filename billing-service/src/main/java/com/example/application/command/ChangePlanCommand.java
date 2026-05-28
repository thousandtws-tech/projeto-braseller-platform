package com.example.application.command;

import com.example.domain.model.BillingPlanCode;

public record ChangePlanCommand(String tenantId, BillingPlanCode planCode) {
}
