export interface AuthState {
  token: string | null;
  role: "operador" | "gerente" | "visitante" | null;
  authenticated: boolean;
}

export const initialAuthState: AuthState = {
  token: null,
  role: null,
  authenticated: false,
};
