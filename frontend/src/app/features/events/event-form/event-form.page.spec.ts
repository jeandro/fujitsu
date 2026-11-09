import { TestBed } from "@angular/core/testing";
import { EventFormPage } from "./event-form.page";
import { EventService } from "../event.service";
import { HttpErrorResponse } from "@angular/common/http";

function makeService() {
  return {
    canRead: () => true,
    canRegister: () => true,
    registerEvent: async () => ({
      eventId: "EVT-X",
      eventType: "PRODUCTION",
      description: "d",
      timestamp: "2026-05-20T13:00:00Z",
    }),
  } as any;
}

describe("EventFormPage", () => {
  let service: any;

  beforeEach(() => {
    service = makeService();
    TestBed.configureTestingModule({
      imports: [EventFormPage],
      providers: [{ provide: EventService, useValue: service }],
    });
  });

  function form() {
    const fixture = TestBed.createComponent(EventFormPage);
    fixture.detectChanges();
    return fixture;
  }

  it("bloqueia registro quando sem permissão (RN-13)", () => {
    service.canRegister = () => false;
    const fixture = form();
    expect(fixture.nativeElement.textContent).toContain("Sem permissão para registrar eventos");
    expect(fixture.nativeElement.textContent).toContain("Acesse com o papel operador");
  });

  it("exibe título e resumo do formulário", () => {
    const fixture = form();
    expect(fixture.nativeElement.textContent).toContain("Registrar evento de fábrica");
    expect(fixture.nativeElement.textContent).toContain("Informe os dados do evento");
  });

  it("desabilita botão quando formulário inválido", () => {
    const fixture = form();
    const btn = fixture.nativeElement.querySelector("button[type='submit']");
    expect(btn.disabled).toBeTrue();
  });

  it("habilita botão quando formulário válido", () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: "EVT-1001",
      eventType: "PRODUCTION",
      description: "Linha Ativa",
      timestamp: "2026-05-20T13:00:00",
    });
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector("button[type='submit']");
    expect(btn.disabled).toBeFalse();
  });

  it("impedi duplo clique: um só POST com mesma Idempotency-Key", async () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: " Q ",
      eventType: "QUALITY",
      description: "x".repeat(100),
      timestamp: "2026-05-20T13:00:00",
    });
    fixture.detectChanges();

    const response = { eventId: "Q", eventType: "QUALITY", description: "x".repeat(100), timestamp: "2026-05-20T13:00:00Z" };
    spyOn(service, "registerEvent").and.resolveTo(response);

    c.onSubmit();
    c.onSubmit(); // duplo clique — submitting já é true, ignora
    fixture.detectChanges();

    expect(service.registerEvent).toHaveBeenCalledTimes(1);
    const args = service.registerEvent.calls.mostRecent().args;
    expect(args[0]).toBe(" Q ");
    expect(args[2]).toBe("x".repeat(100));
    expect(c.submitting()).toBeTrue();

    await c.onSubmit();
    expect(c.successMessage()).toBe("Evento registrado com sucesso.");
    // description nunca aparece no sucesso (RN-17/20)
    expect(c.successMessage()).not.toContain("x".repeat(100));
    expect(c.submitting()).toBeFalse();
  });

  it("E-03: idempotency-key-reused exibe 'Novo registro' com nova chave", async () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: "X",
      eventType: "PRODUCTION",
      description: "d",
      timestamp: "2026-05-20T13:00:00",
    });
    service.registerEvent = async () => {
      throw {
        status: 409,
        error: { type: "https://api.factory/events/errors/idempotency-key-reused" },
      } as HttpErrorResponse;
    };
    await c.onSubmit();
    expect(c.errorMessage()).toContain("dados diferentes");
    expect(c.errorType()).toBe("idempotency-key-reused");
    const btn = fixture.nativeElement.querySelector(".btn-error-action");
    expect(btn.textContent).toContain("Novo registro");
    expect(c.submitting()).toBeFalse();
  });

  it("E-04: idempotency-in-progress aguarda sem reenvio, botão desabilitado", async () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: "X",
      eventType: "PRODUCTION",
      description: "d",
      timestamp: "2026-05-20T13:00:00",
    });
    service.registerEvent = async () => {
      throw {
        status: 409,
        error: { type: "https://api.factory/events/errors/idempotency-in-progress" },
      } as HttpErrorResponse;
    };
    await c.onSubmit();
    expect(c.errorMessage()).toContain("em andamento");
    expect(c.errorType()).toBe("idempotency-in-progress");
    expect(c.awaitingResolution()).toBeTrue();
    expect(c.submitting()).toBeTrue(); // botão permanece desabilitado
    const btn = fixture.nativeElement.querySelector("button[type='submit']");
    expect(btn.disabled).toBeTrue();
    // Não deve haver botão de reenvio
    expect(fixture.nativeElement.querySelector(".btn-error-action")).toBeNull();
  });

  it("E-05: audit-compensation-required exibe 'Tentar registro novamente'", async () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: "X",
      eventType: "QUALITY",
      description: "d",
      timestamp: "2026-05-20T13:00:00",
    });
    service.registerEvent = async () => {
      throw {
        status: 500,
        error: { type: "https://api.factory/events/errors/audit-compensation-required" },
      } as HttpErrorResponse;
    };
    await c.onSubmit();
    expect(c.errorMessage()).toContain("compensação foi executada e auditada");
    expect(c.errorType()).toBe("audit-compensation-required");
    const btn = fixture.nativeElement.querySelector(".btn-error-action");
    expect(btn.textContent).toContain("Tentar registro novamente");
  });

  it("E-02: duplicate-event exibe mensagem e ação Corrigir ID", async () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: "X",
      eventType: "PRODUCTION",
      description: "d",
      timestamp: "2026-05-20T13:00:00",
    });
    service.registerEvent = async () => {
      throw {
        status: 409,
        error: { type: "https://api.factory/events/errors/duplicate-event" },
      } as HttpErrorResponse;
    };
    await c.onSubmit();
    expect(c.errorMessage()).toContain("já foi processado");
    const btn = fixture.nativeElement.querySelector(".btn-error-action");
    expect(btn.textContent).toContain("Corrigir ID");
  });

  it("E-13: 500 genérico exibe mensagem de erro interno", async () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: "X",
      eventType: "PRODUCTION",
      description: "d",
      timestamp: "2026-05-20T13:00:00",
    });
    service.registerEvent = async () => {
      throw {
        status: 500,
        error: { type: "https://api.factory/events/errors/internal-error" },
      } as HttpErrorResponse;
    };
    await c.onSubmit();
    expect(c.errorMessage()).toContain("Erro interno");
    expect(c.errorMessage()).not.toContain("stack");
    expect(c.errorMessage()).not.toContain("SQL");
  });

  it("E-11: 401 exibe mensagem e ação Fazer login", async () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: "X",
      eventType: "PRODUCTION",
      description: "d",
      timestamp: "2026-05-20T13:00:00",
    });
    service.registerEvent = async () => {
      throw {
        status: 401,
        error: { type: "https://api.factory/events/errors/unauthorized" },
      } as HttpErrorResponse;
    };
    await c.onSubmit();
    expect(c.errorMessage()).toContain("Autenticação necessária");
    const btn = fixture.nativeElement.querySelector(".btn-error-action");
    expect(btn.textContent).toContain("Fazer login");
  });

  it("rejeita eventId maior que 100 caracteres", () => {
    const fixture = form();
    const c = fixture.componentInstance;
    spyOn(service, "registerEvent");
    c.eventId.setValue("x".repeat(101));
    c.description.setValue("valido");
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector("button[type='submit']");
    expect(btn.disabled).toBeTrue();
    c.onSubmit();
    expect(service.registerEvent).not.toHaveBeenCalled();
  });

  it("expõe erro de campo para tecnologias assistivas (aria-describedby / aria-invalid)", () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.description.setValue("invalido".repeat(200));
    c.description.markAsTouched();
    fixture.detectChanges();
    const field = fixture.nativeElement.querySelector("#description");
    expect(field.getAttribute("aria-invalid")).toBe("true");
    expect(fixture.nativeElement.querySelector("#description-error").textContent).toContain("1000 caracteres");
  });

  it("E-01 mensagens exatas por campo", () => {
    const fixture = form();
    const c = fixture.componentInstance;

    // eventId obrigatório
    c.eventId.markAsTouched();
    fixture.detectChanges();
    expect(c.eventIdErrors()).toBe("O ID do evento é obrigatório.");

    // eventId acima de 100
    c.eventId.setValue("x".repeat(101));
    c.eventId.markAsTouched();
    fixture.detectChanges();
    expect(c.eventIdErrors()).toBe("O ID do evento deve ter até 100 caracteres.");

    // description obrigatório
    c.description.markAsTouched();
    fixture.detectChanges();
    expect(c.descriptionErrors()).toBe("A descrição é obrigatória.");

    // timestamp obrigatório
    c.timestamp.markAsTouched();
    fixture.detectChanges();
    expect(c.timestampErrors()).toBe("O timestamp é obrigatório.");

    // eventType obrigatório
    c.eventType.markAsTouched();
    fixture.detectChanges();
    expect(c.eventTypeErrors()).toBe("O tipo do evento é obrigatório.");
  });

  it("Limpar campos reseta o formulário", () => {
    const fixture = form();
    const c = fixture.componentInstance;
    c.form.setValue({
      eventId: "EVT-1",
      eventType: "QUALITY",
      description: "alguma coisa",
      timestamp: "2026-05-20T13:00:00",
    });
    c.clearFields();
    expect(c.form.get("eventId")?.value).toBe("");
    expect(c.form.get("description")?.value).toBe("");
  });
});
