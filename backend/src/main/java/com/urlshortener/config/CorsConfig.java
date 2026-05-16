package com.urlshortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig {

  @Value("${app.cors-origins:http://localhost:3000}")
  private String corsOrigins;

  @Bean
  WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = List.of(corsOrigins.split(","));
        registry.addMapping("/api/**")
            .allowedOriginPatterns(origins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "DELETE")
            .allowedHeaders("*");
      }
    };
  }
}
