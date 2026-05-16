package com.urlshortener.service;

import com.urlshortener.dto.*;
import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UrlService {

  private static final List<String> BLOCKED_PREFIXES = List.of(
      "localhost", "127.", "0.0.0.0", "10.", "172.16.", "192.168.",
      "[::1]", "169.254.", "0.");

  private static final String CODE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int CODE_LENGTH = 6;
  private static final int MAX_COLLISION_RETRIES = 5;

  private final SecureRandom random = new SecureRandom();

  private final ShortUrlRepository shortUrlRepo;
  private final ClickEventRepository clickRepo;
  private final UserRepository userRepo;

  @Value("${app.base-url}")
  private String baseUrl;

  public UrlService(ShortUrlRepository shortUrlRepo, ClickEventRepository clickRepo, UserRepository userRepo) {
    this.shortUrlRepo = shortUrlRepo;
    this.clickRepo = clickRepo;
    this.userRepo = userRepo;
  }

  public UrlResponse createShortUrl(CreateUrlRequest request, Long userId) {
    String original = request.originalUrl().strip();
    if (original.isBlank()) {
      throw new IllegalArgumentException("URL must not be empty");
    }
    if (!original.startsWith("http://") && !original.startsWith("https://")) {
      original = "https://" + original;
    }

    URL parsed;
    try {
      parsed = new URL(original);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid URL format");
    }

    String host = parsed.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("URL must have a valid host");
    }
    if (!host.contains(".")) {
      throw new IllegalArgumentException("URL must be a fully qualified domain name");
    }

    String hostLower = host.toLowerCase();
    for (String blocked : BLOCKED_PREFIXES) {
      if (hostLower.startsWith(blocked)) {
        throw new IllegalArgumentException("URL points to a private or blocked domain");
      }
    }

    String code = generateUniqueCode();
    ShortUrl url = new ShortUrl();
    url.setOriginalUrl(original);
    url.setShortCode(code);
    url.setUserId(userId);
    shortUrlRepo.save(url);

    return toResponse(url);
  }

  @Transactional
  public String resolveAndTrack(String shortCode, HttpServletRequest request) {
    ShortUrl url = shortUrlRepo.findByShortCode(shortCode)
        .orElseThrow(() -> new EntityNotFoundException("Short URL not found: " + shortCode));

    ClickEvent event = new ClickEvent();
    event.setShortUrl(url);
    event.setIpAddress(request.getRemoteAddr());
    event.setUserAgent(request.getHeader("User-Agent"));
    event.setReferer(request.getHeader("Referer"));

    String ua = request.getHeader("User-Agent");
    if (ua != null) {
      parseUserAgent(ua, event);
    }

    clickRepo.save(event);
    return url.getOriginalUrl();
  }

  @Cacheable(value = "shortUrl", key = "#shortCode")
  public String resolveCached(String shortCode) {
    ShortUrl url = shortUrlRepo.findByShortCode(shortCode)
        .orElseThrow(() -> new EntityNotFoundException("Short URL not found: " + shortCode));
    return url.getOriginalUrl();
  }

  public Page<UrlResponse> listAll(Long userId, int page, int size) {
    return shortUrlRepo.findByUserId(userId, PageRequest.of(page, size))
        .map(this::toResponse);
  }

  public Page<UrlResponse> listAllUrls(int page, int size) {
    return shortUrlRepo.findAll(PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
        .map(this::toResponse);
  }

  public Map<String, Object> getTotalStats() {
    long totalUrls = shortUrlRepo.count();
    long totalClicks = 0;
    for (var url : shortUrlRepo.findAll()) {
      totalClicks += clickRepo.countByShortUrlId(url.getId());
    }
    return Map.of(
        "totalUsers", userRepo.count(),
        "totalUrls", totalUrls,
        "totalClicks", totalClicks
    );
  }

  public AnalyticsResponse getAnalytics(Long id, Long userId, boolean admin) {
    ShortUrl url = shortUrlRepo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("URL not found: " + id));
    if (!url.getUserId().equals(userId) && !admin) {
      throw new EntityNotFoundException("URL not found: " + id);
    }

    long totalClicks = clickRepo.countByShortUrlId(id);
    LocalDateTime since = LocalDateTime.now().minusDays(30);

    List<Object[]> dailyRows = clickRepo.dailyClicks(id, since);
    List<DailyCount> daily = dailyRows.stream().map(r -> {
      LocalDate day;
      if (r[0] instanceof java.sql.Date d) {
        day = d.toLocalDate();
      } else {
        day = (LocalDate) r[0];
      }
      return new DailyCount(day, (Long) r[1]);
    }).collect(Collectors.toList());

    List<CountItem> devices = toCountItems(clickRepo.deviceBreakdown(id));
    List<CountItem> browsers = toCountItems(clickRepo.browserBreakdown(id));
    List<CountItem> referrers = toCountItems(clickRepo.refererBreakdown(id));

    return new AnalyticsResponse(url.getShortCode(), url.getOriginalUrl(), totalClicks, daily, devices, browsers, referrers);
  }

  @CacheEvict(value = "shortUrl", key = "#shortCode")
  public void evictCache(String shortCode) {
  }

  public void delete(Long id, Long userId, boolean admin) {
    ShortUrl url = shortUrlRepo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("URL not found: " + id));
    if (!url.getUserId().equals(userId) && !admin) {
      throw new EntityNotFoundException("URL not found: " + id);
    }
    shortUrlRepo.deleteById(id);
  }

  private String generateUniqueCode() {
    for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
      String code = randomCode();
      if (!shortUrlRepo.existsByShortCode(code)) {
        return code;
      }
    }
    throw new RuntimeException("Unable to generate unique short code after " + MAX_COLLISION_RETRIES + " attempts");
  }

  private String randomCode() {
    StringBuilder sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
    }
    return sb.toString();
  }

  private void parseUserAgent(String ua, ClickEvent event) {
    String lower = ua.toLowerCase();
    if (lower.contains("mobile") || lower.contains("iphone") || lower.contains("android")) {
      event.setDeviceType("Mobile");
    } else if (lower.contains("tablet") || lower.contains("ipad")) {
      event.setDeviceType("Tablet");
    } else {
      event.setDeviceType("Desktop");
    }
    if (lower.contains("chrome") && !lower.contains("edg")) {
      event.setBrowser("Chrome");
    } else if (lower.contains("safari") && !lower.contains("chrome")) {
      event.setBrowser("Safari");
    } else if (lower.contains("firefox")) {
      event.setBrowser("Firefox");
    } else if (lower.contains("edg")) {
      event.setBrowser("Edge");
    } else {
      event.setBrowser("Other");
    }
    if (lower.contains("windows")) {
      event.setOs("Windows");
    } else if (lower.contains("mac")) {
      event.setOs("macOS");
    } else if (lower.contains("linux")) {
      event.setOs("Linux");
    } else if (lower.contains("android")) {
      event.setOs("Android");
    } else if (lower.contains("iphone") || lower.contains("ipad")) {
      event.setOs("iOS");
    } else {
      event.setOs("Other");
    }
  }

  private UrlResponse toResponse(ShortUrl url) {
    return new UrlResponse(
        url.getId(),
        url.getOriginalUrl(),
        url.getShortCode(),
        baseUrl + "/" + url.getShortCode(),
        url.getCreatedAt(),
        clickRepo.countByShortUrlId(url.getId())
    );
  }

  private List<CountItem> toCountItems(List<Object[]> rows) {
    return rows.stream()
        .map(r -> new CountItem((String) r[0], (Long) r[1]))
        .collect(Collectors.toList());
  }
}
