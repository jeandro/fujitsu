import { TestBed } from "@angular/core/testing";
import { ActivatedRoute } from "@angular/router";
import { SagaStatusPage } from "./saga-status.page";
import { EventService } from "../event.service";
import { signal } from "@angular/core";
import { components } from "../../../core/api/schema";

type SagaState = components["schemas"]["SagaState"];

function makeResource(value: SagaState | null = null, error: any = null, loading = false) {
  return { value: signal(value), error: signal(error), isLoading: signal(loading) };
}

function makeService() {
  return {
    canRead: () => true,
    canRegister: () => false,
    getSaga: () => makeResource(),
  } as any;
}

describe("SagaStatusPage", () => {
  let service: any;
  const sagaId = "550e8400-e29b-41d4-a716-446655440000";

  beforeEach(() => {
    service = makeService();
    TestBed.configureTestingModule({
      imports: [SagaStatusPage],
      providers: [
        { provide: EventService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => sagaId } } } },
      ],
    });
  });

  it("carregando exibe mensagem", () => {
    service.getSaga = () => makeResource(null, null, true);
    const fixture = TestBed.createComponent(SagaStatusPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Carregando estado da saga");
  });

  it("erro exibe mensagem e permite tentar novamente", () => {
    service.getSaga = () => makeResource(null, { status: 500 }, false);
    const fixture = TestBed.createComponent(SagaStatusPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Não foi possível carregar o estado da saga");
  });

  it("saga não encontrada exibe estado vazio", () => {
    service.getSaga = () => makeResource(null, { status: 404 }, false);
    const fixture = TestBed.createComponent(SagaStatusPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Saga não encontrada");
  });

  it("SUCCEEDED exibe steps e não polla", () => {
    const saga: SagaState = {
      sagaId,
      status: "SUCCEEDED",
      currentStep: "gravar-auditoria-qualidade",
      steps: [
        { step: "registrar-evento", status: "SUCCEEDED", attempts: 1, error: null },
        { step: "gravar-auditoria-qualidade", status: "SUCCEEDED", attempts: 1, error: null },
      ],
      error: null,
      createdAt: "2026-11-05T13:00:00Z",
      updatedAt: "2026-11-05T13:00:02Z",
    };
    service.getSaga = () => makeResource(saga, null, false);
    const fixture = TestBed.createComponent(SagaStatusPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Concluída");
    expect(fixture.nativeElement.textContent).toContain("registrar-evento");
    expect(fixture.nativeElement.textContent).toContain("gravar-auditoria-qualidade");
    expect(fixture.nativeElement.textContent).not.toContain("Aguardando conclusão");
  });

  it("IN_PROGRESS exibe polling", () => {
    const saga: SagaState = {
      sagaId,
      status: "IN_PROGRESS",
      currentStep: "gravar-auditoria-qualidade",
      steps: [{ step: "registrar-evento", status: "SUCCEEDED", attempts: 1, error: null }],
      error: null,
      createdAt: "2026-11-05T13:00:00Z",
      updatedAt: "2026-11-05T13:00:02Z",
    };
    service.getSaga = () => makeResource(saga, null, false);
    const fixture = TestBed.createComponent(SagaStatusPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Em execução");
    expect(fixture.nativeElement.textContent).toContain("Aguardando conclusão");
  });

  it("FAILED_COMPENSATED exibe 'Registro não concluído' e ação Tentar registro novamente (RN-07, E-05)", () => {
    const saga: SagaState = {
      sagaId,
      status: "FAILED_COMPENSATED",
      currentStep: "gravar-auditoria-qualidade",
      steps: [{ step: "registrar-evento", status: "SUCCEEDED", attempts: 1, error: null }],
      error: "Falha na auditoria com compensação executada",
      createdAt: "2026-11-05T13:00:00Z",
      updatedAt: "2026-11-05T13:00:05Z",
    };
    service.getSaga = () => makeResource(saga, null, false);
    const fixture = TestBed.createComponent(SagaStatusPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Registro não concluído");
    expect(fixture.nativeElement.textContent).toContain("compensação foi executada e auditada");
    expect(fixture.nativeElement.textContent).toContain("O evento não foi persistido");
    expect(fixture.nativeElement.textContent).toContain("Tentar registro novamente");
    expect(fixture.nativeElement.textContent).not.toContain("Aguardando conclusão");
  });

  it("sem permissão exibe estado próprio", () => {
    service.canRead = () => false;
    const fixture = TestBed.createComponent(SagaStatusPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Sem permissão para consultar sagas");
  });
});
