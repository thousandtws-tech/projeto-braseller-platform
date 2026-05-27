package com.example.application.command;

public record SyncExternalProfileCommand(String email, String provider, String providerSubject, String fullName,
                                         String preferredUsername, String firstName, String lastName, String pictureUrl,
                                         boolean emailVerified) {
}
