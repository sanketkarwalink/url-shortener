package com.urlshortener.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

  private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil(
        "test-secret-key-that-is-at-least-32-characters-long!!",
        86400000L
    );
  }

  @Test
  void generateAndValidateToken() {
    String token = jwtUtil.generateToken(1L, "test@example.com", false);
    assertNotNull(token);
    assertTrue(jwtUtil.validateToken(token));
    assertEquals(1L, jwtUtil.getUserId(token));
    assertFalse(jwtUtil.isAdmin(token));
  }

  @Test
  void generateAdminToken() {
    String token = jwtUtil.generateToken(2L, "admin@example.com", true);
    assertTrue(jwtUtil.validateToken(token));
    assertEquals(2L, jwtUtil.getUserId(token));
    assertTrue(jwtUtil.isAdmin(token));
  }

  @Test
  void rejectInvalidToken() {
    assertFalse(jwtUtil.validateToken("invalid-token"));
    assertFalse(jwtUtil.validateToken(""));
    assertFalse(jwtUtil.validateToken(null));
  }

  @Test
  void rejectTamperedToken() {
    String token = jwtUtil.generateToken(1L, "test@example.com", false);
    String tampered = token.substring(0, token.length() - 5) + "XXXXX";
    assertFalse(jwtUtil.validateToken(tampered));
  }

  @Test
  void differentSecretsDontValidate() {
    JwtUtil other = new JwtUtil(
        "a-completely-different-secret-key-that-is-long-enough!",
        86400000L
    );
    String token = jwtUtil.generateToken(1L, "test@example.com", false);
    // Token signed with one key shouldn't validate with another
    assertFalse(other.validateToken(token));
  }
}
