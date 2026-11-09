import { Injectable, inject, signal, computed, effect } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { components } from "../../core/api/schema";
import { IdempotencyStore } from "../../core/idempotency";
import { AuthService } from "../../core/auth/auth.service";

type EventResponse = components["schemas"]["EventResponse"];
type EventResponsePage = components["schemas"]["EventResponsePage"];
type RegisterEventRequest = components["schemas"]["RegisterEventRequest"];
type SagaState = components["schemas"]["SagaState"];

const API_EVENTS = "/api/v1/events";

@Injectable({ providedIn: "root" })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly idempotency = inject(IdempotencyStore);
  private readonly auth = inject(AuthService);

  private readonly pageSignal = signal(0);
  private readonly sizeSignal = signal(20);

  readonly page = this.pageSignal.asReadonly();
  readonly size = this.sizeSignal.asReadonly();

  private readonly eventsData = signal<EventResponsePage | undefined>(undefined);
  private readonly eventsError = signal<any | null>(null);
  private readonly eventsLoading = signal(false);

  readonly eventsValue = computed(() => this.eventsData()?.content ?? []);
  readonly loading = this.eventsLoading.asReadonly();
  readonly error = this.eventsError.asReadonly();
  readonly totalElements = computed(() => this.eventsData()?.totalElements ?? 0);
  readonly totalPages = computed(() => this.eventsData()?.totalPages ?? 0);

  constructor() {
    effect(
      () => {
        const p = this.pageSignal();
        const s = this.sizeSignal();
        this.eventsLoading.set(true);
        this.eventsError.set(null);
        const url = `${API_EVENTS}?page=${p}&size=${s}&sort=timestamp,desc`;
        this.http.get<EventResponsePage>(url).subscribe({
          next: (d) => {
            this.eventsData.set(d);
            this.eventsLoading.set(false);
          },
          error: (err) => {
            this.eventsError.set(err);
            this.eventsLoading.set(false);
          },
        });
      },
      { allowSignalWrites: true },
    );
  }

  setPage(page: number): void {
    this.pageSignal.set(page);
  }

  setSize(size: number): void {
    this.sizeSignal.set(size);
  }

  refresh(): void {
    this.pageSignal.set(0);
    this.sizeSignal.set(20);
  }

  getEvent(eventId: string) {
    return this.http.get<EventResponse>(`${API_EVENTS}/${encodeURIComponent(eventId)}`);
  }

  getSaga(sagaId: string) {
    return this.http.get<SagaState>(`/api/v1/sagas/${encodeURIComponent(sagaId)}`);
  }

  async registerEvent(
    eventId: string,
    eventType: "PRODUCTION" | "MAINTENANCE" | "QUALITY",
    description: string,
    timestamp: string,
  ): Promise<EventResponse> {
    if (!this.auth.canRegister()) {
      throw new Error("Somente o papel operador pode registrar eventos.");
    }

    const key = this.idempotency.newKey();
    const body: RegisterEventRequest = {
      eventId: eventId.trim(),
      eventType,
      description,
      timestamp,
    };

    return this.idempotency.postOnce(API_EVENTS, body, key);
  }

  canRead(): boolean {
    return this.auth.canRead();
  }

  canRegister(): boolean {
    return this.auth.canRegister();
  }
}
