package com.urlshortener.dto;

public record AuthResponse(String token, Long userId, String email) {}
