import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule, NgForm } from "@angular/forms";
import { finalize } from "rxjs";
import { EventService } from "../../services/event.service";
import { EventType, FactoryEvent } from "../../models/event.model";
import { apiErrorMessage } from "../../services/api-error";

@Component({
  selector: "app-event-form",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./event-form.component.html",
})
export class EventFormComponent {
  eventId = "";
  eventType = EventType.PRODUCTION;
  description = "";
  successMessage: string | null = null;
  errorMessage: string | null = null;
  fieldErrors: Record<string, string> = {};
  submitting = false;
  constructor(private eventService: EventService) {}
  get valid(): boolean {
    return (
      this.eventId.trim().length > 0 &&
      this.eventId.trim().length <= 100 &&
      this.description.trim().length > 0 &&
      this.description.length <= 1000 &&
      Object.values(EventType).includes(this.eventType)
    );
  }
  onSubmit(form?: NgForm): void {
    if (this.submitting || !this.valid) return;
    this.successMessage = null;
    this.errorMessage = null;
    this.fieldErrors = {};
    const event: FactoryEvent = {
      eventId: this.eventId.trim(),
      eventType: this.eventType,
      description: this.description,
      timestamp: new Date().toISOString(),
    };
    this.submitting = true;
    this.eventService
      .registerEvent(event)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (response) => {
          this.successMessage = `Evento ${response.eventId} registrado com sucesso!`;
          this.eventId = "";
          this.description = "";
          this.eventType = EventType.PRODUCTION;
          form?.resetForm({
            eventId: "",
            description: "",
            eventType: EventType.PRODUCTION,
          });
          this.eventService.notifyEventCreated();
        },
        error: (err) => {
          this.errorMessage = apiErrorMessage(err);
          this.fieldErrors = err.status === 400 ? err.error?.errors || {} : {};
        },
      });
  }
}
