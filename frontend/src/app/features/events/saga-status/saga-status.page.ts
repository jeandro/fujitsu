import { Component, inject, computed, effect, onCleanup } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { DatePipe } from "@angular/common";
import { EventService } from "../event.service";
import { EmptyStateComponent } from "../../../shared/ui/empty-state/empty-state.component";
import { LoadingStateComponent } from "../../../shared/ui/loading-state/loading-state.component";
import { ErrorStateComponent } from "../../../shared/ui/error-state/error-state.component";
import { PermissionStateComponent } from "../../../shared/ui/permission-state/permission-state.component";
import { components } from "../../../core/api/schema";

type SagaStatus = components["schemas"]["SagaState"]["status"];

@Component({
  selector: "app-saga-status-page",
  standalone: true,
  imports: [
    DatePipe,
    EmptyStateComponent,
    LoadingStateComponent,
    ErrorStateComponent,
    PermissionStateComponent,
  ],
  template: `
    <div class="container">
      @if (!canRead()) {
        <app-permission-state
          title="Sem permissão para consultar sagas"
          message="Apenas papéis operador ou gerente podem consultar o estado de sagas (RN-06, RN-07)."
        ></app-permission-state>
      } @else if (loading()) {
        <app-loading-state title="Carregando estado da saga…" />
      } @else if (error()) {
        <app-error-state
          title="Erro ao consultar saga"
          [message]="errorMessage()"
          retryLabel="Tentar novamente"
          (retry)="reload()"
        ></app-error-state>
      } @else if (!saga()) {
        <app-empty-state
          title="Saga não encontrada"
          message="Não existe saga com este identificador."
          actionLabel="Voltar à lista"
          (action)="goBack()"
        ></app-empty-state>
      } @else {
        <section aria-labelledby="saga-title">
          <h2 id="saga-title">Estado da Saga</h2>
          <button type="button" (click)="goBack()" class="btn-back">← Voltar à lista</button>

          <div class="saga-header">
            <span class="badge status" [class]="statusClass(status())">
              {{ statusLabel(status()) }}
            </span>
            <span class="saga-id">{{ sagaId }}</span>
          </div>

          <p><strong>Passo atual:</strong> {{ currentStep() ?? "—" }}</p>
          <p>
            <strong>Criado em:</strong>
            <time>{{ createdAt() | date: "dd/MM/yyyy HH:mm:ss" }}</time>
          </p>
          <p>
            <strong>Atualizado em:</strong>
            <time>{{ updatedAt() | date: "dd/MM/yyyy HH:mm:ss" }}</time>
          </p>

          @if (steps().length > 0) {
            <h3>Passos da execução</h3>
            <table class="steps-table">
              <thead>
                <tr>
                  <th scope="col">Passo</th>
                  <th scope="col">Status</th>
                  <th scope="col">Tentativas</th>
                  <th scope="col">Erro</th>
                </tr>
              </thead>
              <tbody>
                @for (step of steps(); track step.step) {
                  <tr>
                    <td>{{ step.step }}</td>
                    <td><span class="badge" [class]="'status-' + step.status.toLowerCase()">{{ step.status }}</span></td>
                    <td>{{ step.attempts }}</td>
                    <td class="step-error">{{ step.error ?? "—" }}</td>
                  </tr>
                }
              </tbody>
            </table>
          }

          @if (sagaError()) {
            <div class="saga-error" role="alert" aria-live="polite">
              <strong>Erro da saga:</strong> {{ sagaError() }}
            </div>
          }

          @if (isPolling()) {
            <p class="polling" role="status" aria-live="polite">Aguardando conclusão…</p>
          }
        </section>
      }
    </div>
  `,
  styles: [
    `
      .container { max-width: 700px; margin: 0 auto; padding: 20px; }
      .btn-back { background-color: #6c757d; color: #fff; border: none; padding: 8px 16px; border-radius: 4px; font-size: 14px; cursor: pointer; margin-bottom: 16px; display: inline-block; }
      .btn-back:focus-visible { outline: 3px solid #145c9c; outline-offset: 3px; }
      .saga-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
      .saga-id { font-family: monospace; font-size: 0.85rem; color: #4b5563; background: #f3f4f6; padding: 4px 8px; border-radius: 4px; }
      .badge { padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: bold; }
      .status-succeeded, .status-compensated { background-color: #d4edda; color: #155724; }
      .status-started, .status-in_progress { background-color: #fff3cd; color: #856404; }
      .status-failed, .status-failed_compensated { background-color: #f8d7da; color: #721c24; }
      .steps-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
      .steps-table th, .steps-table td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
      .steps-table th { background-color: #f8f9fa; color: #2c3e50; }
      .step-error { color: #4b5563; font-size: 0.9rem; }
      .saga-error { background: #fdecea; color: #721c24; padding: 12px; border-radius: 4px; border: 1px solid #f5c6cb; margin-top: 12px; }
      .polling { color: #17689b; font-size: 0.9rem; margin-top: 12px; }
    `,
  ],
})
export class SagaStatusPage {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(EventService);

  readonly canRead = computed(() => this.service.canRead());

  readonly sagaId = this.route.snapshot.paramMap.get("sagaId") ?? "";

  private readonly sagaResource = this.service.getSaga(this.sagaId);

  readonly loading = this.sagaResource.isLoading;
  readonly error = this.sagaResource.error;
  readonly saga = computed(() => this.sagaResource.value());
  readonly reload = () => this.sagaResource.reload();

  readonly status = computed(() => this.saga()?.status);
  readonly currentStep = computed(() => this.saga()?.currentStep);
  readonly steps = computed(() => this.saga()?.steps ?? []);
  readonly sagaError = computed(() => this.saga()?.error ?? null);
  readonly createdAt = computed(() => this.saga()?.createdAt ?? "");
  readonly updatedAt = computed(() => this.saga()?.updatedAt ?? "");

  readonly isPolling = computed(() => {
    const s = this.status();
    return s === "STARTED" || s === "IN_PROGRESS";
  });

  private intervalId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    effect(() => {
      if (this.isPolling() && !this.intervalId) {
        this.startPolling();
      }
      if (!this.isPolling() && this.intervalId) {
        this.stopPolling();
      }
    });
    onCleanup(() => this.stopPolling());
  }

  private startPolling(): void {
    this.intervalId = setInterval(() => {
      const s = this.status();
      if (s === "SUCCEEDED" || s === "FAILED" || s === "COMPENSATED" || s === "FAILED_COMPENSATED") {
        this.stopPolling();
        return;
      }
      this.sagaResource.reload();
    }, 3000);
  }

  private stopPolling(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  statusLabel(s: SagaStatus | undefined): string {
    switch (s) {
      case "SUCCEEDED": return "Concluída";
      case "COMPENSATED": return "Compensada";
      case "IN_PROGRESS": return "Em execução";
      case "STARTED": return "Iniciada";
      case "FAILED": return "Falhou";
      case "FAILED_COMPENSATED": return "Falhou com compensação";
      default: return "Desconhecido";
    }
  }

  statusClass(s: SagaStatus | undefined): string {
    switch (s) {
      case "SUCCEEDED":
      case "COMPENSATED": return "status-succeeded";
      case "STARTED":
      case "IN_PROGRESS": return "status-started";
      case "FAILED":
      case "FAILED_COMPENSATED": return "status-failed";
      default: return "";
    }
  }

  readonly errorMessage = computed(() => {
    const err = this.error();
    if (!err) return "";
    if (err.status === 404) return "Saga não encontrada.";
    if (err.status === 401) return "Autenticação necessária.";
    if (err.status === 403) return "Sem permissão para consultar esta saga.";
    return "Não foi possível carregar o estado da saga.";
  });

  goBack(): void {
    history.back();
  }
}
