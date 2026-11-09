import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { lastValueFrom } from "rxjs";
import { components } from "./api/schema";

type EventResponse = components["schemas"]["EventResponse"];

interface PendingEntry {
  key: string;
  bodyHash: string; // hash SHA-256 do corpo canônico (description confidencial nunca em memória)
  promise: Promise<EventResponse>;
}

interface CompletedEntry {
  bodyHash: string;
  response: EventResponse;
}

/**
 * Chave de idempotência operacional por intenção de comando (UUID v4, gerada pelo cliente).
 * Replay com mesma chave + mesmo corpo devolve a resposta armazenada sem reenviar HTTP (RN-03).
 * Nunca armazena dado pessoal (description confidencial LGPD) neste mapa: guarda apenas o hash
 * do corpo para detecção de reuso de chave com payload diferente (RN-04).
 */
@Injectable({ providedIn: "root" })
export class IdempotencyStore {
  private readonly http = inject(HttpClient);
  private readonly pending = new Map<string, PendingEntry>();
  private readonly completed = new Map<string, CompletedEntry>();

  /** Gera uma nova chave de intenção por ação de registro (duplo clique = mesma intenção). */
  newKey(): string {
    return crypto.randomUUID();
  }

  /** Hash SHA-256 do corpo canônico (síncrono o suficiente para detecção de duplicidade). */
  private async bodyHash(body: unknown): Promise<string> {
    const data = new TextEncoder().encode(JSON.stringify(body));
    const hash = await crypto.subtle.digest("SHA-256", data);
    return Array.from(new Uint8Array(hash))
      .map((b) => b.toString(16).padStart(2, "0"))
      .join("");
  }

  /** Verifica se já existe resposta armazenada para esta chave + corpo (hash). */
  async hasReplay(key: string, bodyHash: string): Promise<boolean> {
    const entry = this.completed.get(key);
    return !!entry && entry.bodyHash === bodyHash;
  }

  /** Retorna a resposta armazenada (replay) sem reenviar HTTP. */
  async getReplay(key: string, bodyHash: string): Promise<EventResponse | undefined> {
    const entry = this.completed.get(key);
    return entry?.bodyHash === bodyHash ? entry.response : undefined;
  }

  /** Registra uma resposta concluída para replay futuro. */
  async store(key: string, bodyHash: string, response: EventResponse): Promise<void> {
    this.completed.set(key, { bodyHash, response });
  }

  /** Marca uma intenção como em andamento (evita duplo POST concorrente com mesma chave). */
  async markPending(key: string, bodyHash: string, promise: Promise<EventResponse>): Promise<void> {
    this.pending.set(key, { key, bodyHash, promise });
  }

  /** Remove a intenção do mapa de pendentes após concluir. */
  clearPending(key: string): void {
    this.pending.delete(key);
  }

  /** Verifica se há execução concorrente com mesma chave (RN-05). */
  isInProgress(key: string): boolean {
    return this.pending.has(key);
  }

  async postOnce(
    url: string,
    body: unknown,
    key: string,
  ): Promise<EventResponse> {
    const bodyHash = await this.bodyHash(body);

    // Replay: mesma chave + mesmo corpo (hash) → resposta armazenada, sem HTTP
    const replay = await this.getReplay(key, bodyHash);
    if (replay) return replay;

    // Concorrência com mesma chave: já existe em andamento
    if (this.isInProgress(key)) {
      const pending = this.pending.get(key)!;
      if (pending.bodyHash === bodyHash) {
        return pending.promise;
      }
      // Mesma chave, corpo diferente → deixa o backend responder 409 idempotency-key-reused
    }

    const promise = lastValueFrom(
      this.http.post<EventResponse>(url, body, {
        headers: { "Idempotency-Key": key },
      }),
    );

    await this.markPending(key, bodyHash, promise);

    try {
      const response = await promise;
      await this.store(key, bodyHash, response);
      return response;
    } finally {
      this.clearPending(key);
    }
  }
}
