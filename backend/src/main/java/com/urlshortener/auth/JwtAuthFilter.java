package com.urlshortener.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2)
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  public JwtAuthFilter(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    return path.equals("/api/auth/login")
        || path.equals("/api/auth/register")
        || path.equals("/actuator/health")
        || (method.equals("GET") && path.matches("^/[a-zA-Z0-9]{6}$"));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    String path = request.getRequestURI();
    String method = request.getMethod();

    String authHeader = request.getHeader("Authorization");

    if (method.equals("POST") && path.equals("/api/urls")) {
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
          request.setAttribute("userId", jwtUtil.getUserId(token));
        }
      }
      chain.doFilter(request, response);
      return;
    }

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      writeUnauthorized(response, "Missing or invalid Authorization header");
      return;
    }

    String token = authHeader.substring(7);
    if (!jwtUtil.validateToken(token)) {
      writeUnauthorized(response, "Invalid or expired token");
      return;
    }

    request.setAttribute("userId", jwtUtil.getUserId(token));
    chain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletResponse res, String msg) throws IOException {
    res.setStatus(401);
    res.setContentType("application/json");
    res.getWriter().write("{\"error\":\"" + msg + "\"}");
  }
}
