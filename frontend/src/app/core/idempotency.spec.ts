import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { IdempotencyStore } from "./idempotency";

describe("IdempotencyStore", () => {
  let store: IdempotencyStore;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), IdempotencyStore],
    });
    store = TestBed.inject(IdempotencyStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("deve gerar chave UUID v4", () => {
    const k1 = store.newKey();
    const k2 = store.newKey();
    expect(k1).not.toBe(k2);
    expect(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(k1)).toBeTrue();
  });

  it("deve retornar replay (mesma chave + mesmo corpo) sem reenviar HTTP", async () => {
    const response = {
      eventId: "EVT-1",
      eventType: "PRODUCTION" as const,
      description: "d",
      timestamp: "2026-05-20T13:00:00Z",
    };
    const body = { eventId: "EVT-1", eventType: "PRODUCTION" as const, description: "d", timestamp: "2026-05-20T13:00:00Z" };
    const key = store.newKey();
    const bodyHash = await (store as any).bodyHash(body);

    // Armazena manualmente (simula primeira conclusão) — nunca guarda description em memória
    await (store as any).store(key, bodyHash, response);

    expect(await store.hasReplay(key, bodyHash)).toBeTrue();
    expect(await store.getReplay(key, bodyHash)).toEqual(response);

    // Corpo diferente com mesma chave → não é replay
    const bodyB = { ...body, eventId: "EVT-2" };
    const bodyHashB = await (store as any).bodyHash(bodyB);
    expect(await store.hasReplay(key, bodyHashB)).toBeFalse();

    // Nenhuma requisição HTTP foi feita
    http.expectNone("/api/v1/events");
  });

  it("deve marcar e limpar pendente, e detectar execução em andamento", () => {
    const key = store.newKey();
    const promise = Promise.resolve({
      eventId: "X",
      eventType: "PRODUCTION" as const,
      description: "d",
      timestamp: "2026-05-20T13:00:00Z",
    });

    expect(store.isInProgress(key)).toBeFalse();
    (store as any).markPending(key, "hash-abc", promise as any);
    expect(store.isInProgress(key)).toBeTrue();
    store.clearPending(key);
    expect(store.isInProgress(key)).toBeFalse();
  });

  it("postOnce: primeira chamada envia HTTP com Idempotency-Key", async () => {
    const key = store.newKey();
    const body = { eventId: "EVT-1", eventType: "PRODUCTION" as const, description: "d", timestamp: "2026-05-20T13:00:00Z" };
    const response = { ...body, eventId: "EVT-1" };

    const promise = store.postOnce("/api/v1/events", body, key);

    const req = http.expectOne("/api/v1/events");
    expect(req.request.method).toBe("POST");
    expect(req.request.headers.get("Idempotency-Key")).toBe(key);
    expect(req.request.body).toEqual(body);
    req.flush(response);

    const result = await promise;
    expect(result).toEqual(response);

    // Replay agora disponível
    const bodyHash = await (store as any).bodyHash(body);
    expect(await store.hasReplay(key, bodyHash)).toBeTrue();
  });

  it("postOnce: replay com mesma chave + mesmo corpo não reenvia HTTP", async () => {
    const key = store.newKey();
    const body = { eventId: "EVT-1", eventType: "PRODUCTION" as const, description: "d", timestamp: "2026-05-20T13:00:00Z" };
    const response = { ...body };

    // Primeira chamada
    const p1 = store.postOnce("/api/v1/events", body, key);
    const req1 = http.expectOne("/api/v1/events");
    req1.flush(response);
    await p1;

    // Segunda chamada (replay) — não deve fazer HTTP
    const p2 = store.postOnce("/api/v1/events", body, key);
    const result2 = await p2;
    expect(result2).toEqual(response);

    http.expectNone("/api/v1/events");
  });

  it("postOnce: mesma chave com corpo diferente → backend responde 409 idempotency-key-reused", async () => {
    const key = store.newKey();
    const bodyA = { eventId: "EVT-1", eventType: "PRODUCTION" as const, description: "A", timestamp: "2026-05-20T13:00:00Z" };
    const bodyB = { eventId: "EVT-2", eventType: "PRODUCTION" as const, description: "B", timestamp: "2026-05-20T13:00:00Z" };
    const responseA = { ...bodyA };

    // Primeira chamada concluída
    const p1 = store.postOnce("/api/v1/events", bodyA, key);
    const req1 = http.expectOne("/api/v1/events");
    req1.flush(responseA);
    await p1;

    // Segunda chamada com corpo diferente → vai para o HTTP (backend devolve 409)
    const p2 = store.postOnce("/api/v1/events", bodyB, key);
    const req2 = http.expectOne("/api/v1/events");
    expect(req2.request.headers.get("Idempotency-Key")).toBe(key);
    req2.flush(
      { type: "https://api.factory/events/errors/idempotency-key-reused" },
      { status: 409, statusText: "Conflict" },
    );

    await expectAsync(p2).toBeRejected();
    http.expectNone("/api/v1/events");
  });

  it("postOnce: mesma chave concorrente aguarda a promessa em andamento", async () => {
    const key = store.newKey();
    const body = { eventId: "EVT-1", eventType: "PRODUCTION" as const, description: "d", timestamp: "2026-05-20T13:00:00Z" };
    const response = { ...body };

    let resolveFirst!: (v: unknown) => void;
    const firstPromise = new Promise<unknown>((res) => (resolveFirst = res));

    // Simula primeira em andamento
    (store as any).markPending(key, await (store as any).bodyHash(body), firstPromise as any);

    // Segunda chamada com mesma chave + mesmo corpo deve aguardar a primeira
    const p2 = store.postOnce("/api/v1/events", body, key);

    // Nenhum HTTP ainda (aguarda a primeira)
    http.expectNone("/api/v1/events");

    // Conclui a primeira
    resolveFirst(response);
    await firstPromise;

    const result2 = await p2;
    expect(result2).toEqual(response);

    http.expectNone("/api/v1/events");
  });

  it("não armazena description (PII) no mapa de replay", async () => {
    const key = store.newKey();
    const body = { eventId: "EVT-1", eventType: "PRODUCTION" as const, description: "dado confidencial LGPD", timestamp: "2026-05-20T13:00:00Z" };
    const response = { ...body };

    const p1 = store.postOnce("/api/v1/events", body, key);
    const req1 = http.expectOne("/api/v1/events");
    req1.flush(response);
    await p1;

    // O mapa de completed deve conter bodyHash, não o corpo com description
    const completed = (store as any).completed;
    const entry = completed.get(key);
    expect(entry.bodyHash).toBeDefined();
    expect(entry.bodyHash).not.toContain("dado confidencial");
    expect(JSON.stringify(entry)).not.toContain("dado confidencial");
  });
});
