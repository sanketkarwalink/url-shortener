package com.urlshortener.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.urlshortener.auth.JwtUtil;
import com.urlshortener.dto.AuthResponse;
import com.urlshortener.dto.ErrorResponse;
import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final GoogleIdTokenVerifier googleVerifier;

  public AuthController(
      UserRepository userRepo,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil,
      @Value("${app.google.client-id}") String googleClientId
  ) {
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.googleVerifier = new GoogleIdTokenVerifier.Builder(
        new NetHttpTransport(), GsonFactory.getDefaultInstance()
    ).setAudience(Collections.singletonList(googleClientId)).build();
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
    if (user == null || user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      return ResponseEntity.status(401).body(new ErrorResponse("Invalid email or password"));
    }
    String token = jwtUtil.generateToken(user.getId(), user.getEmail());
    return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail()));
  }

  @PostMapping("/google")
  public ResponseEntity<?> googleAuth(@RequestBody com.urlshortener.dto.GoogleAuthRequest request) {
    try {
      GoogleIdToken idToken = googleVerifier.verify(request.credential());
      if (idToken == null) {
        return ResponseEntity.status(401).body(new ErrorResponse("Invalid Google token"));
      }
      GoogleIdToken.Payload payload = idToken.getPayload();
      String email = payload.getEmail();
      String name = (String) payload.get("name");

      User user = userRepo.findByEmail(email).orElseGet(() -> {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setPasswordHash(null);
        return userRepo.save(newUser);
      });

      String token = jwtUtil.generateToken(user.getId(), user.getEmail());
      return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail()));
    } catch (Exception e) {
      return ResponseEntity.status(401).body(new ErrorResponse("Google authentication failed"));
    }
  }
}
