import { TestBed } from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { errorInterceptor } from "./error.interceptor";
import { AuthService } from "../auth/auth.service";

describe("errorInterceptor", () => {
  let http: HttpTestingController;
  let auth: any;
  let router: any;

  beforeEach(() => {
    auth = { logout: jasmine.createSpy("logout") };
    router = { navigate: jasmine.createSpy("navigate") };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("401 desloga e redireciona para login com returnUrl", () => {
    TestBed.runInInjectionContext(() => {
      const req = new Request("http://localhost/api/v1/events");
      errorInterceptor(req as any, {
        handle: () => {
          throw new HttpErrorResponse({ status: 401, url: "/api/v1/events" });
        },
      } as any).subscribe({
        error: () => {
          expect(auth.logout).toHaveBeenCalled();
          expect(router.navigate).toHaveBeenCalledWith("/login", {
            queryParams: { returnUrl: "/api/v1/events" },
          });
        },
      });
    });
    http.verify();
  });

  it("403 não desloga nem redireciona", () => {
    TestBed.runInInjectionContext(() => {
      const req = new Request("http://localhost/api/v1/events");
      errorInterceptor(req as any, {
        handle: () => {
          throw new HttpErrorResponse({ status: 403, url: "/api/v1/events" });
        },
      } as any).subscribe({
        error: () => {
          expect(auth.logout).not.toHaveBeenCalled();
          expect(router.navigate).not.toHaveBeenCalled();
        },
      });
    });
    http.verify();
  });

  it("outros erros são reemitidos sem efeito colateral", () => {
    TestBed.runInInjectionContext(() => {
      const req = new Request("http://localhost/api/v1/events");
      errorInterceptor(req as any, {
        handle: () => {
          throw new HttpErrorResponse({ status: 500, url: "/api/v1/events" });
        },
      } as any).subscribe({
        error: (err) => {
          expect(err.status).toBe(500);
          expect(auth.logout).not.toHaveBeenCalled();
        },
      });
    });
    http.verify();
  });
});
