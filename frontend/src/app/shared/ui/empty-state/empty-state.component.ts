import { Component, input, output } from "@angular/core";

@Component({
  selector: "app-empty-state",
  standalone: true,
  imports: [],
  template: `
    <section class="empty-state" aria-labelledby="empty-title">
      <h2 id="empty-title">{{ title() }}</h2>
      <p>{{ message() }}</p>
      @if (actionLabel()) {
        <button type="button" (click)="action.emit()" class="btn-primary">
          {{ actionLabel() }}
        </button>
      }
    </section>
  `,
  styles: [
    `
      .empty-state {
        text-align: center;
        padding: 48px 20px;
        color: #4b5563;
      }
      .empty-state h2 {
        font-size: 1.25rem;
        color: #1f2937;
        margin-bottom: 8px;
      }
      .empty-state p {
        margin-bottom: 24px;
        max-width: 420px;
        margin-left: auto;
        margin-right: auto;
      }
      .btn-primary {
        background-color: #17689b;
        color: #ffffff;
        border: none;
        padding: 10px 20px;
        border-radius: 4px;
        font-size: 14px;
        cursor: pointer;
      }
      .btn-primary:focus-visible {
        outline: 3px solid #145c9c;
        outline-offset: 3px;
      }
    `,
  ],
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly actionLabel = input<string | null>(null);
  readonly action = output<void>();
}
