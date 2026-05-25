package com.urlshortener.dto;

import java.util.List;

public record AnalyticsResponse(
    String shortCode,
    String originalUrl,
    long totalClicks,
    List<DailyCount> dailyClicks,
    List<DailyDeviceCount> dailyDeviceBreakdown,
    List<CountItem> devices,
    List<CountItem> browsers,
    List<CountItem> os,
    List<CountItem> referrers
) {}
