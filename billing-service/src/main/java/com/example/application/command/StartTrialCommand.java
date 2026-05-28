package com.example.application.command;

import com.example.domain.model.BillingPlanCode;

public record StartTrialCommand(String tenantId, BillingPlanCode planCode) {
}
