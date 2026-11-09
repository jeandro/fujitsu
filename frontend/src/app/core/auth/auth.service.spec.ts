import { AuthService } from "./auth.service";

describe("AuthService", () => {
  let auth: AuthService;

  beforeEach(() => {
    auth = new AuthService();
  });

  it("inicializa não autenticado e sem papel", () => {
    expect(auth.authenticated()).toBeFalse();
    expect(auth.token()).toBeNull();
    expect(auth.role()).toBeNull();
    expect(auth.canRegister()).toBeFalse();
    expect(auth.canRead()).toBeFalse();
  });

  it("login define token, papel e autenticado em memória", () => {
    auth.login("token-abc", "operador");
    expect(auth.authenticated()).toBeTrue();
    expect(auth.token()).toBe("token-abc");
    expect(auth.role()).toBe("operador");
    expect(auth.canRegister()).toBeTrue();
    expect(auth.canRead()).toBeTrue();
  });

  it("gerente pode ler mas não registrar (RN-13/RN-14)", () => {
    auth.login("token-abc", "gerente");
    expect(auth.canRead()).toBeTrue();
    expect(auth.canRegister()).toBeFalse();
  });

  it("logout limpa estado", () => {
    auth.login("token-abc", "operador");
    auth.logout();
    expect(auth.authenticated()).toBeFalse();
    expect(auth.token()).toBeNull();
    expect(auth.role()).toBeNull();
    expect(auth.canRegister()).toBeFalse();
    expect(auth.canRead()).toBeFalse();
  });
});
