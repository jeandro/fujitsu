import { HttpErrorResponse } from "@angular/common/http";
export function apiErrorMessage(error: HttpErrorResponse): string {
  if (error.status === 0)
    return "Não foi possível conectar à API. Verifique a conexão e tente novamente.";
  if (error.status >= 500)
    return "O servidor não conseguiu concluir a operação. Tente novamente mais tarde.";
  if (error.status === 409)
    return error.error?.message || "Este ID já foi processado.";
  if (error.status === 400) return "Verifique os campos informados.";
  return "Não foi possível concluir a operação. Tente novamente.";
}
