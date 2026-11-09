import { inject } from "@angular/core";
import { signal, computed } from "@angular/core";
import { AuthState, initialAuthState } from "./auth.model";

/**
 * Estado de autenticação em memória. Token NUNCA é gravado em localStorage/sessionStorage
 * (segurança: dado sensível). O papel é lido do token/perfil e exposto via signal.
 */
export class AuthService {
  private readonly state = signal<AuthState>(initialAuthState);

  readonly token = computed(() => this.state().token);
  readonly role = computed(() => this.state().role);
  readonly authenticated = computed(() => this.state().authenticated);

  login(token: string, role: "operador" | "gerente" | "visitante"): void {
    this.state.set({ token, role, authenticated: true });
  }

  logout(): void {
    this.state.set(initialAuthState);
  }

  hasRole(role: "operador" | "gerente"): boolean {
    return this.state().authenticated && this.state().role === role;
  }

  canRegister(): boolean {
    return this.hasRole("operador");
  }

  canRead(): boolean {
    return (
      this.state().authenticated &&
      (this.state().role === "operador" || this.state().role === "gerente")
    );
  }
}
