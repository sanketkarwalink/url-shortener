package com.urlshortener.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 2048)
  private String originalUrl;

  @Column(nullable = false, unique = true, length = 10)
  private String shortCode;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column
  private Long userId;

  @Column
  private LocalDateTime expiresAt;

  @OneToMany(mappedBy = "shortUrl", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private List<ClickEvent> clickEvents = new ArrayList<>();

  @PrePersist
  void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getOriginalUrl() { return originalUrl; }
  public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

  public String getShortCode() { return shortCode; }
  public void setShortCode(String shortCode) { this.shortCode = shortCode; }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }

  public LocalDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

  public boolean isExpired() {
    return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
  }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
