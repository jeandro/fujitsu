import { Component, inject, computed } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { DatePipe, NgOptimizedImage } from "@angular/common";
import { EventService } from "../event.service";
import { MaskConfidentialPipe } from "../../../shared/pipes/mask.pipe";
import { EmptyStateComponent } from "../../../shared/ui/empty-state/empty-state.component";
import { LoadingStateComponent } from "../../../shared/ui/loading-state/loading-state.component";
import { ErrorStateComponent } from "../../../shared/ui/error-state/error-state.component";
import { PermissionStateComponent } from "../../../shared/ui/permission-state/permission-state.component";
import { components } from "../../../core/api/schema";

type EventResponse = components["schemas"]["EventResponse"];

@Component({
  selector: "app-event-detail-page",
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
          title="Sem permissão para consultar este evento"
          message="Apenas papéis operador ou gerente podem consultar eventos (RN-14)."
        ></app-permission-state>
      } @else if (loading()) {
        <app-loading-state title="Carregando evento…" />
      } @else if (error()) {
        <app-error-state
          title="Erro ao carregar evento"
          [message]="errorMessage()"
          retryLabel="Tentar novamente"
          (retry)="reload()"
        ></app-error-state>
      } @else if (!event()) {
        <app-empty-state
          title="Evento não encontrado"
          message="Não existe evento com este ID."
          actionLabel="Voltar à lista"
          (action)="goBack()"
        ></app-empty-state>
      } @else {
        <section aria-labelledby="detail-title">
          <h2 id="detail-title">Detalhe do Evento</h2>
          <button
            type="button"
            (click)="goBack()"
            class="btn-back"
            aria-label="Voltar à lista de eventos"
          >
            ← Voltar à lista
          </button>
          <table class="detail-table">
            <caption>{{ event().eventId }}</caption>
            <tbody>
              <tr>
                <th scope="row">Tipo do evento</th>
                <td>
                  <span class="badge" [class]="'badge-' + event().eventType.toLowerCase()">
                    {{ event().eventType }}
                  </span>
                </td>
              </tr>
              <tr>
                <th scope="row">Descrição</th>
                <td class="description-cell">{{ event().description | maskConfidential }}</td>
              </tr>
              <tr>
                <th scope="row">Data / Hora</th>
                <td><time [attr.datetime]="event().timestamp">{{ event().timestamp | date: "dd/MM/yyyy HH:mm:ss" }}</time></td>
              </tr>
            </tbody>
          </table>
          <p class="hint">
            A descrição é confidencial (LGPD, RN-17): mascarada para papéis
            sem permissão; leitura direta, sem atraso de cache (RN-21).
          </p>
        </section>
      }
    </div>
  `,
  styles: [
    `
      .container { max-width: 600px; margin: 0 auto; padding: 20px; }
      .btn-back { background-color: #6c757d; color: #fff; border: none; padding: 8px 16px; border-radius: 4px; font-size: 14px; cursor: pointer; margin-bottom: 16px; display: inline-block; }
      .btn-back:focus-visible { outline: 3px solid #145c9c; outline-offset: 3px; }
      .detail-table { width: 100%; border-collapse: collapse; margin-top: 12px; }
      .detail-table th, .detail-table td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; vertical-align: top; }
      .detail-table th { background-color: #f8f9fa; color: #2c3e50; width: 40%; }
      .detail-table caption { text-align: left; font-weight: bold; font-size: 1.25rem; margin-bottom: 8px; color: #1f2937; }
      .description-cell { white-space: pre-wrap; max-width: 320px; overflow-wrap: anywhere; }
      .badge { padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: bold; }
      .badge-production { background-color: #e3f2fd; color: #0d47a1; }
      .badge-maintenance { background-color: #e8eaf6; color: #283593; }
      .badge-quality { background-color: #fff3e0; color: #9c3d00; }
      .hint { color: #4b5563; font-size: 0.9rem; margin-top: 12px; }
    `,
  ],
})
export class EventDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(EventService);

  readonly canRead = computed(() => this.service.canRead());

  private readonly eventId = this.route.snapshot.paramMap.get("eventId") ?? "";
  private readonly eventResource = this.service.getEvent(this.eventId);

  readonly loading = this.eventResource.isLoading;
  readonly error = this.eventResource.error;
  readonly event = computed(() => this.eventResource.value());
  readonly reload = () => this.eventResource.reload();

  readonly errorMessage = computed(() => {
    const err = this.error();
    if (!err) return "";
    if (err.status === 404) return "Evento não encontrado.";
    if (err.status === 401) return "Autenticação necessária.";
    if (err.status === 403) return "Sem permissão para consultar este evento.";
    return "Não foi possível carregar o evento.";
  });

  goBack(): void {
    history.back();
  }
}
