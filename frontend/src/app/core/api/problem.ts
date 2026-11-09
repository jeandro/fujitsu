import { HttpErrorResponse } from "@angular/common/http";

/**
 * Extrai a mensagem segura do usuário de uma resposta de erro HTTP (ProblemDetail RFC 9457).
 * Nunca expõe `detail` bruto de 5xx no log da UI; o `type` do Problem é o contrato estável.
 * Campos pessoais (ex.: description confidencial LGPD) nunca entram no console.
 */
export interface ProblemSafe {
  status: number;
  type: string;
  title: string;
  detail: string; // mensagem legível ao usuário, sem PII
  instance?: string;
  errors?: Record<string, string>;
}

export function problemMessage(error: HttpErrorResponse): ProblemSafe {
  const body = error.error as Partial<ProblemSafe> | undefined;

  // 5xx: nunca espelha detalhes internos (stack, SQL, segredos) para o usuário
  if (error.status >= 500) {
    return {
      status: error.status,
      type: body?.type ?? "https://api.factory/events/errors/internal-error",
      title: body?.title ?? "Erro interno",
      detail:
        body?.detail ??
        "O servidor não conseguiu concluir a operação. Tente novamente mais tarde.",
      instance: body?.instance,
    };
  }

  // Erros de rede (sem resposta do servidor)
  if (error.status === 0) {
    return {
      status: 0,
      type: "https://api.factory/events/errors/internal-error",
      title: "Sem conexão",
      detail:
        "Não foi possível conectar à API. Verifique a conexão e tente novamente.",
    };
  }

  return {
    status: error.status,
    type: body?.type ?? "",
    title: body?.title ?? "Erro na requisição",
    detail: body?.detail ?? "Não foi possível concluir a operação.",
    instance: body?.instance,
    errors: body?.errors,
  };
}

/**
 * Verdadeiro quando o erro é de idempotência reutilizada com payload diferente (RN-04, E-03).
 */
export function isIdempotencyKeyReused(error: HttpErrorResponse): boolean {
  return (
    error.status === 409 &&
    (error.error as { type?: string })?.type ===
      "https://api.factory/events/errors/idempotency-key-reused"
  );
}

/**
 * Verdadeiro quando o erro é de execução de idempotência em andamento (RN-05, E-04).
 */
export function isIdempotencyInProgress(error: HttpErrorResponse): boolean {
  return (
    error.status === 409 &&
    (error.error as { type?: string })?.type ===
      "https://api.factory/events/errors/idempotency-in-progress"
  );
}

/**
 * Verdadeiro quando o erro é de evento duplicado (RN-02, E-02).
 */
export function isDuplicateEvent(error: HttpErrorResponse): boolean {
  return (
    error.status === 409 &&
    (error.error as { type?: string })?.type ===
      "https://api.factory/events/errors/duplicate-event"
  );
}

/**
 * Verdadeiro quando a falha é de compensação de auditoria (RN-07, E-05).
 */
export function isAuditCompensationRequired(error: HttpErrorResponse): boolean {
  return (
    error.status === 500 &&
    (error.error as { type?: string })?.type ===
      "https://api.factory/events/errors/audit-compensation-required"
  );
}

/**
 * Mensagem final ao usuário (texto exato de docs/spec/telas.md §4) para cada `type` da API.
 * Nunca espelha o `detail` técnico da API. O `instance` (requestId) é mostrado apenas para suporte.
 */
export function errorMessageFor(type: string | undefined, status: number): string {
  const t = type ?? "";

  if (t === "https://api.factory/events/errors/idempotency-key-reused") {
    return "Esta ação já foi processada com dados diferentes. Reinicie o registro com uma nova ação.";
  }
  if (t === "https://api.factory/events/errors/idempotency-in-progress") {
    return "O registro já está em andamento. Aguarde a conclusão.";
  }
  if (t === "https://api.factory/events/errors/duplicate-event") {
    return "O evento com este ID já foi processado anteriormente.";
  }
  if (t === "https://api.factory/events/errors/audit-compensation-required") {
    return "Não foi possível concluir o registro: o log de auditoria QUALITY não pôde ser gravado. A compensação foi executada e auditada. O evento não foi persistido.";
  }
  if (t === "https://api.factory/events/errors/not-found") {
    return "Recurso não encontrado.";
  }
  if (t === "https://api.factory/events/errors/method-not-allowed") {
    return "Método não permitido para este recurso.";
  }
  if (t === "https://api.factory/events/errors/unsupported-media-type") {
    return "Formato não suportado. Utilize application/json.";
  }
  if (t === "https://api.factory/events/errors/payload-too-large") {
    return "O corpo da requisição excede o limite de 1 MB. Reduza a descrição e tente novamente.";
  }
  if (t === "https://api.factory/events/errors/too-many-requests") {
    return "Limite de 100 requisições por minuto excedido. Tente novamente mais tarde.";
  }
  if (t === "https://api.factory/events/errors/unauthorized") {
    return "Autenticação necessária para acessar este recurso.";
  }
  if (t === "https://api.factory/events/errors/forbidden") {
    return "Seu papel não tem permissão para executar esta ação.";
  }
  // 5xx ou desconhecido → texto genérico sem detalhe técnico
  return "Erro interno. Tente novamente ou informe o identificador da requisição ao suporte.";
}

/**
 * Mensagem de erro de validação de campo (E-01) com texto exato da spec.
 */
export function fieldErrorMessage(field: string, rule: "required" | "maxlength" | "pattern" | "format", max?: number): string {
  switch (rule) {
    case "required":
      return `O ${field} é obrigatório.`;
    case "maxlength":
      return `O ${field} deve ter até ${max} caracteres.`;
    case "pattern":
      return `O ${field} não pode conter apenas espaços.`;
    case "format":
      return "Informe um timestamp válido (formato ISO 8601).";
  }
}
