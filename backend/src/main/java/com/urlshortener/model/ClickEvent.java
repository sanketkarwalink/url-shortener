package com.urlshortener.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_events")
public class ClickEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "short_url_id", nullable = false)
  private ShortUrl shortUrl;

  @Column(nullable = false)
  private LocalDateTime clickedAt;

  @Column(length = 45)
  private String ipAddress;

  @Column(length = 512)
  private String userAgent;

  @Column(length = 512)
  private String referer;

  @Column(length = 50)
  private String deviceType;

  @Column(length = 50)
  private String browser;

  @Column(length = 50)
  private String os;

  @PrePersist
  void onClick() {
    this.clickedAt = LocalDateTime.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public ShortUrl getShortUrl() { return shortUrl; }
  public void setShortUrl(ShortUrl shortUrl) { this.shortUrl = shortUrl; }

  public LocalDateTime getClickedAt() { return clickedAt; }
  public void setClickedAt(LocalDateTime clickedAt) { this.clickedAt = clickedAt; }

  public String getIpAddress() { return ipAddress; }
  public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

  public String getUserAgent() { return userAgent; }
  public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

  public String getReferer() { return referer; }
  public void setReferer(String referer) { this.referer = referer; }

  public String getDeviceType() { return deviceType; }
  public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

  public String getBrowser() { return browser; }
  public void setBrowser(String browser) { this.browser = browser; }

  public String getOs() { return os; }
  public void setOs(String os) { this.os = os; }
}
