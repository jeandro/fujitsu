import { Component, OnInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { finalize, Subscription } from "rxjs";
import { EventService } from "../../services/event.service";
import { FactoryEvent } from "../../models/event.model";
import { apiErrorMessage } from "../../services/api-error";
@Component({
  selector: "app-event-list",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./event-list.component.html",
})
export class EventListComponent implements OnInit, OnDestroy {
  private subscription?: Subscription;
  events: FactoryEvent[] = [];
  loading = false;
  errorMessage: string | null = null;
  private refreshPending = false;
  constructor(private eventService: EventService) {}
  ngOnInit(): void {
    this.loadEvents();
    this.subscription = this.eventService.eventCreated$.subscribe(() =>
      this.loadEvents(),
    );
  }
  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }
  loadEvents(): void {
    if (this.loading) {
      this.refreshPending = true;
      return;
    }
    this.loading = true;
    this.errorMessage = null;
    this.eventService
      .getEvents()
      .pipe(
        finalize(() => {
          this.loading = false;
          if (this.refreshPending) {
            this.refreshPending = false;
            this.loadEvents();
          }
        }),
      )
      .subscribe({
        next: (data) => (this.events = data),
        error: (err) => (this.errorMessage = apiErrorMessage(err)),
      });
  }
  trackEvent(_index: number, event: FactoryEvent): string {
    return event.eventId;
  }
}
