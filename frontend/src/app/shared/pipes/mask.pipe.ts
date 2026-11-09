import { Pipe, PipeTransform, inject } from "@angular/core";
import { AuthService } from "../../core/auth/auth.service";

/**
 * Máscara LGPD para o campo `description` (classificado como confidencial, RN-17/RN-20).
 * Papéis autorizados (`operador`, `gerente`) veem o texto completo; outros veem máscara.
 * Usado apenas em apresentação; o dado nunca é alterado no modelo.
 */
@Pipe({ name: "maskConfidential", standalone: true })
export class MaskConfidentialPipe implements PipeTransform {
  private readonly auth = inject(AuthService);

  transform(value: string | null | undefined): string {
    if (!value) return "";
    const auth = this.auth;
    const canSee =
      auth.authenticated() &&
      (auth.role() === "operador" || auth.role() === "gerente");
    if (canSee) return value;
    return "[dado restrito]";
  }
}
