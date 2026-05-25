package com.urlshortener.dto;

import java.time.LocalDate;

public record DailyDeviceCount(LocalDate date, String deviceType, long count) {}
