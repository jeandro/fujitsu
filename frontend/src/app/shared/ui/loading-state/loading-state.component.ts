import { Component, input } from "@angular/core";

@Component({
  selector: "app-loading-state",
  standalone: true,
  imports: [],
  template: `
    <section class="loading-state" aria-labelledby="loading-title" role="status" aria-live="polite">
      <h2 id="loading-title">{{ title() }}</h2>
    </section>
  `,
  styles: [
    `
      .loading-state {
        text-align: center;
        padding: 40px 20px;
        color: #4b5563;
      }
      .loading-state h2 {
        font-size: 1rem;
        color: #1f2937;
        margin: 0;
      }
    `,
  ],
})
export class LoadingStateComponent {
  readonly title = input<string>("Carregando…");
}
