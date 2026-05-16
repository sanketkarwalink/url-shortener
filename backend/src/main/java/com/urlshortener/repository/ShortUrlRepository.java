package com.urlshortener.repository;

import com.urlshortener.model.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
  Optional<ShortUrl> findByShortCode(String shortCode);
  boolean existsByShortCode(String shortCode);
  Page<ShortUrl> findByUserId(Long userId, Pageable pageable);
  boolean existsByIdAndUserId(Long id, Long userId);
  long count();
}
