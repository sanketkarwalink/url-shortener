package com.urlshortener.config;

import com.urlshortener.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse(msg));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.internalServerError()
        .body(new ErrorResponse("Internal server error"));
  }
}
