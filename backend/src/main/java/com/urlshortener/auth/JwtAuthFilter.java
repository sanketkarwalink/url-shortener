package com.urlshortener.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
@Order(2)
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  private static final Pattern REDIRECT_PATH = Pattern.compile("^/[a-zA-Z0-9]{6}$");

  public JwtAuthFilter(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    String path = request.getRequestURI();
    String method = request.getMethod();

    boolean protectedPath =
        (method.equals("GET") && path.equals("/api/urls"))
        || (method.equals("GET") && path.matches("^/api/urls/\\d+/analytics$"))
        || (method.equals("DELETE") && path.matches("^/api/urls/\\d+$"))
        || (method.equals("GET") && path.equals("/api/urls/admin/stats"));

    if (!protectedPath) {
      if (method.equals("POST") && path.equals("/api/urls")) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
          String token = authHeader.substring(7);
          if (jwtUtil.validateToken(token)) {
            request.setAttribute("userId", jwtUtil.getUserId(token));
            request.setAttribute("admin", jwtUtil.isAdmin(token));
          }
        }
      }
      chain.doFilter(request, response);
      return;
    }

    String authHeader = request.getHeader("Authorization");
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
    request.setAttribute("admin", jwtUtil.isAdmin(token));
    chain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletResponse res, String msg) throws IOException {
    res.setStatus(401);
    res.setContentType("application/json");
    res.getWriter().write("{\"error\":\"" + msg + "\"}");
  }
}
