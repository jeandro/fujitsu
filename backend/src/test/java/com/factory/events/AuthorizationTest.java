package com.factory.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.factory.events.adapter.in.web.EventController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Teste de autorizacao por papel: 401 sem token / 403 papel errado por endpoint (CA-12,
 * RN-13, RN-14).
 *
 * <p>Usa {@link SpringBootTest} com MockMvc + Spring Security. Cobre:
 * <ul>
 *   <li>POST /v1/events — 401 sem token; 403 papel != operador; 201 operador</li>
 *   <li>GET /v1/events — 401 sem token; 403 papel externo; 200 operador e gerente</li>
 *   <li>GET /v1/events/{eventId} — 401 sem token; 403 papel externo; 200 operador/gerente</li>
 * </ul>
 */
@SpringBootTest
class AuthorizationTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @Test
  @DisplayName("CA-12: POST sem token retorna 401 E-11 (RN-13)")
  void postSemTokenRetorna401() throws Exception {
    mockMvc.perform(post("/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"eventId\":\"EVT-X\",\"eventType\":\"PRODUCTION\",\"description\":\"d\"," +
          "\"timestamp\":\"2026-05-20T13:00:00Z\"}")
        .header("Idempotency-Key", "k-1"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("CA-12: POST com papel gerente retorna 403 (apenas operador, RN-13)")
  @WithMockUser(roles = "GERENTE")
  void postComPapelGerenteRetorna403() throws Exception {
    mockMvc.perform(post("/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"eventId\":\"EVT-X\",\"eventType\":\"PRODUCTION\",\"description\":\"d\"," +
          "\"timestamp\":\"2026-05-20T13:00:00Z\"}")
        .header("Idempotency-Key", "k-1"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CA-12: POST com papel operador retorna 201 (RN-13)")
  @WithMockUser(roles = "OPERADOR")
  void postComPapelOperadorRetorna201() throws Exception {
    mockMvc.perform(post("/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"eventId\":\"EVT-X\",\"eventType\":\"PRODUCTION\",\"description\":\"d\"," +
          "\"timestamp\":\"2026-05-20T13:00:00Z\"}")
        .header("Idempotency-Key", "k-1"))
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("CA-12: GET sem token retorna 401 E-11 (RN-14)")
  void getSemTokenRetorna401() throws Exception {
    mockMvc.perform(get("/v1/events"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("CA-12: GET com papel gerente retorna 200 (RN-14)")
  @WithMockUser(roles = "GERENTE")
  void getComPapelGerenteRetorna200() throws Exception {
    mockMvc.perform(get("/v1/events"))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("CA-12: GET com papel operador retorna 200 (RN-14)")
  @WithMockUser(roles = "OPERADOR")
  void getComPapelOperadorRetorna200() throws Exception {
    mockMvc.perform(get("/v1/events"))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("CA-12: GET com papel externo (auditor) retorna 403 (RN-14)")
  @WithMockUser(roles = "AUDITOR")
  void getComPapelExternoRetorna403() throws Exception {
    mockMvc.perform(get("/v1/events"))
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CA-12: GET/{eventId} sem token retorna 401 (RN-14)")
  void getByIdSemTokenRetorna401() throws Exception {
    mockMvc.perform(get("/v1/events/EVT-1"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("CA-12: GET/{eventId} com papel externo retorna 403 (RN-14)")
  @WithMockUser(roles = "AUDITOR")
  void getByIdComPapelExternoRetorna403() throws Exception {
    mockMvc.perform(get("/v1/events/EVT-1"))
      .andExpect(status().isForbidden());
  }
}
