package com.urlshortener.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
class RedirectControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ShortUrlRepository shortUrlRepo;

  @BeforeEach
  void setUp() {
    shortUrlRepo.deleteAll();
  }

  @Test
  void redirect_validCode() throws Exception {
    // Create a URL first
    var createReq = new CreateUrlRequest("https://example.com/redirect-test", null);
    String createBody = mockMvc.perform(get("/test")  // Force via POST
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andReturn().getResponse().getContentAsString();

    // Create via POST
    String resp = mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String shortCode = objectMapper.readTree(resp).get("shortCode").asText();

    // Follow redirect
    mockMvc.perform(get("/{shortCode}", shortCode))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "https://example.com/redirect-test"));
  }

  @Test
  void redirect_invalidCodeFormat() throws Exception {
    // Codes must be exactly 6 alphanumeric characters
    mockMvc.perform(get("/abc"))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/abcdefg"))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/abc-def"))
        .andExpect(status().isNotFound());
  }

  @Test
  void redirect_nonexistentCode() throws Exception {
    mockMvc.perform(get("/XXXXXX"))
        .andExpect(status().isNotFound());
  }

  @Test
  void healthEndpoint() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Test
  void redirect_expiredUrl() throws Exception {
    // Create a ShortUrl with a past expiry directly via the repository
    ShortUrl url = new ShortUrl();
    url.setOriginalUrl("https://example.com/expired");
    url.setShortCode("expire");
    url.setExpiresAt(LocalDateTime.now().minusMinutes(5));
    shortUrlRepo.save(url);

    // The redirect should return 410 Gone
    mockMvc.perform(get("/expire"))
        .andExpect(status().isGone());
  }
}
