import { Component, computed, inject } from "@angular/core";
import { Router } from "@angular/router";
import { EventListUI } from "./event-list.ui";
import { EventService } from "../event.service";

/**
 * Página de consulta paginada de eventos (RN-09, RN-21).
 * Smart component: orquestra o serviço, paginação e estados.
 * Nenhuma lógica de negócio; regras vêm do serviço/contrato.
 */
@Component({
  selector: "app-event-list-page",
  standalone: true,
  imports: [EventListUI],
  template: `
    <app-event-list-ui
      [canRead]="canRead()"
      [events]="events()"
      [loading]="loading()"
      [error]="error()"
      [page]="page()"
      [size]="size()"
      [totalElements]="totalElements()"
      [totalPages]="totalPages()"
      (pageChange)="setPage($event)"
      (sizeChange)="setSize($event)"
      (refresh)="refresh()"
      (onRegister)="goToRegister()"
    ></app-event-list-ui>
  `,
})
export class EventListPage {
  private readonly service = inject(EventService);
  private readonly router = inject(Router);

  readonly canRead = computed(() => this.service.canRead());
  readonly events = this.service.eventsValue;
  readonly loading = this.service.loading;
  readonly error = this.service.error;
  readonly page = this.service.page;
  readonly size = this.service.size;
  readonly totalElements = this.service.totalElements;
  readonly totalPages = this.service.totalPages;

  /** Atualiza a página consumida pela consulta. */
  setPage(p: number): void {
    this.service.setPage(p);
  }

  /** Atualiza o tamanho da página (máx. 100 no serviço). */
  setSize(s: number): void {
    this.service.setSize(s);
  }

  /** Volta para a primeira página e tamanho padrão (reload explícito). */
  refresh(): void {
    this.service.refresh();
  }

  goToRegister(): void {
    this.router.navigate(["../novo"]);
  }
}
