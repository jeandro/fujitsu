import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { EventService } from "./event.service";
import { IdempotencyStore } from "../../core/idempotency";
import { AuthService } from "../../core/auth/auth.service";

describe("EventService", () => {
  let service: EventService;
  let http: HttpTestingController;
  let idempotency: jasmine.SpyObj<IdempotencyStore>;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    idempotency = jasmine.createSpyObj("IdempotencyStore", [
      "newKey",
      "postOnce",
    ]);
    auth = jasmine.createSpyObj("AuthService", ["canRegister", "canRead"]);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        EventService,
        { provide: IdempotencyStore, useValue: idempotency },
        { provide: AuthService, useValue: auth },
      ],
    });

    service = TestBed.inject(EventService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("deve listar eventos paginados ordenados por timestamp decrescente (RN-09)", () => {
    const req = http.expectOne((r) =>
      r.url.includes("/api/v1/events") &&
      r.params.get("sort") === "timestamp,desc",
    );
    expect(req.request.method).toBe("GET");
    req.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    expect(Array.isArray(service.events())).toBeTrue();
    expect(service.events().length).toBe(0);
  });

  it("deve definir page e size antes de consultar", () => {
    const req0 = http.expectOne((r) =>
      r.params.get("page") === "0" && r.params.get("size") === "20",
    );
    req0.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    service.setPage(2);
    service.setSize(50);
    const reqs = http.match((r) =>
      r.params.get("page") === "2" && r.params.get("size") === "50",
    );
    expect(reqs.length).toBeGreaterThan(0);
    expect(reqs[0].request.params.get("sort")).toBe("timestamp,desc");
    reqs.forEach((r) =>
      r.flush({
        content: [],
        page: 2,
        size: 50,
        totalElements: 0,
        totalPages: 0,
      }),
    );
  });

  it("deve limitar tamanho da página a 100 (RN-09)", () => {
    service.setSize(200);
    expect(service.size()).toBe(100);

    service.setSize(0);
    expect(service.size()).toBe(1);
  });

  it("deve consultar evento por ID", () => {
    const res = service.getEvent("EVT-1001");
    expect(res.value()).toBeUndefined();
    const req = http.expectOne("/api/v1/events/EVT-1001");
    expect(req.request.method).toBe("GET");
    req.flush({
      eventId: "EVT-1001",
      eventType: "PRODUCTION",
      description: "d",
      timestamp: "2026-05-20T13:00:00Z",
    });
    expect(res.value()?.eventId).toBe("EVT-1001");
  });

  it("deve consultar saga por ID", () => {
    const res = service.getSaga("550e8400-e29b-41d4-a716-446655440000");
    expect(res.value()).toBeUndefined();
    const req = http.expectOne(
      "/api/v1/sagas/550e8400-e29b-41d4-a716-446655440000",
    );
    expect(req.request.method).toBe("GET");
    req.flush({
      sagaId: "550e8400-e29b-41d4-a716-446655440000",
      status: "SUCCEEDED",
      createdAt: "2026-11-05T13:00:00Z",
      updatedAt: "2026-11-05T13:00:02Z",
      error: null,
    });
    expect(res.value()?.status).toBe("SUCCEEDED");
  });

  it("deve registrar evento com sucesso enviando Idempotency-Key (RN-03)", async () => {
    auth.canRegister.and.returnValue(true);
    idempotency.newKey.and.returnValue("key-123");
    idempotency.postOnce.and.resolveTo({
      eventId: "EVT-1001",
      eventType: "PRODUCTION",
      description: "Linha Ativa",
      timestamp: "2026-05-20T13:00:00Z",
    });

    const result = await service.registerEvent(
      "EVT-1001",
      "PRODUCTION",
      "Linha Ativa",
      "2026-05-20T13:00:00Z",
    );

    expect(idempotency.postOnce).toHaveBeenCalledWith(
      "/api/v1/events",
      jasmine.objectContaining({ eventId: "EVT-1001" }),
      "key-123",
    );
    expect(result.eventId).toBe("EVT-1001");
  });

  it("deve rejeitar registro quando não é operador (RN-13)", async () => {
    auth.canRegister.and.returnValue(false);
    await expectAsync(
      service.registerEvent("X", "PRODUCTION", "d", "2026-05-20T13:00:00Z"),
    ).toBeRejectedWithError("Somente o papel operador pode registrar eventos.");
    expect(idempotency.newKey).not.toHaveBeenCalled();
  });

  it("deve informar se pode ler (operador e gerente, RN-14)", () => {
    auth.canRead.and.returnValue(true);
    expect(service.canRead()).toBeTrue();
    auth.canRead.and.returnValue(false);
    expect(service.canRead()).toBeFalse();
  });

  it("deve informar se pode registrar (apenas operador, RN-13)", () => {
    auth.canRegister.and.returnValue(true);
    expect(service.canRegister()).toBeTrue();
    auth.canRegister.and.returnValue(false);
    expect(service.canRegister()).toBeFalse();
  });

  it("deve recarregar consulta explícita (refresh)", () => {
    const req0 = http.expectOne((r) =>
      r.params.get("page") === "0" && r.params.get("size") === "20",
    );
    req0.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    service.refresh();
    expect(service.page()).toBe(0);
    expect(service.size()).toBe(20);

    const req1 = http.expectOne((r) =>
      r.params.get("page") === "0" && r.params.get("size") === "20",
    );
    req1.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });
});
