package com.urlshortener.controller;

import com.urlshortener.dto.*;
import com.urlshortener.service.UrlService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/urls")
public class UrlApiController {

  private final UrlService urlService;

  public UrlApiController(UrlService urlService) {
    this.urlService = urlService;
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreateUrlRequest request) {
    try {
      UrlResponse response = urlService.createShortUrl(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
  }

  @GetMapping
  public ResponseEntity<Page<UrlResponse>> listAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    if (size > 200) size = 200;
    return ResponseEntity.ok(urlService.listAll(page, size));
  }

  @GetMapping("/{id}/analytics")
  public ResponseEntity<?> getAnalytics(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(urlService.getAnalytics(id));
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    urlService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
