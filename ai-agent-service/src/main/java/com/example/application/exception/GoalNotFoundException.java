package com.example.application.exception;

public class GoalNotFoundException extends RuntimeException {
    public GoalNotFoundException(String goalId) {
        super("goal_not_found: " + goalId);
    }
}
