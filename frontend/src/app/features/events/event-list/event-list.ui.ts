import { Component, input, output, computed } from "@angular/core";
import { RouterLink } from "@angular/router";
import { DatePipe, NgOptimizedImage } from "@angular/common";
import { MaskConfidentialPipe } from "../../../shared/pipes/mask.pipe";
import { EmptyStateComponent } from "../../../shared/ui/empty-state/empty-state.component";
import { LoadingStateComponent } from "../../../shared/ui/loading-state/loading-state.component";
import { ErrorStateComponent } from "../../../shared/ui/error-state/error-state.component";
import { PermissionStateComponent } from "../../../shared/ui/permission-state/permission-state.component";
import { components } from "../../../core/api/schema";
import { ProblemSafe } from "../../../core/api/problem";

type EventResponse = components["schemas"]["EventResponse"];

@Component({
  selector: "app-event-list-ui",
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    NgOptimizedImage,
    MaskConfidentialPipe,
    EmptyStateComponent,
    LoadingStateComponent,
    ErrorStateComponent,
    PermissionStateComponent,
  ],
  template: `
    <div class="container">
      @if (!canRead()) {
        <app-permission-state
          title="Sem permissão para consultar eventos"
          message="Você não tem permissão para consultar eventos. Seu papel não tem acesso a esta tela. Solicite acesso ao administrador."
          actionLabel="Voltar"
          (action)="onRegister.emit()"
        ></app-permission-state>
      } @else if (loading()) {
        <app-loading-state title="Carregando eventos…" />
      } @else if (error()) {
        <app-error-state
          title="Não foi possível carregar os eventos."
          [message]="errorMessage()"
          detail="Tente novamente ou informe o identificador da requisição ao suporte."
          retryLabel="Tentar novamente"
          (retry)="refresh.emit()"
        ></app-error-state>
      } @else if (events().length === 0) {
        <app-empty-state
          title="Nenhum evento encontrado."
          message="Ainda não há eventos registrados. Registre o primeiro evento."
          actionLabel="Novo evento"
          (action)="onRegister.emit()"
        ></app-empty-state>
      } @else {
        <section aria-labelledby="list-title">
          <div class="section-heading">
            <h2 id="list-title">Eventos de fábrica</h2>
            <p class="summary">Mostrando {{ showingFrom() }} de {{ totalElements() }} eventos.</p>
            <button
              type="button"
              (click)="refresh.emit()"
              [disabled]="loading()"
              class="btn-refresh"
            >
              {{ loading() ? "Atualizando…" : "Atualizar eventos" }}
            </button>
            <a routerLink="../novo" class="btn-primary">Novo evento</a>
          </div>
          <div class="pagination">
            <span>Página {{ page() + 1 }} de {{ totalPages() }}</span>
            <span>{{ totalElements() }} evento(s) no total</span>
            <select
              [value]="size()"
              (change)="sizeChange.emit($any($event.target).value)"
              class="page-size-select"
            >
              <option [value]="20">20 por página</option>
              <option [value]="50">50 por página</option>
              <option [value]="100">100 por página</option>
            </select>
            <button
              type="button"
              (click)="pageChange.emit(page() - 1)"
              [disabled]="page() === 0"
              class="btn-page"
            >
              Anterior
            </button>
            <button
              type="button"
              (click)="pageChange.emit(page() + 1)"
              [disabled]="page() >= totalPages() - 1"
              class="btn-page"
            >
              Próxima
            </button>
          </div>
          <div class="table-scroll" role="region" aria-label="Tabela de eventos">
            <table>
              <caption>Eventos registrados, mais recentes primeiro</caption>
              <thead>
                <tr>
                  <th scope="col">ID Evento</th>
                  <th scope="col">Tipo</th>
                  <th scope="col">Descrição</th>
                  <th scope="col">Data / Hora</th>
                  <th scope="col">Detalhe</th>
                </tr>
              </thead>
              <tbody>
                @for (evt of events(); track evt.eventId) {
                  <tr>
                    <td><strong>{{ evt.eventId }}</strong></td>
                    <td>
                      <span class="badge" [class]="'badge-' + evt.eventType.toLowerCase()">
                        {{ evt.eventType }}
                      </span>
                    </td>
                    <td class="description-cell">
                      {{ evt.description | maskConfidential }}
                    </td>
                    <td>
                      <time [attr.datetime]="evt.timestamp">{{
                        evt.timestamp | date: "dd/MM/yyyy HH:mm:ss"
                      }}</time>
                    </td>
                    <td>
                      <a
                        [routerLink]="['../', evt.eventId]"
                        class="btn-detail"
                        >Ver detalhe</a
                      >
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    </div>
  `,
  styles: [
    `
      .container { max-width: 1000px; margin: 0 auto; padding: 20px; }
      .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; align-items: baseline; }
      .summary { color: #4b5563; font-size: 0.9rem; margin: 0; }
      .btn-primary { background-color: #17689b; color: #fff; border: none; padding: 8px 16px; border-radius: 4px; font-size: 14px; cursor: pointer; text-decoration: none; display: inline-block; }
      .btn-primary:focus-visible { outline: 3px solid #145c9c; outline-offset: 3px; }
      .btn-refresh { background-color: #6c757d; color: #fff; border: none; padding: 8px 16px; border-radius: 4px; font-size: 14px; cursor: pointer; }
      .btn-refresh:disabled { background-color: #bdc3c7; cursor: not-allowed; }
      .btn-refresh:focus-visible { outline: 3px solid #145c9c; outline-offset: 3px; }
      .pagination { display: flex; align-items: center; gap: 8px; margin: 12px 0; flex-wrap: wrap; }
      .pagination select { padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
      .btn-page { background-color: #17689b; color: #fff; border: none; padding: 6px 12px; border-radius: 4px; font-size: 14px; cursor: pointer; }
      .btn-page:disabled { background-color: #bdc3c7; cursor: not-allowed; }
      .btn-detail { background-color: #17689b; color: #fff; border: none; padding: 6px 12px; border-radius: 4px; font-size: 13px; cursor: pointer; text-decoration: none; display: inline-block; }
      .badge { padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: bold; }
      .badge-production { background-color: #e3f2fd; color: #0d47a1; }
      .badge-maintenance { background-color: #e8eaf6; color: #283593; }
      .badge-quality { background-color: #fff3e0; color: #9c3d00; }
      table { width: 100%; border-collapse: collapse; margin-top: 12px; min-width: 620px; }
      th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
      th { background-color: #f8f9fa; color: #2c3e50; }
      .description-cell { white-space: pre-wrap; min-width: 160px; max-width: 320px; overflow-wrap: anywhere; }
      time { white-space: nowrap; }
    `,
  ],
})
export class EventListUI {
  readonly canRead = input.required<boolean>();
  readonly events = input.required<EventResponse[]>();
  readonly loading = input.required<boolean>();
  readonly error = input.required<ProblemSafe | null>();
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly totalPages = input.required<number>();

  readonly pageChange = output<number>();
  readonly sizeChange = output<number>();
  readonly refresh = output<void>();
  readonly onRegister = output<void>();

  readonly showingFrom = computed(() => {
    const p = this.page();
    const s = this.size();
    const total = this.totalElements();
    if (total === 0) return 0;
    return Math.min(p * s + 1, total);
  });

  readonly errorMessage = computed(() => {
    const err = this.error();
    return err ? (err.detail ?? "Não foi possível carregar os eventos. Tente novamente.") : "";
  });
}
