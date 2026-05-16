package com.urlshortener.controller;

import com.urlshortener.auth.JwtUtil;
import com.urlshortener.dto.AuthResponse;
import com.urlshortener.dto.ErrorResponse;
import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    if (userRepo.existsByEmail(request.email())) {
      return ResponseEntity.badRequest().body(new ErrorResponse("Email already registered"));
    }
    User user = new User();
    user.setEmail(request.email().strip().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    userRepo.save(user);
    String token = jwtUtil.generateToken(user.getId(), user.getEmail());
    return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, user.getId(), user.getEmail()));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    User user = userRepo.findByEmail(request.email().strip().toLowerCase()).orElse(null);
    if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      return ResponseEntity.status(401).body(new ErrorResponse("Invalid email or password"));
    }
    String token = jwtUtil.generateToken(user.getId(), user.getEmail());
    return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail()));
  }
}
