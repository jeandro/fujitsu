package com.factory.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.factory.events.dto.EventRequestDTO;
import com.factory.events.model.EventType;
import com.factory.events.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestExecutionListeners(
  listeners = DependencyInjectionTestExecutionListener.class,
  mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
class EventApiTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper mapper;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  EventService service;

  String data(String id, String type, String description, String time)
    throws Exception {
    return mapper.writeValueAsString(
      Map.of(
        "eventId",
        id,
        "eventType",
        type,
        "description",
        description,
        "timestamp",
        time
      )
    );
  }

  String data(String id, String type, String desc) throws Exception {
    return data(id, type, desc, "2026-05-20T13:00:00Z");
  }

  int count(String table, String id) {
    return jdbc.queryForObject(
      "select count(*) from " + table + " where event_id=?",
      Integer.class,
      id
    );
  }

  @Test
  void productionAndQualityPersistWithCorrectAudit() throws Exception {
    mvc
      .perform(
        post("/api/events")
          .contentType("application/json")
          .content(data("API-P", "PRODUCTION", "p"))
      )
      .andExpect(status().isCreated());
    mvc
      .perform(
        post("/api/events")
          .contentType("application/json")
          .content(data("API-Q", "QUALITY", "x".repeat(1000)))
      )
      .andExpect(status().isCreated());
    assertEquals(0, count("tb_audit_logs", "API-P"));
    assertEquals(1, count("tb_audit_logs", "API-Q"));
    assertTrue(
      jdbc.queryForObject(
        "select length(log_message) from tb_audit_logs where event_id='API-Q'",
        Integer.class
      ) > 1000
    );
  }

  @Test
  void maintenanceDoesNotCreateAudit() throws Exception {
    mvc
      .perform(
        post("/api/events")
          .contentType("application/json")
          .content(data("API-M", "MAINTENANCE", "Manutencao"))
      )
      .andExpect(status().isCreated());
    assertEquals(1, count("tb_factory_events", "API-M"));
    assertEquals(0, count("tb_audit_logs", "API-M"));
  }

  @Test
  void duplicateAndTrimReturnConflict() throws Exception {
    mvc
      .perform(
        post("/api/events")
          .contentType("application/json")
          .content(data(" DUP ", "QUALITY", "d"))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.eventId").value("DUP"));
    mvc
      .perform(
        post("/api/events")
          .contentType("application/json")
          .content(data("DUP", "QUALITY", "d"))
      )
      .andExpect(status().isConflict());
    assertEquals(1, count("tb_audit_logs", "DUP"));
  }

  @Test
  void malformedAndInvalidInputsReturn400() throws Exception {
    for (String body : List.of(
      "{",
      "{}",
      data("BAD", "OTHER", "d"),
      data("BAD", "PRODUCTION", "d", "not-a-date"),
      data(" ", "PRODUCTION", " "),
      data("x".repeat(101), "PRODUCTION", "d"),
      data("LONG", "QUALITY", "x".repeat(1001))
    )) {
      mvc
        .perform(
          post("/api/events").contentType("application/json").content(body)
        )
        .andExpect(status().isBadRequest());
    }
  }

  @Test
  void handlesHttpSemantics() throws Exception {
    mvc
      .perform(put("/api/events"))
      .andExpect(status().isMethodNotAllowed())
      .andExpect(header().exists("Allow"));
    mvc
      .perform(post("/api/events").contentType("text/plain").content("x"))
      .andExpect(status().isUnsupportedMediaType());
    mvc.perform(get("/not-found")).andExpect(status().isNotFound());
  }

  @Test
  void swaggerAndSchemaAreAvailable() throws Exception {
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    mvc
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.paths['/api/events'].post").exists());
  }

  @Test
  void acceptsOffsetsAndLegacyTimestamp() throws Exception {
    mvc
      .perform(
        post("/api/events")
          .contentType("application/json")
          .content(
            data("TZ-OFFSET", "PRODUCTION", "d", "2026-05-20T10:00:00-03:00")
          )
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.timestamp").value("2026-05-20T13:00:00Z"));
    mvc
      .perform(
        post("/api/events")
          .contentType("application/json")
          .content(data("TZ-LOCAL", "PRODUCTION", "d", "2026-05-20T10:00:00"))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.timestamp").value("2026-05-20T13:00:00Z"));
    mvc
      .perform(get("/api/events"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].timestamp").exists());
  }

  @Test
  void concurrentDuplicateIsAlways409() throws Exception {
    String body = data("RACE", "QUALITY", "d");
    var barrier = new CyclicBarrier(12);
    ExecutorService pool = Executors.newFixedThreadPool(12);
    try {
      List<Future<Integer>> futures = new ArrayList<>();
      for (int i = 0; i < 12; i++) futures.add(
        pool.submit(() -> {
          barrier.await(10, TimeUnit.SECONDS);
          return mvc
            .perform(
              post("/api/events").contentType("application/json").content(body)
            )
            .andReturn()
            .getResponse()
            .getStatus();
        })
      );
      List<Integer> statuses = new ArrayList<>();
      for (var f : futures) statuses.add(f.get(30, TimeUnit.SECONDS));
      assertEquals(1, Collections.frequency(statuses, 201));
      assertEquals(
        11,
        Collections.frequency(statuses, 409),
        statuses.toString()
      );
      assertEquals(1, count("tb_factory_events", "RACE"));
      assertEquals(1, count("tb_audit_logs", "RACE"));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void auditFailureRollsBackWholeTransaction() {
    var dto = new EventRequestDTO();
    dto.setEventId("ROLLBACK");
    dto.setEventType(EventType.QUALITY);
    // Bypass HTTP validation deliberately to force a database failure in the audit write.
    dto.setDescription("x".repeat(980));
    dto.setTimestamp(Instant.now());
    jdbc.execute(
      "alter table tb_audit_logs add constraint qa_audit_failure check (event_id <> 'ROLLBACK')"
    );
    try {
      assertThrows(RuntimeException.class, () -> service.processEvent(dto));
      assertEquals(0, count("tb_factory_events", "ROLLBACK"));
    } finally {
      jdbc.execute(
        "alter table tb_audit_logs drop constraint qa_audit_failure"
      );
    }
  }

  @Test
  void listOrdersByTimestampThenInsertion() throws Exception {
    for (String id : List.of("ORDER-A", "ORDER-B")) {
      mvc
        .perform(
          post("/api/events")
            .contentType("application/json")
            .content(data(id, "PRODUCTION", "d", "2026-05-20T13:00:00Z"))
        )
        .andExpect(status().isCreated());
    }
    mvc
      .perform(
        post("/api/events")
          .contentType("application/json")
          .content(data("ORDER-OLD", "PRODUCTION", "d", "2025-01-01T00:00:00Z"))
      )
      .andExpect(status().isCreated());
    var response = mvc
      .perform(get("/api/events"))
      .andExpect(status().isOk())
      .andReturn();
    var items = mapper.readTree(response.getResponse().getContentAsString());
    List<String> ids = new ArrayList<>();
    items.forEach(item -> {
      if (item.get("eventId").asText().startsWith("ORDER-")) ids.add(
        item.get("eventId").asText()
      );
    });
    assertEquals(List.of("ORDER-B", "ORDER-A", "ORDER-OLD"), ids);
  }

  @Test
  void openApiDescribesActualErrorSchema() throws Exception {
    mvc
      .perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$.components.schemas.ApiError.properties.requestId").exists()
      )
      .andExpect(
        jsonPath(
          "$.paths['/api/events'].post.responses['409'].content['application/json'].schema['$ref']"
        ).value("#/components/schemas/ApiError")
      );
  }
}
