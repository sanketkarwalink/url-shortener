package com.urlshortener.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

  private static final Pattern CODE_PATTERN = Pattern.compile("^/[a-zA-Z0-9]{6}$");

  private final ConcurrentMap<String, SlidingWindow> createWindows = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, SlidingWindow> clickWindows = new ConcurrentHashMap<>();

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;
    String ip = request.getRemoteAddr();
    String uri = request.getRequestURI();
    String method = request.getMethod();

    if (method.equals("POST") && uri.equals("/api/urls")) {
      SlidingWindow w = createWindows.computeIfAbsent(ip, k -> new SlidingWindow(20));
      if (!w.tryAcquire()) {
        writeTooMany(response);
        return;
      }
    }

    if (method.equals("GET") && CODE_PATTERN.matcher(uri).matches()) {
      SlidingWindow w = clickWindows.computeIfAbsent(ip, k -> new SlidingWindow(60));
      if (!w.tryAcquire()) {
        writeTooMany(response);
        return;
      }
    }

    chain.doFilter(req, res);
  }

  private void writeTooMany(HttpServletResponse res) throws IOException {
    res.setStatus(429);
    res.setContentType("application/json");
    res.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
  }

  static class SlidingWindow {
    private final long windowMs;
    private final int limit;
    private final long[] timestamps;
    private int idx = 0;
    private int count = 0;

    SlidingWindow(int limit) {
      this.limit = limit;
      this.windowMs = 60_000;
      this.timestamps = new long[limit];
    }

    synchronized boolean tryAcquire() {
      long now = System.currentTimeMillis();
      timestamps[idx] = now;
      idx = (idx + 1) % limit;
      count++;
      if (count <= limit) return true;
      return (now - timestamps[idx]) >= windowMs;
    }
  }
}
