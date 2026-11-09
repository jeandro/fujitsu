import { TestBed } from "@angular/core/testing";
import { EventListPage } from "./event-list.page";
import { EventService } from "../event.service";
import { signal, computed } from "@angular/core";
import { components } from "../../../core/api/schema";
import { ProblemSafe } from "../../../core/api/problem";

type EventResponse = components["schemas"]["EventResponse"];

function makeService() {
  return {
    canRead: () => true,
    canRegister: () => true,
    setPage: () => {},
    setSize: () => {},
    refresh: () => {},
    events: computed(() => [] as EventResponse[]),
    loading: signal(false),
    error: signal<ProblemSafe | null>(null),
    page: signal(0),
    size: signal(20),
    totalElements: computed(() => 0),
    totalPages: computed(() => 0),
  } as any;
}

describe("EventListPage", () => {
  let service: any;

  beforeEach(() => {
    service = makeService();
    TestBed.configureTestingModule({
      imports: [EventListPage],
      providers: [{ provide: EventService, useValue: service }],
    });
  });

  it("exibe estado de carregamento enquanto isLoading", () => {
    service.loading.set(true);
    const fixture = TestBed.createComponent(EventListPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Carregando eventos");
    expect(fixture.nativeElement.textContent).not.toContain("Nenhum evento");
  });

  it("exibe erro e não confunde com lista vazia", () => {
    service.error.set({ status: 0, type: "", title: "", detail: "Falha" });
    const fixture = TestBed.createComponent(EventListPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Não foi possível carregar os eventos");
    expect(fixture.nativeElement.textContent).not.toContain("Nenhum evento encontrado");
  });

  it("exibe estado vazio quando sem eventos", () => {
    service.events = computed(() => []);
    const fixture = TestBed.createComponent(EventListPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Nenhum evento encontrado.");
    expect(fixture.nativeElement.textContent).toContain("Novo evento");
  });

  it("exibe lista de eventos com sucesso e resumo Mostrando X de Y", () => {
    const events: EventResponse[] = [
      { eventId: "EVT-2", eventType: "MAINTENANCE", description: "Revisão", timestamp: "2026-05-20T13:10:00Z" },
      { eventId: "EVT-1", eventType: "PRODUCTION", description: "Linha Ativa", timestamp: "2026-05-20T13:00:00Z" },
    ];
    service.events = computed(() => events);
    service.totalElements = computed(() => 2);
    service.totalPages = computed(() => 1);
    const fixture = TestBed.createComponent(EventListPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("EVT-2");
    expect(fixture.nativeElement.textContent).toContain("EVT-1");
    expect(fixture.nativeElement.textContent).toContain("Mostrando 1 de 2 eventos.");
  });

  it("bloqueia consulta quando sem permissão (RN-14)", () => {
    spyOn(service, "canRead").and.returnValue(false);
    const fixture = TestBed.createComponent(EventListPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Sem permissão para consultar eventos");
  });

  it("pagina e altera tamanho da página", () => {
    const fixture = TestBed.createComponent(EventListPage);
    const component = fixture.componentInstance;
    spyOn(service, "setPage");
    spyOn(service, "setSize");
    component.setPage(1);
    fixture.detectChanges();
    expect(service.setPage).toHaveBeenCalledWith(1);
    component.setSize(50);
    fixture.detectChanges();
    expect(service.setSize).toHaveBeenCalledWith(50);
  });
});
