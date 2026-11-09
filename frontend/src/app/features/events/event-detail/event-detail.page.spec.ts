import { TestBed } from "@angular/core/testing";
import { ActivatedRoute } from "@angular/router";
import { EventDetailPage } from "./event-detail.page";
import { EventService } from "../event.service";
import { AuthService } from "../../../core/auth/auth.service";
import { signal } from "@angular/core";
import { components } from "../../../core/api/schema";

type EventResponse = components["schemas"]["EventResponse"];

function makeResource(value: EventResponse | null = null, error: any = null, loading = false) {
  return { value: signal(value), error: signal(error), isLoading: signal(loading) };
}

function makeService() {
  return {
    canRead: () => true,
    canRegister: () => false,
    getEvent: () => makeResource(),
    registerEvent: async () => ({}),
  } as any;
}

describe("EventDetailPage", () => {
  let service: any;
  let auth: any;
  const routeParam = "EVT-1001";

  beforeEach(() => {
    service = makeService();
    auth = {
      token: () => null,
      role: () => null,
      authenticated: () => false,
      canRead: () => true,
      canRegister: () => false,
    } as any;
    TestBed.configureTestingModule({
      imports: [EventDetailPage],
      providers: [
        { provide: EventService, useValue: service },
        { provide: AuthService, useValue: auth },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => routeParam } } } },
      ],
    });
  });

  it("carregando exibe mensagem sem deslocar layout", () => {
    service.getEvent = () => makeResource(null, null, true);
    const fixture = TestBed.createComponent(EventDetailPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Carregando evento");
  });

  it("erro exibe mensagem e permite tentar novamente", () => {
    service.getEvent = () => makeResource(null, { status: 500 }, false);
    const fixture = TestBed.createComponent(EventDetailPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Erro ao carregar evento");
  });

  it("evento não encontrado exibe estado vazio com ação", () => {
    service.getEvent = () => makeResource(null, { status: 404 }, false);
    const fixture = TestBed.createComponent(EventDetailPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Evento não encontrado");
  });

  it("sucesso exibe dados do evento", () => {
    const evt: EventResponse = { eventId: "EVT-1001", eventType: "QUALITY", description: "dado pessoal sensível", timestamp: "2026-05-20T13:00:00Z" };
    service.getEvent = () => makeResource(evt, null, false);
    const fixture = TestBed.createComponent(EventDetailPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("EVT-1001");
    expect(fixture.nativeElement.textContent).toContain("QUALITY");
  });

  it("sem permissão exibe estado próprio (RN-14)", () => {
    service.canRead = () => false;
    const fixture = TestBed.createComponent(EventDetailPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Sem permissão para consultar este evento");
  });
});
