package com.urlshortener.dto;

import java.time.LocalDateTime;

public record UrlResponse(
    Long id,
    String originalUrl,
    String shortCode,
    String shortUrl,
    LocalDateTime createdAt,
    long clickCount,
    LocalDateTime expiresAt,
    boolean expired
) {}
