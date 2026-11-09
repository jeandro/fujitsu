import { Component, input, output } from "@angular/core";

@Component({
  selector: "app-permission-state",
  standalone: true,
  imports: [],
  template: `
    <section class="permission-state" role="alert" aria-live="polite">
      <h2>{{ title() }}</h2>
      <p>{{ message() }}</p>
      @if (actionLabel()) {
        <button type="button" (click)="action.emit()" class="btn-action">
          {{ actionLabel() }}
        </button>
      }
    </section>
  `,
  styles: [
    `
      .permission-state {
        text-align: center;
        padding: 32px 20px;
        color: #4b5563;
        background: #fdf6e3;
        border: 1px solid #e3d29a;
        border-radius: 4px;
        max-width: 560px;
        margin: 0 auto;
      }
      .permission-state h2 {
        font-size: 1.1rem;
        margin-top: 0;
        color: #7a5a00;
      }
      .permission-state p {
        margin: 12px 0;
      }
      .btn-action {
        background-color: #17689b;
        color: #ffffff;
        border: none;
        padding: 8px 16px;
        border-radius: 4px;
        font-size: 14px;
        cursor: pointer;
        margin-top: 12px;
      }
      .btn-action:focus-visible {
        outline: 3px solid #145c9c;
        outline-offset: 3px;
      }
    `,
  ],
})
export class PermissionStateComponent {
  readonly title = input<string>("Sem permissão");
  readonly message = input<string>("Seu papel não tem permissão para executar esta ação.");
  readonly actionLabel = input<string | null>(null);
  readonly action = output<void>();
}
