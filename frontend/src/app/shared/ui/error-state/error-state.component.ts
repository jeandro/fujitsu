import { Component, input, output } from "@angular/core";

@Component({
  selector: "app-error-state",
  standalone: true,
  imports: [],
  template: `
    <section class="error-state" role="alert" aria-live="polite">
      <h2>{{ title() }}</h2>
      <p>{{ message() }}</p>
      @if (retryLabel()) {
        <button type="button" (click)="retry.emit()" class="btn-retry">
          {{ retryLabel() }}
        </button>
      }
      @if (detail()) {
        <p class="detail">{{ detail() }}</p>
      }
    </section>
  `,
  styles: [
    `
      .error-state {
        text-align: center;
        padding: 32px 20px;
        color: #721c24;
        background: #fdecea;
        border: 1px solid #f5c6cb;
        border-radius: 4px;
        max-width: 560px;
        margin: 0 auto;
      }
      .error-state h2 {
        font-size: 1.1rem;
        margin-top: 0;
      }
      .error-state p {
        margin: 12px 0;
      }
      .error-state .detail {
        font-size: 0.9rem;
        color: #4b5563;
        word-break: break-word;
      }
      .btn-retry {
        background-color: #17689b;
        color: #ffffff;
        border: none;
        padding: 8px 16px;
        border-radius: 4px;
        font-size: 14px;
        cursor: pointer;
        margin-top: 12px;
      }
      .btn-retry:focus-visible {
        outline: 3px solid #145c9c;
        outline-offset: 3px;
      }
    `,
  ],
})
export class ErrorStateComponent {
  readonly title = input<string>("Erro");
  readonly message = input<string>("Não foi possível concluir a operação.");
  readonly retryLabel = input<string | null>("Tentar novamente");
  readonly retry = output<void>();
  readonly detail = input<string | null>(null);
}
