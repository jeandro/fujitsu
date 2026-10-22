import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EventService } from '../../services/event.service';
import { EventType, EventRequest } from '../../models/event.model';

@Component({
  selector: 'app-event-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './event-form.component.html'
})
export class EventFormComponent {
  eventId: string = '';
  eventType: EventType = EventType.PRODUCTION;
  description: string = '';

  eventTypes = [
    { value: EventType.PRODUCTION, label: 'PRODUCTION (Produção Normal)' },
    { value: EventType.MAINTENANCE, label: 'MAINTENANCE (Manutenção de Equipamento)' },
    { value: EventType.QUALITY, label: 'QUALITY (Qualidade - Gera Auditoria)' }
  ];

  isSubmitting: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  constructor(private eventService: EventService) {}

  onSubmit(): void {
    if (!this.eventId.trim() || !this.description.trim()) {
      this.errorMessage = 'Por favor, preencha todos os campos obrigatórios.';
      return;
    }

    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';

    const payload: EventRequest = {
      eventId: this.eventId.trim(),
      eventType: this.eventType,
      description: this.description.trim(),
      timestamp: new Date().toISOString()
    };

    const submittedId = payload.eventId;

    this.eventService.registerEvent(payload).subscribe({
      next: () => {
        this.successMessage = `Evento '${submittedId}' registrado com sucesso!`;
        this.eventId = '';
        this.description = '';
        this.eventType = EventType.PRODUCTION;
        this.isSubmitting = false;
        this.eventService.notifyEventCreated();
      },
      error: (err) => {
        this.isSubmitting = false;
        if (err.status === 409) {
          this.errorMessage = `Conflito (409): O evento '${submittedId}' já foi registrado previamente.`;
        } else if (err.status === 400) {
          this.errorMessage = 'Dados inválidos (400). Verifique os limites de tamanho e campos obrigatórios.';
        } else {
          this.errorMessage = 'Erro ao conectar com o servidor. Verifique se o backend está em execução na porta 8080.';
        }
      }
    });
  }
}
