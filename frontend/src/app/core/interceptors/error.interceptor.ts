import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError } from "rxjs/operators";
import { throwError } from "rxjs";
import { AuthService } from "../auth/auth.service";

/**
 * Tratamento global de erros HTTP:
 * - 401: desloga e redireciona para login com feedback ("Sessão expirada..."), preservando returnUrl.
 * - 403: mantém sessão; o consumidor exibe estado de sem permissão.
 * - Outros: reemite para o consumidor tratar (resource/error signal ou service).
 * Não loga payload de resposta (evitar PII confidencial LGPD).
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401) {
        auth.logout();
        const returnUrl = req.urlWithParams ?? router.url;
        void router.navigate(["/login"], {
          queryParams: { returnUrl },
        });
      } else if (error.status === 403) {
        // papel sem permissão; mantém sessão mas sinaliza via auth (não altera estado aqui)
      }
      return throwError(() => error);
    }),
  );
};
