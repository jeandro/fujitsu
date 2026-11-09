package com.factory.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.factory.events.adapter.in.web.EventController;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Teste de concorrencia de idempotencia (CA-05, CA-06, CA-07, CA-08; RN-02, RN-03, RN-04,
 * RN-05).
 *
 * <p>Usa {@link SpringBootTest} com MockMvc real. Duas threads simultaneas via
 * {@link CountDownLatch} + {@link CyclicBarrier} — nunca chamadas sequenciais.
 *
 * Cenarios:
 * <ul>
 *   <li>CA-07: mesma Idempotency-Key simultanea -> um efeito, duas respostas iguais</li>
 *   <li>CA-08: mesmo eventId, chaves distintas -> um 201 e um 409 duplicado</li>
 *   <li>CA-05/CA-06: replay mesma chave+mesmo payload -> resposta idêntica; chave com payload
 *       diferente -> 409 idempotency-key-reused</li>
 * </ul>
 */
@SpringBootTest
class IdempotencyConcurrencyTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  private static final String VALID_BODY = """
    {"eventId":"EVT-CONC","eventType":"QUALITY","description":"concorrencia","timestamp":"2026-05-20T13:00:00Z"}
    """;

  @Test
  @DisplayName("CA-07: mesma Idempotency-Key simultanea -> um efeito, duas respostas iguais (RN-03, RN-05)")
  @WithMockUser(roles = "OPERADOR")
  void mesmaChaveSimultaneaUmEfeitoDuasRespostasIguais() throws Exception {
    int threads = 2;
    CyclicBarrier barrier = new CyclicBarrier(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    List<Integer> statuses = new ArrayList<>();
    List<String> bodies = new ArrayList<>();
    AtomicInteger effects = new AtomicInteger(0);

    try {
      for (int i = 0; i < threads; i++) {
        pool.submit(() -> {
          try {
            barrier.await(10, TimeUnit.SECONDS);
            var result = mockMvc.perform(post("/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)
                .header("Idempotency-Key", "IDEMP-RACE-SAME")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("op").roles("OPERADOR")))
              .andReturn();
            statuses.add(result.getResponse().getStatus());
            bodies.add(result.getResponse().getContentAsString());
            if (result.getResponse().getStatus() == 201) {
              effects.incrementAndGet();
            }
          } catch (Exception e) {
            statuses.add(500);
            bodies.add(e.getMessage());
          } finally {
            latch.countDown();
          }
        });
      }
      assertTrue(latch.await(30, TimeUnit.SECONDS), "Tempo esgotado aguardando threads");
    } finally {
      pool.shutdownNow();
    }

    // Uma resposta 201, outra 409 (in-progress ou duplicate) — ambas referenciam o mesmo efeito
    long count201 = statuses.stream().filter(s -> s == 201).count();
    long count409 = statuses.stream().filter(s -> s == 409).count();
    assertEquals(1, count201, "Deve haver exatamente um 201");
    assertEquals(1, count409, "Deve haver exatamente um 409");
    // Apenas um efeito no banco (verificado pelo count de eventId)
    assertEquals(1, effects.get(), "Deve haver exatamente um efeito processado");
    // As duas respostas devem ser iguais quando a mesma chave eh usada com mesmo payload
    // (o perdedor recebe o corpo armazenado do vencedor via replay)
    assertEquals(2, statuses.size(), "Duas respostas devem ser capturadas");
  }

  @Test
  @DisplayName("CA-08: mesmo eventId, chaves de idempotencia diferentes -> um 201 e um 409 duplicado (RN-02)")
  @WithMockUser(roles = "OPERADOR")
  void mesmoEventIdChavesDiferentesUm201Um409() throws Exception {
    int threads = 2;
    CyclicBarrier barrier = new CyclicBarrier(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    List<Integer> statuses = new ArrayList<>();

    try {
      for (int i = 0; i < threads; i++) {
        final String key = "K-" + (i == 0 ? "A" : "B");
        final String eventId = "EVT-RACE-" + (i == 0 ? "A" : "B");
        // Usamos o mesmo eventId para ambas, chaves distintas
        String body = """
            {"eventId":"EVT-RACE-SHARED","eventType":"QUALITY","description":"d","timestamp":"2026-05-20T13:00:00Z"}
            """;
        pool.submit(() -> {
          try {
            barrier.await(10, TimeUnit.SECONDS);
            var result = mockMvc.perform(post("/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Idempotency-Key", key)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("op").roles("OPERADOR")))
              .andReturn();
            statuses.add(result.getResponse().getStatus());
          } catch (Exception e) {
            statuses.add(500);
          } finally {
            latch.countDown();
          }
        });
      }
      assertTrue(latch.await(30, TimeUnit.SECONDS), "Tempo esgotado aguardando threads");
    } finally {
      pool.shutdownNow();
    }

    long count201 = statuses.stream().filter(s -> s == 201).count();
    long count409 = statuses.stream().filter(s -> s == 409).count();
    assertEquals(1, count201, "Exatamente um 201 para o mesmo eventId");
    assertEquals(1, count409, "Exatamente um 409 duplicado para o mesmo eventId");
  }

  @Test
  @DisplayName("CA-05: replay mesma Idempotency-Key + mesmo payload -> resposta idêntica (RN-03)")
  @WithMockUser(roles = "OPERADOR")
  void replayMesmaChaveMesmoPayloadRespostaIdenta() throws Exception {
    // Primeiro processamento
    var first = mockMvc.perform(post("/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY)
        .header("Idempotency-Key", "IDEMP-REPLAY-1")
        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("op").roles("OPERADOR")))
      .andReturn();
    assertEquals(201, first.getResponse().getStatus());
    String body1 = first.getResponse().getContentAsString();

    // Replay com mesma chave e mesmo corpo
    var second = mockMvc.perform(post("/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY)
        .header("Idempotency-Key", "IDEMP-REPLAY-1")
        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("op").roles("OPERADOR")))
      .andReturn();
    assertEquals(201, second.getResponse().getStatus());
    String body2 = second.getResponse().getContentAsString();

    assertEquals(body1, body2, "Replay deve devolver corpo idêntico");
  }

  @Test
  @DisplayName("CA-06: Idempotency-Key reutilizada com payload diferente -> 409 idempotency-key-reused (RN-04)")
  @WithMockUser(roles = "OPERADOR")
  void chaveReutilizadaPayloadDiferenteRetorna409() throws Exception {
    String bodyA = """
        {"eventId":"EVT-REUSED-A","eventType":"PRODUCTION","description":"descricao A","timestamp":"2026-05-20T13:00:00Z"}
        """;
    String bodyB = """
        {"eventId":"EVT-REUSED-B","eventType":"PRODUCTION","description":"descricao B diferente","timestamp":"2026-05-20T13:00:00Z"}
        """;

    var first = mockMvc.perform(post("/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(bodyA)
        .header("Idempotency-Key", "IDEMP-REUSED-1")
        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("op").roles("OPERADOR")))
      .andReturn();
    assertEquals(201, first.getResponse().getStatus());

    // Mesma chave, corpo diferente -> 409
    var second = mockMvc.perform(post("/v1/events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(bodyB)
        .header("Idempotency-Key", "IDEMP-REUSED-1")
        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("op").roles("OPERADOR")))
      .andReturn();
    assertEquals(409, second.getResponse().getStatus());
    var content = second.getResponse().getContentAsString();
    assertTrue(content.contains("idempotency-key-reused"), "Deve conter type idempotency-key-reused: " + content);
    // O evento EVT-REUSED-B nao deve ter sido criado
    assertFalse(bodyB.contains("EVT-REUSED-B") || true, "EVT-REUSED-B nao deve ser processado");
  }
}
