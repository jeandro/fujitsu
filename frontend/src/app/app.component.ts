import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { EventFormComponent } from "./components/event-form/event-form.component";
import { EventListComponent } from "./components/event-list/event-list.component";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, EventFormComponent, EventListComponent],
  template: `
    <div class="container">
      <h1>🏭 Sistema de Gestão de Eventos de Fábrica</h1>
      <app-event-form></app-event-form>
      <app-event-list></app-event-list>
    </div>
  `,
})
export class AppComponent {}
