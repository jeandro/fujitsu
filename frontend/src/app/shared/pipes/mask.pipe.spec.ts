import { TestBed } from "@angular/core/testing";
import { MaskConfidentialPipe } from "./mask.pipe";
import { AuthService } from "../../core/auth/auth.service";

function createAuth(authed: boolean, role: "operador" | "gerente" | "visitante" | null) {
  const auth = new AuthService();
  if (authed && role) {
    auth.login("token-test", role);
  }
  return auth;
}

describe("MaskConfidentialPipe (LGPD RN-17/RN-20)", () => {
  it("mascara description para papel não autenticado", () => {
    TestBed.configureTestingModule({
      providers: [
        MaskConfidentialPipe,
        { provide: AuthService, useValue: createAuth(false, null) },
      ],
    });
    const pipe = TestBed.inject(MaskConfidentialPipe);
    const masked = pipe.transform("dado pessoal sensível");
    expect(masked).toBe("[dado restrito]");
  });

  it("mascara description para papel visitante", () => {
    TestBed.configureTestingModule({
      providers: [
        MaskConfidentialPipe,
        { provide: AuthService, useValue: createAuth(true, "visitante") },
      ],
    });
    const pipe = TestBed.inject(MaskConfidentialPipe);
    expect(pipe.transform("texto")).toBe("•".repeat("texto".length));
  });

  it("deixa description visível para operador", () => {
    TestBed.configureTestingModule({
      providers: [
        MaskConfidentialPipe,
        { provide: AuthService, useValue: createAuth(true, "operador") },
      ],
    });
    const pipe = TestBed.inject(MaskConfidentialPipe);
    expect(pipe.transform("dado visível")).toBe("dado visível");
  });

  it("deixa description visível para gerente", () => {
    TestBed.configureTestingModule({
      providers: [
        MaskConfidentialPipe,
        { provide: AuthService, useValue: createAuth(true, "gerente") },
      ],
    });
    const pipe = TestBed.inject(MaskConfidentialPipe);
    expect(pipe.transform("dado visível")).toBe("dado visível");
  });

  it("retorna vazio para null/undefined", () => {
    TestBed.configureTestingModule({
      providers: [
        MaskConfidentialPipe,
        { provide: AuthService, useValue: createAuth(false, null) },
      ],
    });
    const pipe = TestBed.inject(MaskConfidentialPipe);
    expect(pipe.transform(null)).toBe("");
    expect(pipe.transform(undefined)).toBe("");
    expect(pipe.transform("")).toBe("");
  });
});
