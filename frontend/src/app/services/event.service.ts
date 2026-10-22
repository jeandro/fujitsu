import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { EventRequest, EventResponse } from '../models/event.model';

@Injectable({
  providedIn: 'root'
})
export class EventService {
  private apiUrl = 'http://localhost:8080/api/events';
  private eventCreatedSource = new Subject<void>();

  eventCreated$ = this.eventCreatedSource.asObservable();

  constructor(private http: HttpClient) {}

  registerEvent(event: EventRequest): Observable<EventResponse> {
    return this.http.post<EventResponse>(this.apiUrl, event);
  }

  getEvents(): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(this.apiUrl);
  }

  notifyEventCreated(): void {
    this.eventCreatedSource.next();
  }
}
