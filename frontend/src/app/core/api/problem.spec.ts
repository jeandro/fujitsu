import { HttpErrorResponse } from "@angular/common/http";
import {
  problemMessage,
  isIdempotencyKeyReused,
  isIdempotencyInProgress,
  isDuplicateEvent,
  isAuditCompensationRequired,
  errorMessageFor,
  fieldErrorMessage,
} from "./problem";

describe("problemMessage", () => {
  it("erro de rede (status 0) retorna mensagem de conexão sem vazar detalhe", () => {
    const m = problemMessage(new HttpErrorResponse({ status: 0 }));
    expect(m.status).toBe(0);
    expect(m.detail).toContain("conectar");
    expect(m.type).toContain("errors/");
  });

  it("5xx não espelha detalhe interno (sem PII/stack)", () => {
    const m = problemMessage(
      new HttpErrorResponse({
        status: 500,
        error: { message: "SQL SECRET", stack: "at X" },
      }),
    );
    expect(m.status).toBe(500);
    expect(m.detail).not.toContain("SQL SECRET");
    expect(m.detail).not.toContain("stack");
    expect(m.detail).toContain("Tente novamente mais tarde");
  });

  it("409 idempotency-key-reused reconhecido", () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: {
        type: "https://api.factory/events/errors/idempotency-key-reused",
        title: "Chave reutilizada",
        detail: "Payload diferente",
      },
    });
    expect(isIdempotencyKeyReused(err)).toBeTrue();
    expect(isDuplicateEvent(err)).toBeFalse();
    expect(isIdempotencyInProgress(err)).toBeFalse();
  });

  it("409 idempotency-in-progress reconhecido", () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: {
        type: "https://api.factory/events/errors/idempotency-in-progress",
        detail: "Em andamento",
      },
    });
    expect(isIdempotencyInProgress(err)).toBeTrue();
    expect(isIdempotencyKeyReused(err)).toBeFalse();
  });

  it("409 duplicate-event reconhecido", () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: {
        type: "https://api.factory/events/errors/duplicate-event",
        detail: "Duplicado",
      },
    });
    expect(isDuplicateEvent(err)).toBeTrue();
  });

  it("500 audit-compensation-required reconhecido", () => {
    const err = new HttpErrorResponse({
      status: 500,
      error: {
        type: "https://api.factory/events/errors/audit-compensation-required",
        detail: "Compensação executada",
      },
    });
    expect(isAuditCompensationRequired(err)).toBeTrue();
  });

  it("400 retorna detail do corpo", () => {
    const m = problemMessage(
      new HttpErrorResponse({
        status: 400,
        error: { type: "validation", detail: "Campo inválido", errors: { eventId: "obrigatório" } },
      }),
    );
    expect(m.status).toBe(400);
    expect(m.detail).toBe("Campo inválido");
    expect(m.errors?.["eventId"]).toBe("obrigatório");
  });
});

describe("errorMessageFor (textos finais E-01..E-13)", () => {
  it("E-03 idempotency-key-reused", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/idempotency-key-reused", 409),
    ).toBe(
      "Esta ação já foi processada com dados diferentes. Reinicie o registro com uma nova ação.",
    );
  });

  it("E-04 idempotency-in-progress", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/idempotency-in-progress", 409),
    ).toBe("O registro já está em andamento. Aguarde a conclusão.");
  });

  it("E-02 duplicate-event", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/duplicate-event", 409),
    ).toBe("O evento com este ID já foi processado anteriormente.");
  });

  it("E-05 audit-compensation-required", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/audit-compensation-required", 500),
    ).toBe(
      "Não foi possível concluir o registro: o log de auditoria QUALITY não pôde ser gravado. A compensação foi executada e auditada. O evento não foi persistido.",
    );
  });

  it("E-09 payload-too-large", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/payload-too-large", 413),
    ).toBe(
      "O corpo da requisição excede o limite de 1 MB. Reduza a descrição e tente novamente.",
    );
  });

  it("E-10 too-many-requests", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/too-many-requests", 429),
    ).toBe(
      "Limite de 100 requisições por minuto excedido. Tente novamente mais tarde.",
    );
  });

  it("E-11 unauthorized", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/unauthorized", 401),
    ).toBe("Autenticação necessária para acessar este recurso.");
  });

  it("E-12 forbidden", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/forbidden", 403),
    ).toBe("Seu papel não tem permissão para executar esta ação.");
  });

  it("E-13 internal-error (5xx sem detalhe técnico)", () => {
    expect(
      errorMessageFor("https://api.factory/events/errors/internal-error", 500),
    ).toBe(
      "Erro interno. Tente novamente ou informe o identificador da requisição ao suporte.",
    );
  });

  it("tipo desconhecido retorna erro interno genérico", () => {
    expect(errorMessageFor(undefined, 500)).toContain("Erro interno");
    expect(errorMessageFor("https://api.factory/events/errors/unknown", 500)).toContain("Erro interno");
  });
});

describe("fieldErrorMessage (E-01)", () => {
  it("required", () => {
    expect(fieldErrorMessage("ID do evento", "required")).toBe("O ID do evento é obrigatório.");
    expect(fieldErrorMessage("descrição", "required")).toBe("A descrição é obrigatória.");
    expect(fieldErrorMessage("tipo do evento", "required")).toBe("O tipo do evento é obrigatório.");
    expect(fieldErrorMessage("timestamp", "required")).toBe("O timestamp é obrigatório.");
  });

  it("maxlength", () => {
    expect(fieldErrorMessage("ID do evento", "maxlength", 100)).toBe("O ID do evento deve ter até 100 caracteres.");
    expect(fieldErrorMessage("descrição", "maxlength", 1000)).toBe("A descrição deve ter até 1000 caracteres.");
  });

  it("format timestamp", () => {
    expect(fieldErrorMessage("timestamp", "format")).toBe("Informe um timestamp válido (formato ISO 8601).");
  });
});
