package com.urlshortener.config;

import static org.junit.jupiter.api.Assertions.*;

import com.urlshortener.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleBadRequest() {
    ResponseEntity<ErrorResponse> response =
        handler.handleBadRequest(new IllegalArgumentException("Bad input"));
    assertEquals(400, response.getStatusCode().value());
    assertEquals("Bad input", response.getBody().error());
  }

  @Test
  void handleNotFound() {
    ResponseEntity<ErrorResponse> response =
        handler.handleNotFound(new EntityNotFoundException("Not found"));
    assertEquals(404, response.getStatusCode().value());
    assertEquals("Not found", response.getBody().error());
  }

  @Test
  void handleNoResource() {
    ResponseEntity<ErrorResponse> response =
        handler.handleNoResource(new NoResourceFoundException(null, "Resource"));
    assertEquals(404, response.getStatusCode().value());
    assertEquals("Not found", response.getBody().error());
  }

  @Test
  void handleGeneralException() {
    ResponseEntity<ErrorResponse> response =
        handler.handleGeneral(new RuntimeException("Unexpected error"));
    assertEquals(500, response.getStatusCode().value());
    assertEquals("Internal server error", response.getBody().error());
  }
}
