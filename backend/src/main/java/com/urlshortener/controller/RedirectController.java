package com.urlshortener.controller;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.service.UrlService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.regex.Pattern;

@RestController
public class RedirectController {

  private static final Pattern CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{6}$");

  private final UrlService urlService;

  public RedirectController(UrlService urlService) {
    this.urlService = urlService;
  }

  @GetMapping("/{shortCode}")
  public ResponseEntity<Void> redirect(
      @PathVariable String shortCode,
      HttpServletRequest request
  ) {
    if (!CODE_PATTERN.matcher(shortCode).matches()) {
      return ResponseEntity.notFound().build();
    }
    try {
      ShortUrl url = urlService.resolveCached(shortCode);
      if (url.isExpired()) {
        return ResponseEntity.status(HttpStatus.GONE).build();
      }
      urlService.resolveAndTrack(shortCode, request);
      HttpHeaders headers = new HttpHeaders();
      headers.setLocation(URI.create(url.getOriginalUrl()));
      return new ResponseEntity<>(headers, HttpStatus.FOUND);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
