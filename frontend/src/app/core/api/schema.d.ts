/**
 * Este arquivo é GERADO por `npm run api:types` a partir de `docs/api/openapi.yaml`.
 * Não edite manualmente; alterações serão sobrescritas na próxima geração.
 * Fonte da verdade: docs/api/openapi.yaml (contrato backend, squad backend-java).
 */

export type paths = {
  "/v1/events": {
    post: {
      parameters: {
        header: {
          "Idempotency-Key": string;
        };
        query: never;
      };
      requestBody: {
        content: {
          "application/json": components["schemas"]["RegisterEventRequest"];
        };
      };
      responses: {
        "200": {
          content: {
            "application/json": components["schemas"]["EventResponse"];
          };
        };
        "201": {
          content: {
            "application/json": components["schemas"]["EventResponse"];
          };
        };
        "400": components["responses"]["Problem400Validation"];
        "401": components["responses"]["Problem401Unauthorized"];
        "403": components["responses"]["Problem403Forbidden"];
        "409": {
          content: {
            "application/json": components["schemas"]["Problem"];
          };
        };
        "413": components["responses"]["Problem413TooLarge"];
        "429": components["responses"]["Problem429TooManyRequests"];
        "500": {
          content: {
            "application/json": components["schemas"]["Problem"];
          };
        };
      };
    };
    get: {
      parameters: {
        query: {
          page?: number;
          size?: number;
          sort?: string[];
        };
      };
      responses: {
        "200": {
          content: {
            "application/json": components["schemas"]["EventResponsePage"];
          };
        };
        "400": components["responses"]["Problem400Validation"];
        "401": components["responses"]["Problem401Unauthorized"];
        "403": components["responses"]["Problem403Forbidden"];
        "500": components["responses"]["Problem500Internal"];
      };
    };
  };
  "/v1/events/{eventId}": {
    get: {
      parameters: {
        path: {
          eventId: string;
        };
      };
      responses: {
        "200": {
          content: {
            "application/json": components["schemas"]["EventResponse"];
          };
        };
        "400": components["responses"]["Problem400Validation"];
        "401": components["responses"]["Problem401Unauthorized"];
        "403": components["responses"]["Problem403Forbidden"];
        "404": components["responses"]["Problem404NotFound"];
        "500": components["responses"]["Problem500Internal"];
      };
    };
  };
  "/v1/sagas/{sagaId}": {
    get: {
      parameters: {
        path: {
          sagaId: string;
        };
      };
      responses: {
        "200": {
          content: {
            "application/json": components["schemas"]["SagaState"];
          };
        };
        "401": components["responses"]["Problem401Unauthorized"];
        "403": components["responses"]["Problem403Forbidden"];
        "404": components["responses"]["Problem404NotFound"];
        "500": components["responses"]["Problem500Internal"];
      };
    };
  };
};

export type webhooks = {
  [path: string]: undefined;
};

export type components = {
  schemas: {
    Problem: {
      type: string;
      title: string;
      status: number;
      detail: string;
      instance: string;
      errors?: Record<string, string>;
      timestamp?: string;
      requestId?: string;
      [key: string]: unknown;
    };
    RegisterEventRequest: {
      eventId: string;
      eventType: components["schemas"]["EventType"];
      description: string;
      timestamp: string;
    };
    EventResponse: {
      eventId: string;
      eventType: components["schemas"]["EventType"];
      description: string;
      timestamp: string;
    };
    EventResponsePage: {
      content: components["schemas"]["EventResponse"][];
      page: number;
      size: number;
      sort?: string[];
      totalElements: number;
      totalPages: number;
      first?: boolean;
      last?: boolean;
      numberOfElements?: number;
      empty?: boolean;
    };
    EventType: "PRODUCTION" | "MAINTENANCE" | "QUALITY";
    SagaState: {
      sagaId: string;
      status:
        | "STARTED"
        | "IN_PROGRESS"
        | "SUCCEEDED"
        | "FAILED"
        | "FAILED_COMPENSATED"
        | "COMPENSATED";
      currentStep?: string;
      steps?: components["schemas"]["SagaStep"][];
      error: string | null;
      createdAt: string;
      updatedAt: string;
    };
    SagaStep: {
      step: string;
      status:
        | "STARTED"
        | "IN_PROGRESS"
        | "SUCCEEDED"
        | "FAILED"
        | "FAILED_COMPENSATED"
        | "COMPENSATED";
      attempts: number;
      error: string | null;
    };
  };
  parameters: {
    IdempotencyKey: {
      header: {
        "Idempotency-Key": string;
      };
    };
    Page: {
      query: {
        page?: number;
      };
    };
    Size: {
      query: {
        size?: number;
      };
    };
    Sort: {
      query: {
        sort?: string[];
      };
    };
  };
  responses: {
    Problem400Validation: {
      content: {
        "application/json": components["schemas"]["Problem"];
      };
    };
    Problem401Unauthorized: {
      content: {
        "application/json": components["schemas"]["Problem"];
      };
    };
    Problem403Forbidden: {
      content: {
        "application/json": components["schemas"]["Problem"];
      };
    };
    Problem404NotFound: {
      content: {
        "application/json": components["schemas"]["Problem"];
      };
    };
    Problem413TooLarge: {
      content: {
        "application/json": components["schemas"]["Problem"];
      };
    };
    Problem429TooManyRequests: {
      content: {
        "application/json": components["schemas"]["Problem"];
      };
    };
    Problem500Internal: {
      content: {
        "application/json": components["schemas"]["Problem"];
      };
    };
  };
  requestBodies: never;
  headers: never;
};

export type $defs = Record<string, never>;

export type operations = {
  registrarEvento: paths["/v1/events"]["post"];
  listarEventos: paths["/v1/events"]["get"];
  obterEvento: paths["/v1/events/{eventId}"]["get"];
  consultarSaga: paths["/v1/sagas/{sagaId}"]["get"];
};
