import { Component, inject, signal, computed } from "@angular/core";
import {
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { Router } from "@angular/router";
import { HttpErrorResponse } from "@angular/common/http";
import { EventService } from "../event.service";
import { MaskConfidentialPipe } from "../../../shared/pipes/mask.pipe";
import { EmptyStateComponent } from "../../../shared/ui/empty-state/empty-state.component";
import { LoadingStateComponent } from "../../../shared/ui/loading-state/loading-state.component";
import { ErrorStateComponent } from "../../../shared/ui/error-state/error-state.component";
import { PermissionStateComponent } from "../../../shared/ui/permission-state/permission-state.component";
import {
  problemMessage,
  isIdempotencyKeyReused,
  isDuplicateEvent,
} from "../../../core/api/problem";

type EventType = "PRODUCTION" | "MAINTENANCE" | "QUALITY";

interface EventFormValue {
  eventId: string;
  eventType: EventType;
  description: string;
  timestamp: string;
}

@Component({
  selector: "app-event-form-page",
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MaskConfidentialPipe,
    EmptyStateComponent,
    LoadingStateComponent,
    ErrorStateComponent,
    PermissionStateComponent,
  ],
  template: `
    <div class="container">
      @if (!canRegister()) {
        <app-permission-state
          title="Sem permissão para registrar eventos"
          message="Apenas o papel operador pode registrar eventos (RN-13)."
        ></app-permission-state>
      } @else {
        <section aria-labelledby="form-title">
          <h2 id="form-title">Cadastrar Novo Evento</h2>

          @if (successMessage()) {
            <div class="alert alert-success" role="status">
              {{ successMessage() }}
              <button
                type="button"
                (click)="clearMessages()"
                class="btn-close"
                aria-label="Fechar mensagem de sucesso"
              >
                ✕
              </button>
            </div>
          }

          @if (errorMessage()) {
            <div class="alert alert-danger" role="alert">
              {{ errorMessage() }}
              <button
                type="button"
                (click)="clearMessages()"
                class="btn-close"
                aria-label="Fechar mensagem de erro"
              >
                ✕
              </button>
            </div>
          }

          <form [formGroup]="form" (ngSubmit)="onSubmit()" [attr.aria-busy]="submitting()">
            <fieldset [disabled]="submitting()">
              <div class="form-group">
                <label for="eventId">ID do Evento (Ex: EVT-1001):</label>
                <input
                  id="eventId"
                  formControlName="eventId"
                  required
                  maxlength="100"
                  aria-describedby="id-help id-error"
                  [attr.aria-invalid]="eventId.invalid && (eventId.touched || eventId.dirty) ? 'true' : null"
                  placeholder="Informe o ID único do evento"
                />
                <small id="id-help">Até 100 caracteres. Espaços nas extremidades serão removidos.</small>
                @if (eventId.invalid && (eventId.touched || eventId.dirty)) {
                  <p id="id-error" class="field-error">
                    {{ eventIdErrors() }}
                  </p>
                }
              </div>

              <div class="form-group">
                <label for="eventType">Tipo do Evento:</label>
                <select
                  id="eventType"
                  formControlName="eventType"
                  required
                  aria-describedby="type-error"
                  [attr.aria-invalid]="eventType.invalid && (eventType.touched || eventType.dirty) ? 'true' : null"
                >
                  <option value="PRODUCTION">PRODUCTION (Produção Normal)</option>
                  <option value="MAINTENANCE">MAINTENANCE (Manutenção)</option>
                  <option value="QUALITY">QUALITY (Exige Auditoria)</option>
                </select>
                @if (eventType.invalid && (eventType.touched || eventType.dirty)) {
                  <p id="type-error" class="field-error">{{ eventTypeErrors() }}</p>
                }
              </div>

              <div class="form-group">
                <label for="description">Descrição:</label>
                <textarea
                  id="description"
                  formControlName="description"
                  required
                  maxlength="1000"
                  rows="3"
                  aria-describedby="description-help description-error"
                  [attr.aria-invalid]="description.invalid && (description.touched || description.dirty) ? 'true' : null"
                  placeholder="Descreva o evento ocorrido na linha de fábrica"
                ></textarea>
                <small id="description-help">
                  {{ description.value.length }} / 1000 caracteres
                </small>
                @if (description.invalid && (description.touched || description.dirty)) {
                  <p id="description-error" class="field-error">
                    {{ descriptionErrors() }}
                  </p>
                }
              </div>

              <div class="form-group">
                <label for="timestamp">Data / Hora (ISO 8601):</label>
                <input
                  id="timestamp"
                  type="datetime-local"
                  formControlName="timestamp"
                  required
                  aria-describedby="timestamp-help"
                  [attr.aria-invalid]="timestamp.invalid && (timestamp.touched || timestamp.dirty) ? 'true' : null"
                />
                <small id="timestamp-help">Instante ISO 8601 válido (RN-01).</small>
              </div>

              <button
                type="submit"
                [disabled]="submitting() || form.invalid"
                class="btn-submit"
              >
                {{ submitting() ? "Registrando…" : "Registrar Evento" }}
              </button>
              <button
                type="button"
                (click)="goBack()"
                [disabled]="submitting()"
                class="btn-cancel"
              >
                Voltar à lista
              </button>
            </fieldset>
          </form>
        </section>
      }
    </div>
  `,
  styles: [
    `
      .container { max-width: 600px; margin: 0 auto; padding: 20px; }
      .alert { padding: 12px 20px; margin-bottom: 16px; border-radius: 4px; font-weight: 600; }
      .alert-success { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
      .alert-danger { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
      .btn-close { background: transparent; border: none; font-size: 1.2rem; cursor: pointer; float: right; }
      .form-group { margin-bottom: 16px; }
      label { display: block; margin-bottom: 6px; font-weight: bold; }
      input, select, textarea { width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
      input:invalid[aria-invalid="true"], textarea:invalid[aria-invalid="true"], select:invalid[aria-invalid="true"] { border-color: #9c1c28; }
      .field-error { color: #9c1c28; font-size: 0.9rem; margin-top: 4px; }
      .btn-submit { background-color: #17689b; color: #fff; border: none; padding: 10px 24px; border-radius: 4px; font-size: 16px; font-weight: bold; cursor: pointer; }
      .btn-submit:disabled { background-color: #bdc3c7; cursor: not-allowed; }
      .btn-submit:focus-visible { outline: 3px solid #145c9c; outline-offset: 3px; }
      .btn-cancel { background-color: #6c757d; color: #fff; border: none; padding: 10px 24px; border-radius: 4px; font-size: 16px; cursor: pointer; margin-left: 8px; }
      .btn-cancel:focus-visible { outline: 3px solid #145c9c; outline-offset: 3px; }
      small { color: #4b5563; display: block; margin-top: 4px; }
    `,
  ],
})
export class EventFormPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly service = inject(EventService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly canRegister = computed(() => this.service.canRegister());

  readonly form = this.fb.group({
    eventId: this.fb.control<string>("", [
      Validators.required,
      Validators.minLength(1),
      Validators.maxLength(100),
      Validators.pattern(/.*\S.*/),
    ]),
    eventType: this.fb.control<EventType>("PRODUCTION", [Validators.required]),
    description: this.fb.control<string>("", [
      Validators.required,
      Validators.minLength(1),
      Validators.maxLength(1000),
      Validators.pattern(/[\s\S]*\S[\s\S]*/),
    ]),
    timestamp: this.fb.control<string>("", [Validators.required]),
  });

  get eventId() {
    return this.form.get("eventId") as FormControl<string>;
  }
  get eventType() {
    return this.form.get("eventType") as FormControl<EventType>;
  }
  get description() {
    return this.form.get("description") as FormControl<string>;
  }
  get timestamp() {
    return this.form.get("timestamp") as FormControl<string>;
  }

  readonly eventIdErrors = computed(() => {
    const c = this.eventId;
    if ((c.touched || c.dirty) && c.errors) {
      if (c.errors["required"]) return "Informe o ID do evento.";
      if (c.errors["maxlength"]) return "O ID deve ter até 100 caracteres.";
      if (c.errors["pattern"]) return "O ID não pode conter apenas espaços.";
    }
    return "";
  });

  readonly eventTypeErrors = computed(() => {
    const c = this.eventType;
    if ((c.touched || c.dirty) && c.errors) {
      if (c.errors["required"]) return "Selecione um tipo de evento.";
    }
    return "";
  });

  readonly descriptionErrors = computed(() => {
    const c = this.description;
    if ((c.touched || c.dirty) && c.errors) {
      if (c.errors["required"]) return "Informe a descrição.";
      if (c.errors["maxlength"]) return "A descrição deve ter até 1000 caracteres.";
      if (c.errors["pattern"]) return "A descrição não pode conter apenas espaços.";
    }
    return "";
  });

  clearMessages(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
  }

  goBack(): void {
    this.router.navigate(["../"]);
  }

  async onSubmit(): Promise<void> {
    if (this.submitting() || this.form.invalid) return;

    this.clearMessages();
    this.submitting.set(true);

    const value = this.form.getRawValue() as EventFormValue;
    const timestamp = value.timestamp
      ? new Date(value.timestamp).toISOString()
      : new Date().toISOString();

    try {
      const response = await this.service.registerEvent(
        value.eventId,
        value.eventType,
        value.description,
        timestamp,
      );
      this.successMessage.set(`Evento ${response.eventId} registrado com sucesso!`);
      this.form.reset({
        eventId: "",
        eventType: "PRODUCTION",
        description: "",
        timestamp: "",
      } as { eventId: string; eventType: EventType; description: string; timestamp: string });
    } catch (error: unknown) {
      if (error instanceof HttpErrorResponse) {
        if (isIdempotencyKeyReused(error)) {
          this.errorMessage.set(
            "Esta ação já foi processada com dados diferentes. Verifique os dados e tente com uma nova intenção.",
          );
        } else if (isDuplicateEvent(error)) {
          this.errorMessage.set("Este ID de evento já foi processado anteriormente (RN-02).");
        } else {
          this.errorMessage.set(
            problemMessage(error).detail ?? "Não foi possível registrar o evento. Tente novamente.",
          );
        }
      } else if (error instanceof Error) {
        this.errorMessage.set(error.message);
      } else {
        this.errorMessage.set("Não foi possível registrar o evento. Tente novamente.");
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
