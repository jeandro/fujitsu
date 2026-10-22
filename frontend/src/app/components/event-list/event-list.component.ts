import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService } from '../../services/event.service';
import { EventResponse } from '../../models/event.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './event-list.component.html'
})
export class EventListComponent implements OnInit, OnDestroy {
  events: EventResponse[] = [];
  isLoading: boolean = false;
  errorMessage: string = '';
  private subscription!: Subscription;

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.loadEvents();
    this.subscription = this.eventService.eventCreated$.subscribe(() => {
      this.loadEvents();
    });
  }

  loadEvents(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.eventService.getEvents().subscribe({
      next: (data) => {
        this.events = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Não foi possível carregar a lista de eventos. Verifique se o servidor backend está online.';
        this.isLoading = false;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
