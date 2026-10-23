import { HttpErrorResponse } from "@angular/common/http";
import { apiErrorMessage } from "./api-error";
describe("apiErrorMessage", () => {
  it("distingue conexão, servidor, conflito e validação", () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 0 }))).toContain(
      "conectar",
    );
    expect(
      apiErrorMessage(
        new HttpErrorResponse({
          status: 500,
          error: { message: "SQL SECRET" },
        }),
      ),
    ).not.toContain("SQL SECRET");
    expect(
      apiErrorMessage(
        new HttpErrorResponse({ status: 409, error: { message: "Duplicado" } }),
      ),
    ).toBe("Duplicado");
    expect(apiErrorMessage(new HttpErrorResponse({ status: 400 }))).toContain(
      "campos",
    );
  });
});
