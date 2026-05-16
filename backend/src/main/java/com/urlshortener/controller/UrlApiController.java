package com.urlshortener.controller;

import com.urlshortener.dto.*;
import com.urlshortener.service.UrlService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
public class UrlApiController {

  private final UrlService urlService;

  public UrlApiController(UrlService urlService) {
    this.urlService = urlService;
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreateUrlRequest request,
                                  @RequestAttribute(name = "userId", required = false) Long userId) {
    try {
      UrlResponse response = urlService.createShortUrl(request, userId);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
  }

  @GetMapping
  public ResponseEntity<Page<UrlResponse>> listAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(defaultValue = "false") boolean all,
      @RequestAttribute("userId") Long userId,
      @RequestAttribute("admin") boolean admin
  ) {
    if (size > 200) size = 200;
    if (all && admin) return ResponseEntity.ok(urlService.listAllUrls(page, size));
    return ResponseEntity.ok(urlService.listAll(userId, page, size));
  }

  @GetMapping("/{id}/analytics")
  public ResponseEntity<?> getAnalytics(@PathVariable Long id,
                                        @RequestAttribute("userId") Long userId,
                                        @RequestAttribute("admin") boolean admin) {
    try {
      return ResponseEntity.ok(urlService.getAnalytics(id, userId, admin));
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id,
                                     @RequestAttribute("userId") Long userId,
                                     @RequestAttribute("admin") boolean admin) {
    urlService.delete(id, userId, admin);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/admin/stats")
  public ResponseEntity<?> adminStats(@RequestAttribute("userId") Long userId,
                                      @RequestAttribute("admin") boolean admin) {
    if (!admin) return ResponseEntity.status(403).body(new ErrorResponse("Forbidden"));
    return ResponseEntity.ok(urlService.getTotalStats());
  }

  @GetMapping("/admin/users")
  public ResponseEntity<?> adminUsers(@RequestAttribute("admin") boolean admin) {
    if (!admin) return ResponseEntity.status(403).body(new ErrorResponse("Forbidden"));
    return ResponseEntity.ok(urlService.listUsers());
  }

  @DeleteMapping("/admin/users/{userId}")
  public ResponseEntity<?> deleteUser(@PathVariable Long userId,
                                      @RequestAttribute("userId") Long myUserId,
                                      @RequestAttribute("admin") boolean admin) {
    if (!admin) return ResponseEntity.status(403).body(new ErrorResponse("Forbidden"));
    if (userId.equals(myUserId)) return ResponseEntity.badRequest().body(new ErrorResponse("Cannot delete yourself"));
    urlService.deleteUser(userId);
    return ResponseEntity.noContent().build();
  }
}
