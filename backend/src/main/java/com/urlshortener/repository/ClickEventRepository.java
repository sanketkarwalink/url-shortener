package com.urlshortener.repository;

import com.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

  long countByShortUrlId(Long shortUrlId);

  @Query("SELECT e FROM ClickEvent e WHERE e.shortUrl.id = :urlId ORDER BY e.clickedAt DESC")
  List<ClickEvent> findByShortUrlId(@Param("urlId") Long shortUrlId);

  @Query("SELECT CAST(e.clickedAt AS date) as day, COUNT(e) FROM ClickEvent e " +
      "WHERE e.shortUrl.id = :urlId AND e.clickedAt >= :since " +
      "GROUP BY CAST(e.clickedAt AS date) ORDER BY day")
  List<Object[]> dailyClicks(@Param("urlId") Long urlId, @Param("since") LocalDateTime since);

  @Query("SELECT e.deviceType, COUNT(e) FROM ClickEvent e " +
      "WHERE e.shortUrl.id = :urlId AND e.deviceType IS NOT NULL " +
      "GROUP BY e.deviceType")
  List<Object[]> deviceBreakdown(@Param("urlId") Long urlId);

  @Query("SELECT e.browser, COUNT(e) FROM ClickEvent e " +
      "WHERE e.shortUrl.id = :urlId AND e.browser IS NOT NULL " +
      "GROUP BY e.browser")
  List<Object[]> browserBreakdown(@Param("urlId") Long urlId);

  void deleteByShortUrlId(Long shortUrlId);

  @Query("SELECT e.referer, COUNT(e) FROM ClickEvent e " +
      "WHERE e.shortUrl.id = :urlId AND e.referer IS NOT NULL " +
      "GROUP BY e.referer ORDER BY COUNT(e) DESC")
  List<Object[]> refererBreakdown(@Param("urlId") Long urlId);
}
