package com.urlshortener.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.model.User;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UrlServiceAdditionalTest {

  @Mock
  private ShortUrlRepository shortUrlRepo;

  @Mock
  private ClickEventRepository clickRepo;

  @Mock
  private UserRepository userRepo;

  @Captor
  private ArgumentCaptor<ClickEvent> clickEventCaptor;

  private UrlService urlService;

  @BeforeEach
  void setUp() {
    urlService = new UrlService(shortUrlRepo, clickRepo, userRepo);
    ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
  }

  // --- createShortUrl: additional blocked prefixes ---

  @Test
  void createShortUrl_blockedIPv6Loopback() {
    CreateUrlRequest request = new CreateUrlRequest("http://[::1]/admin", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_blocked169254() {
    CreateUrlRequest request = new CreateUrlRequest("http://169.254.1.1/config", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_blocked0dot() {
    CreateUrlRequest request = new CreateUrlRequest("http://0.0.0.0/test", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_anonymousUser() {
    when(shortUrlRepo.existsByShortCode(anyString())).thenReturn(false);
    when(shortUrlRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequest request = new CreateUrlRequest("https://example.com", null);
    UrlResponse response = urlService.createShortUrl(request, null);

    assertNotNull(response);
    assertEquals("https://example.com", response.originalUrl());
    verify(shortUrlRepo).save(any());
  }

  @Test
  void createShortUrl_preservesHttpsWhenAlreadyPresent() {
    when(shortUrlRepo.existsByShortCode(anyString())).thenReturn(false);
    when(shortUrlRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequest request = new CreateUrlRequest("https://already-https.com/path?q=1", null);
    UrlResponse response = urlService.createShortUrl(request, 1L);

    assertEquals("https://already-https.com/path?q=1", response.originalUrl());
  }

  @Test
  void createShortUrl_withNegativeExpiry() {
    when(shortUrlRepo.existsByShortCode(anyString())).thenReturn(false);
    when(shortUrlRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequest request = new CreateUrlRequest("https://example.com", -1L);
    UrlResponse response = urlService.createShortUrl(request, 1L);

    assertNull(response.expiresAt());
  }

  // --- listAllUrls ---

  @Test
  void listAllUrls_returnsAllUrlsSorted() {
    ShortUrl url1 = new ShortUrl();
    url1.setId(1L);
    url1.setShortCode("abc111");
    url1.setOriginalUrl("https://one.com");
    url1.setUserId(1L);
    url1.setCreatedAt(LocalDateTime.now());

    ShortUrl url2 = new ShortUrl();
    url2.setId(2L);
    url2.setShortCode("abc222");
    url2.setOriginalUrl("https://two.com");
    url2.setUserId(2L);
    url2.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findAll(any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(url1, url2)));
    when(clickRepo.countByShortUrlId(1L)).thenReturn(3L);
    when(clickRepo.countByShortUrlId(2L)).thenReturn(7L);

    Page<UrlResponse> result = urlService.listAllUrls(0, 50);

    assertEquals(2, result.getTotalElements());
    assertEquals("abc111", result.getContent().get(0).shortCode());
    assertEquals("abc222", result.getContent().get(1).shortCode());
    assertEquals(3L, result.getContent().get(0).clickCount());
    assertEquals(7L, result.getContent().get(1).clickCount());
  }

  // --- listUsers ---

  @Test
  void listUsers_returnsUserWithStats() {
    User user = new User();
    user.setId(1L);
    user.setEmail("user@example.com");
    user.setName("Test User");
    user.setCreatedAt(LocalDateTime.now());
    user.setAdmin(false);

    ShortUrl url = new ShortUrl();
    url.setId(10L);
    url.setUserId(1L);

    when(userRepo.findAll()).thenReturn(List.of(user));
    when(shortUrlRepo.findByUserId(1L)).thenReturn(List.of(url));
    when(clickRepo.countByShortUrlId(10L)).thenReturn(42L);

    List<Map<String, Object>> users = urlService.listUsers();

    assertEquals(1, users.size());
    Map<String, Object> u = users.get(0);
    assertEquals(1L, u.get("id"));
    assertEquals("user@example.com", u.get("email"));
    assertEquals("Test User", u.get("name"));
    assertEquals(1L, u.get("urlCount"));
    assertEquals(42L, u.get("totalClicks"));
    assertEquals(false, u.get("admin"));
  }

  @Test
  void listUsers_handlesUserWithNoUrls() {
    User user = new User();
    user.setId(2L);
    user.setEmail("empty@example.com");
    user.setName(null);
    user.setCreatedAt(LocalDateTime.now());
    user.setAdmin(false);

    when(userRepo.findAll()).thenReturn(List.of(user));
    when(shortUrlRepo.findByUserId(2L)).thenReturn(List.of());

    List<Map<String, Object>> users = urlService.listUsers();

    assertEquals(1, users.size());
    Map<String, Object> u = users.get(0);
    assertEquals(2L, u.get("id"));
    assertEquals("", u.get("name"));
    assertEquals(0L, u.get("urlCount"));
    assertEquals(0L, u.get("totalClicks"));
  }

  // --- getAnalytics: additional breakdowns ---

  @Test
  void getAnalytics_includesOsBreakdown() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setUserId(1L);
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));
    when(clickRepo.countByShortUrlId(1L)).thenReturn(5L);
    when(clickRepo.dailyClicks(eq(1L), any())).thenReturn(List.of());
    when(clickRepo.dailyDeviceBreakdown(eq(1L), any())).thenReturn(List.of());
    when(clickRepo.deviceBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.browserBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.refererBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.osBreakdown(1L)).thenReturn(List.<Object[]>of(
        new Object[]{"Windows", 3L},
        new Object[]{"macOS", 2L}
    ));

    var response = urlService.getAnalytics(1L, 1L, false);

    assertEquals(2, response.os().size());
    assertEquals("Windows", response.os().get(0).label());
    assertEquals(3L, response.os().get(0).count());
    assertEquals("macOS", response.os().get(1).label());
    assertEquals(2L, response.os().get(1).count());
  }

  @Test
  void getAnalytics_includesDailyDeviceBreakdown() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setUserId(1L);
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));
    when(clickRepo.countByShortUrlId(1L)).thenReturn(5L);
    when(clickRepo.dailyClicks(eq(1L), any())).thenReturn(List.of());
    when(clickRepo.deviceBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.browserBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.refererBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.osBreakdown(1L)).thenReturn(List.of());

    LocalDate today = LocalDate.now();
    when(clickRepo.dailyDeviceBreakdown(eq(1L), any())).thenReturn(List.<Object[]>of(
        new Object[]{today, "Desktop", 3L},
        new Object[]{today, "Mobile", 2L}
    ));

    var response = urlService.getAnalytics(1L, 1L, false);

    assertEquals(2, response.dailyDeviceBreakdown().size());
    assertEquals("Desktop", response.dailyDeviceBreakdown().get(0).deviceType());
    assertEquals("Mobile", response.dailyDeviceBreakdown().get(1).deviceType());
  }

  @Test
  void getAnalytics_handlesJavaSqlDate() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setUserId(1L);
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));
    when(clickRepo.countByShortUrlId(1L)).thenReturn(2L);
    when(clickRepo.deviceBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.browserBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.refererBreakdown(1L)).thenReturn(List.of());
    when(clickRepo.osBreakdown(1L)).thenReturn(List.of());

    // Simulate java.sql.Date return from database
    java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.now());
    when(clickRepo.dailyClicks(eq(1L), any())).thenReturn(List.<Object[]>of(
        new Object[]{sqlDate, 2L}
    ));
    when(clickRepo.dailyDeviceBreakdown(eq(1L), any())).thenReturn(List.<Object[]>of(
        new Object[]{sqlDate, "Desktop", 2L}
    ));

    var response = urlService.getAnalytics(1L, 1L, false);

    assertEquals(1, response.dailyClicks().size());
    assertEquals(LocalDate.now(), response.dailyClicks().get(0).date());
    assertEquals(1, response.dailyDeviceBreakdown().size());
  }

  // --- resolveAndTrack: user agent parsing ---

  @Test
  void resolveAndTrack_parsesMobileUserAgent() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("mob123");
    url.setOriginalUrl("https://example.com");

    when(shortUrlRepo.findByShortCode("mob123")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("5.6.7.8");
    when(request.getHeader("User-Agent"))
        .thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) Mobile Safari/605.1");
    when(request.getHeader("Referer")).thenReturn(null);

    urlService.resolveAndTrack("mob123", request);

    verify(clickRepo).save(clickEventCaptor.capture());
    ClickEvent e = clickEventCaptor.getValue();
    assertEquals("Mobile", e.getDeviceType());
    assertEquals("Safari", e.getBrowser());
    assertEquals("iOS", e.getOs());
  }

  @Test
  void resolveAndTrack_parsesAndroidUserAgent() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("and123");
    url.setOriginalUrl("https://example.com");

    when(shortUrlRepo.findByShortCode("and123")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("9.9.9.9");
    when(request.getHeader("User-Agent"))
        .thenReturn("Mozilla/5.0 (Linux; Android 14) Chrome/120.0 Mobile");
    when(request.getHeader("Referer")).thenReturn(null);

    urlService.resolveAndTrack("and123", request);

    verify(clickRepo).save(clickEventCaptor.capture());
    ClickEvent e = clickEventCaptor.getValue();
    assertEquals("Mobile", e.getDeviceType());
    assertEquals("Chrome", e.getBrowser());
    assertEquals("Android", e.getOs());
  }

  @Test
  void resolveAndTrack_parsesDesktopFirefoxOnWindows() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("ff123");
    url.setOriginalUrl("https://example.com");

    when(shortUrlRepo.findByShortCode("ff123")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    when(request.getHeader("User-Agent"))
        .thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Firefox/120.0");
    when(request.getHeader("Referer")).thenReturn(null);

    urlService.resolveAndTrack("ff123", request);

    verify(clickRepo).save(clickEventCaptor.capture());
    ClickEvent e = clickEventCaptor.getValue();
    assertEquals("Desktop", e.getDeviceType());
    assertEquals("Firefox", e.getBrowser());
    assertEquals("Windows", e.getOs());
  }

  @Test
  void resolveAndTrack_parsesEdgeOnMac() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("edg123");
    url.setOriginalUrl("https://example.com");

    when(shortUrlRepo.findByShortCode("edg123")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("2.3.4.5");
    when(request.getHeader("User-Agent"))
        .thenReturn("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Edge/120.0");
    when(request.getHeader("Referer")).thenReturn("https://twitter.com");

    urlService.resolveAndTrack("edg123", request);

    verify(clickRepo).save(clickEventCaptor.capture());
    ClickEvent e = clickEventCaptor.getValue();
    assertEquals("Desktop", e.getDeviceType());
    assertEquals("Edge", e.getBrowser());
    assertEquals("macOS", e.getOs());
    assertEquals("https://twitter.com", e.getReferer());
  }

  @Test
  void resolveAndTrack_parsesTablet() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("tab123");
    url.setOriginalUrl("https://example.com");

    when(shortUrlRepo.findByShortCode("tab123")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("3.4.5.6");
    when(request.getHeader("User-Agent"))
        .thenReturn("Mozilla/5.0 (iPad; CPU OS 17_0) Mobile Safari/605.1");
    when(request.getHeader("Referer")).thenReturn(null);

    urlService.resolveAndTrack("tab123", request);

    verify(clickRepo).save(clickEventCaptor.capture());
    ClickEvent e = clickEventCaptor.getValue();
    assertEquals("Tablet", e.getDeviceType());
    assertEquals("Safari", e.getBrowser());
    assertEquals("iOS", e.getOs());
  }

  @Test
  void resolveAndTrack_handlesNullUserAgent() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("nullua");
    url.setOriginalUrl("https://example.com");

    when(shortUrlRepo.findByShortCode("nullua")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("0.0.0.0");
    when(request.getHeader("User-Agent")).thenReturn(null);
    when(request.getHeader("Referer")).thenReturn(null);

    urlService.resolveAndTrack("nullua", request);

    verify(clickRepo).save(any());
  }

  @Test
  void resolveAndTrack_handlesUnknownUserAgent() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("unk123");
    url.setOriginalUrl("https://example.com");

    when(shortUrlRepo.findByShortCode("unk123")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("7.8.9.0");
    when(request.getHeader("User-Agent")).thenReturn("SomeRandomCrawler/1.0");
    when(request.getHeader("Referer")).thenReturn(null);

    urlService.resolveAndTrack("unk123", request);

    verify(clickRepo).save(clickEventCaptor.capture());
    ClickEvent e = clickEventCaptor.getValue();
    assertEquals("Desktop", e.getDeviceType());
    assertEquals("Other", e.getBrowser());
    assertEquals("Other", e.getOs());
  }

  // --- evictCache ---

  @Test
  void evictCache_doesNotThrow() {
    // This method just evicts from cache, should be a no-op without throwing
    urlService.evictCache("somecode");
  }

  // --- delete: null userId (anonymous URL) ---

  @Test
  void delete_anonymousUrl_adminCanDelete() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("anon99");
    url.setUserId(null);

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));

    urlService.delete(1L, 2L, true);

    verify(clickRepo).deleteByShortUrlId(1L);
    verify(shortUrlRepo).deleteById(1L);
  }

  @Test
  void delete_anonymousUrl_nonOwnerThrows() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("anon99");
    url.setUserId(null);

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));

    assertThrows(EntityNotFoundException.class, () -> urlService.delete(1L, 2L, false));
  }

  @Test
  void delete_anonymousUrl_ownerIsNullSoDifferentUserThrows() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("anon99");
    url.setUserId(null);

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));

    // userId is null, and passed userId is 2L (not null, not equal), and not admin
    assertThrows(EntityNotFoundException.class, () -> urlService.delete(1L, 2L, false));
  }

  // --- helper ---
  private ShortUrl createUrl(Long id) {
    ShortUrl url = new ShortUrl();
    url.setId(id);
    return url;
  }
}
