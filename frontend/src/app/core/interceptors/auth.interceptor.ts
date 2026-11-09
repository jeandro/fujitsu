import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../auth/auth.service";

/**
 * Injeta `Authorization: Bearer <token>` quando houver sessão.
 * O token vive apenas em memória (não localStorage/sessionStorage).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  if (!token) return next(req);
  return next(
    req.clone({ headers: req.headers.set("Authorization", `Bearer ${token}`) }),
  );
};
