package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(
    @NotBlank(message = "URL is required") String originalUrl
) {}
