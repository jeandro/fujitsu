import { HttpInterceptorFn } from "@angular/common/http";

/**
 * Propaga traceId no header `traceparent` para correlação com o backend (OpenTelemetry).
 * Nenhum dado pessoal no traceId (UUID aleatório).
 */
export const correlationInterceptor: HttpInterceptorFn = (req, next) => {
  const traceId = crypto.randomUUID();
  const headers = req.headers.set(
    "traceparent",
    `00-${traceId}-${traceId}-01`,
  );
  return next(req.clone({ headers }));
};
