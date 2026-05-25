package com.urlshortener.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ShortUrlTest {

  @Test
  void isExpired_returnsTrueWhenPastExpiry() {
    ShortUrl url = new ShortUrl();
    url.setExpiresAt(LocalDateTime.now().minusHours(1));
    assertTrue(url.isExpired());
  }

  @Test
  void isExpired_returnsFalseWhenFutureExpiry() {
    ShortUrl url = new ShortUrl();
    url.setExpiresAt(LocalDateTime.now().plusHours(1));
    assertFalse(url.isExpired());
  }

  @Test
  void isExpired_returnsFalseWhenNoExpiry() {
    ShortUrl url = new ShortUrl();
    url.setExpiresAt(null);
    assertFalse(url.isExpired());
  }

  @Test
  void isExpired_returnsFalseWhenExpiryIsNow() {
    ShortUrl url = new ShortUrl();
    url.setExpiresAt(LocalDateTime.now().plusSeconds(5));
    assertFalse(url.isExpired());
  }

  @Test
  void onCreate_setsCreatedAt() {
    ShortUrl url = new ShortUrl();
    url.onCreate();
    assertNotNull(url.getCreatedAt());
  }

  @Test
  void shortCode_setAndGet() {
    ShortUrl url = new ShortUrl();
    url.setShortCode("abc123");
    assertEquals("abc123", url.getShortCode());
  }

  @Test
  void originalUrl_setAndGet() {
    ShortUrl url = new ShortUrl();
    url.setOriginalUrl("https://example.com");
    assertEquals("https://example.com", url.getOriginalUrl());
  }
}
