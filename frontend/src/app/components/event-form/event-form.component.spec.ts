import { TestBed } from "@angular/core/testing";
import { Subject, of, throwError } from "rxjs";
import { HttpErrorResponse } from "@angular/common/http";
import { EventFormComponent } from "./event-form.component";
import { EventService } from "../../services/event.service";
import { EventType, FactoryEvent } from "../../models/event.model";

describe("EventFormComponent", () => {
  let service: jasmine.SpyObj<EventService>;
  beforeEach(() => {
    service = jasmine.createSpyObj("EventService", [
      "registerEvent",
      "notifyEventCreated",
    ]);
    TestBed.configureTestingModule({
      imports: [EventFormComponent],
      providers: [{ provide: EventService, useValue: service }],
    });
  });
  function form() {
    const fixture = TestBed.createComponent(EventFormComponent);
    fixture.detectChanges();
    return fixture;
  }
  it("desabilita formulário vazio e rejeita espaços", () => {
    const fixture = form();
    expect(fixture.nativeElement.querySelector("button").disabled).toBeTrue();
    const c = fixture.componentInstance;
    c.eventId = " ";
    c.description = " ";
    c.onSubmit();
    expect(service.registerEvent).not.toHaveBeenCalled();
  });
  it("impede duplo envio, preserva o ID da resposta e libera o formulário", () => {
    const fixture = form(),
      c = fixture.componentInstance;
    const result = new Subject<FactoryEvent>();
    service.registerEvent.and.returnValue(result);
    c.eventId = " Q ";
    c.description = "x".repeat(1000);
    c.eventType = EventType.QUALITY;
    c.onSubmit();
    c.onSubmit();
    fixture.detectChanges();
    expect(service.registerEvent).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector("fieldset").disabled).toBeTrue();
    const payload = service.registerEvent.calls.mostRecent().args[0];
    expect(payload.eventId).toBe("Q");
    expect(payload.timestamp.endsWith("Z")).toBeTrue();
    c.eventId = "changed";
    result.next({ ...payload, eventId: "Q" });
    result.complete();
    expect(c.successMessage).toContain("Evento Q ");
    expect(c.submitting).toBeFalse();
    expect(c.eventId).toBe("");
  });
  it("mostra erros por campo e mantém dados para correção", () => {
    const c = form().componentInstance;
    c.eventId = "X";
    c.description = "d";
    service.registerEvent.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { errors: { description: "Inválida" } },
          }),
      ),
    );
    c.onSubmit();
    expect(c.fieldErrors["description"]).toBe("Inválida");
    expect(c.description).toBe("d");
    expect(c.submitting).toBeFalse();
  });
  it("rejeita descrição maior que 1000 caracteres", () => {
    const c = form().componentInstance;
    c.eventId = "X";
    c.description = "x".repeat(1001);
    c.onSubmit();
    expect(service.registerEvent).not.toHaveBeenCalled();
  });
  it("expõe erro de campo para tecnologias assistivas", () => {
    const fixture = form();
    fixture.componentInstance.fieldErrors = {
      description: "Descrição inválida",
    };
    fixture.detectChanges();
    const field = fixture.nativeElement.querySelector("#description");
    expect(field.getAttribute("aria-invalid")).toBe("true");
    expect(field.getAttribute("aria-describedby")).toContain(
      "description-error",
    );
    expect(
      fixture.nativeElement.querySelector("#description-error").textContent,
    ).toContain("Descrição inválida");
  });
});
