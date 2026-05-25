package com.urlshortener.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

  @Mock
  private ShortUrlRepository shortUrlRepo;

  @Mock
  private ClickEventRepository clickRepo;

  @Mock
  private UserRepository userRepo;

  private UrlService urlService;

  @BeforeEach
  void setUp() {
    urlService = new UrlService(shortUrlRepo, clickRepo, userRepo);
    ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
  }

  @Test
  void createShortUrl_success() {
    when(shortUrlRepo.existsByShortCode(anyString())).thenReturn(false);
    when(shortUrlRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequest request = new CreateUrlRequest("https://example.com", null);
    UrlResponse response = urlService.createShortUrl(request, 1L);

    assertNotNull(response);
    assertEquals("https://example.com", response.originalUrl());
    assertNotNull(response.shortCode());
    assertEquals(6, response.shortCode().length());
    assertTrue(response.shortUrl().startsWith("http://localhost:8080/"));
    verify(shortUrlRepo).save(any());
  }

  @Test
  void createShortUrl_autoHttps() {
    when(shortUrlRepo.existsByShortCode(anyString())).thenReturn(false);
    when(shortUrlRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequest request = new CreateUrlRequest("example.com", null);
    UrlResponse response = urlService.createShortUrl(request, 1L);

    assertEquals("https://example.com", response.originalUrl());
  }

  @Test
  void createShortUrl_preservesHttp() {
    when(shortUrlRepo.existsByShortCode(anyString())).thenReturn(false);
    when(shortUrlRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequest request = new CreateUrlRequest("http://example.com", null);
    UrlResponse response = urlService.createShortUrl(request, 1L);

    assertEquals("http://example.com", response.originalUrl());
  }

  @Test
  void createShortUrl_emptyUrl() {
    CreateUrlRequest request = new CreateUrlRequest("", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_blankUrl() {
    CreateUrlRequest request = new CreateUrlRequest("   ", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_invalidUrl() {
    CreateUrlRequest request = new CreateUrlRequest("not a valid url at all !!!", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_noHost() {
    CreateUrlRequest request = new CreateUrlRequest("https://", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_blockedLocalhost() {
    CreateUrlRequest request = new CreateUrlRequest("http://localhost:8080/secret", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_blockedPrivateIp() {
    CreateUrlRequest request = new CreateUrlRequest("http://192.168.1.1/admin", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_blockedLoopback() {
    CreateUrlRequest request = new CreateUrlRequest("http://127.0.0.1:3000", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_blocked10dot() {
    CreateUrlRequest request = new CreateUrlRequest("http://10.0.0.1/admin", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_blocked17216() {
    CreateUrlRequest request = new CreateUrlRequest("http://172.16.0.1/admin", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_noDomain() {
    CreateUrlRequest request = new CreateUrlRequest("https://localhost", null);
    assertThrows(IllegalArgumentException.class, () -> urlService.createShortUrl(request, 1L));
  }

  @Test
  void createShortUrl_withExpiry() {
    when(shortUrlRepo.existsByShortCode(anyString())).thenReturn(false);
    when(shortUrlRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequest request = new CreateUrlRequest("https://example.com", 24L);
    UrlResponse response = urlService.createShortUrl(request, 1L);

    assertNotNull(response);
    assertNotNull(response.expiresAt());
    assertFalse(response.expired());
  }

  @Test
  void createShortUrl_zeroExpiry() {
    when(shortUrlRepo.existsByShortCode(anyString())).thenReturn(false);
    when(shortUrlRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequest request = new CreateUrlRequest("https://example.com", 0L);
    UrlResponse response = urlService.createShortUrl(request, 1L);

    assertNull(response.expiresAt());
  }

  @Test
  void resolveAndTrack_success() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findByShortCode("abc123")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/120");
    when(request.getHeader("Referer")).thenReturn("https://google.com");

    String result = urlService.resolveAndTrack("abc123", request);

    assertEquals("https://example.com", result);
    verify(clickRepo).save(any());
  }

  @Test
  void resolveAndTrack_dedupWithin2Seconds() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findByShortCode("abc123")).thenReturn(Optional.of(url));
    when(clickRepo.save(any())).thenAnswer(i -> i.getArgument(0));

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("1.2.3.4");
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(request.getHeader("Referer")).thenReturn(null);

    // First call - should track
    urlService.resolveAndTrack("abc123", request);
    // Second call - should be deduped within 2 seconds
    String result = urlService.resolveAndTrack("abc123", request);

    assertEquals("https://example.com", result);
    verify(clickRepo, times(1)).save(any());
  }

  @Test
  void resolveAndTrack_notFound() {
    when(shortUrlRepo.findByShortCode("nonexist")).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class,
        () -> urlService.resolveAndTrack("nonexist", mock(HttpServletRequest.class)));
  }

  @Test
  void resolveCached_success() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");

    when(shortUrlRepo.findByShortCode("abc123")).thenReturn(Optional.of(url));

    ShortUrl result = urlService.resolveCached("abc123");
    assertEquals("abc123", result.getShortCode());
  }

  @Test
  void resolveCached_notFound() {
    when(shortUrlRepo.findByShortCode("nonexist")).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> urlService.resolveCached("nonexist"));
  }

  @Test
  void listAll_returnsUserUrls() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setUserId(1L);
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findByUserId(eq(1L), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(url)));
    when(clickRepo.countByShortUrlId(1L)).thenReturn(5L);

    Page<UrlResponse> result = urlService.listAll(1L, 0, 50);

    assertEquals(1, result.getTotalElements());
    assertEquals("abc123", result.getContent().get(0).shortCode());
    assertEquals(5L, result.getContent().get(0).clickCount());
  }

  @Test
  void getAnalytics_ownUrl() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setUserId(1L);
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));
    when(clickRepo.countByShortUrlId(1L)).thenReturn(10L);
    when(clickRepo.dailyClicks(eq(1L), any())).thenReturn(List.<Object[]>of(
        new Object[]{LocalDate.now(), 5L},
        new Object[]{LocalDate.now().minusDays(1), 5L}
    ));
    when(clickRepo.deviceBreakdown(1L)).thenReturn(List.<Object[]>of(
        new Object[]{"Desktop", 8L}, new Object[]{"Mobile", 2L}
    ));
    when(clickRepo.browserBreakdown(1L)).thenReturn(List.<Object[]>of(
        new Object[]{"Chrome", 6L}, new Object[]{"Firefox", 4L}
    ));
    when(clickRepo.refererBreakdown(1L)).thenReturn(List.<Object[]>of(
        new Object[]{"https://google.com", 10L}
    ));

    AnalyticsResponse response = urlService.getAnalytics(1L, 1L, false);

    assertEquals("abc123", response.shortCode());
    assertEquals(10L, response.totalClicks());
    assertEquals(2, response.dailyClicks().size());
    assertEquals(2, response.devices().size());
    assertEquals(2, response.browsers().size());
    assertEquals(1, response.referrers().size());
  }

  @Test
  void getAnalytics_notOwnUrl_throws() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setUserId(2L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));

    assertThrows(EntityNotFoundException.class,
        () -> urlService.getAnalytics(1L, 1L, false));
  }

  @Test
  void getAnalytics_adminCanViewAny() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setOriginalUrl("https://example.com");
    url.setUserId(2L);
    url.setCreatedAt(LocalDateTime.now());

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));
    when(clickRepo.countByShortUrlId(1L)).thenReturn(0L);
    when(clickRepo.dailyClicks(eq(1L), any())).thenReturn(List.<Object[]>of());
    when(clickRepo.deviceBreakdown(1L)).thenReturn(List.<Object[]>of());
    when(clickRepo.browserBreakdown(1L)).thenReturn(List.<Object[]>of());
    when(clickRepo.refererBreakdown(1L)).thenReturn(List.<Object[]>of());

    AnalyticsResponse response = urlService.getAnalytics(1L, 1L, true);
    assertNotNull(response);
  }

  @Test
  void getAnalytics_notFound() {
    when(shortUrlRepo.findById(999L)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class,
        () -> urlService.getAnalytics(999L, 1L, false));
  }

  @Test
  void delete_ownUrl() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setUserId(1L);

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));

    urlService.delete(1L, 1L, false);

    verify(clickRepo).deleteByShortUrlId(1L);
    verify(shortUrlRepo).deleteById(1L);
  }

  @Test
  void delete_notOwnUrl_throws() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setUserId(2L);

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));

    assertThrows(EntityNotFoundException.class, () -> urlService.delete(1L, 1L, false));
  }

  @Test
  void delete_adminCanDeleteAny() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setShortCode("abc123");
    url.setUserId(2L);

    when(shortUrlRepo.findById(1L)).thenReturn(Optional.of(url));

    urlService.delete(1L, 1L, true);

    verify(clickRepo).deleteByShortUrlId(1L);
    verify(shortUrlRepo).deleteById(1L);
  }

  @Test
  void delete_notFound() {
    when(shortUrlRepo.findById(999L)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> urlService.delete(999L, 1L, false));
  }

  @Test
  void deleteUser_deletesUserAndUrls() {
    ShortUrl url = new ShortUrl();
    url.setId(1L);
    url.setUserId(1L);

    when(shortUrlRepo.findByUserId(1L)).thenReturn(List.of(url));

    urlService.deleteUser(1L);

    verify(clickRepo).deleteByShortUrlId(1L);
    verify(shortUrlRepo).deleteByUserId(1L);
    verify(userRepo).deleteById(1L);
  }

  @Test
  void getTotalStats() {
    when(shortUrlRepo.count()).thenReturn(5L);
    when(shortUrlRepo.findAll()).thenReturn(List.of(
        createUrl(1L), createUrl(2L)
    ));
    when(clickRepo.countByShortUrlId(1L)).thenReturn(10L);
    when(clickRepo.countByShortUrlId(2L)).thenReturn(20L);
    when(userRepo.count()).thenReturn(3L);

    var stats = urlService.getTotalStats();

    assertEquals(3L, stats.get("totalUsers"));
    assertEquals(5L, stats.get("totalUrls"));
    assertEquals(30L, stats.get("totalClicks"));
  }

  private ShortUrl createUrl(Long id) {
    ShortUrl url = new ShortUrl();
    url.setId(id);
    return url;
  }
}
