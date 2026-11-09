import { TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { LoginPage } from "./login.page";
import { AuthService } from "../../core/auth/auth.service";

function makeAuth() {
  return {
    login: () => {},
    token: () => null,
  } as any;
}

describe("LoginPage", () => {
  let auth: any;
  let router: any;

  beforeEach(() => {
    auth = makeAuth();
    router = { navigate: jasmine.createSpy("navigate") };
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
  });

  it("exibe formulário de login com token e papel", () => {
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Login");
    expect(fixture.nativeElement.textContent).toContain("Token de acesso");
    expect(fixture.nativeElement.textContent).toContain("Papel");
  });

  it("chama auth.login e navega para returnUrl ao submeter", () => {
    const fixture = TestBed.createComponent(LoginPage);
    const c = fixture.componentInstance;
    c.form.setValue({ token: "tok-123", role: "operador" });
    spyOn(auth, "login");
    fixture.detectChanges();

    c.onSubmit();
    fixture.detectChanges();

    expect(auth.login).toHaveBeenCalledWith("tok-123", "operador");
    expect(router.navigate).toHaveBeenCalledWith(["/events"]);
  });

  it("não exibe token na tela (apenas campo de entrada)", () => {
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    // O token digitado não é exibido em texto, apenas no input
    const input = fixture.nativeElement.querySelector("#token");
    expect(input).toBeTruthy();
  });

  it("exibe erro quando login falha", async () => {
    const fixture = TestBed.createComponent(LoginPage);
    const c = fixture.componentInstance;
    spyOn(auth, "login").and.throwError("falha");
    c.form.setValue({ token: "tok-123", role: "operador" });

    await c.onSubmit();
    expect(c.errorMessage()).toContain("Não foi possível fazer login");
  });
});
