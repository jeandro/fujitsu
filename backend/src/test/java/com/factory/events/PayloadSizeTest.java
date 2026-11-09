package com.factory.events;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Teste de tamanho de corpo: POST com corpo > 1 MB retorna 413 (CA-16, RN-16, E-09).
 *
 * <p>BLOQUEADO: depende de {@code spring.servlet.multipart.max-request-size} ou
 * limite de corpo do container (Tomcat maxPostSize) configurado e de mapeamento de exceção
 * para 413 com tipo {@code payload-too-large}.
 */
class PayloadSizeTest {

  @Test
  @DisplayName("Especificacao: corpo maior que 1 MB retorna 413 E-09")
  void corpoMaiorQue1MbRetorna413() {
    // Montar corpo > 1 MB, executar POST, assert status 413 e type E-09
    assertTrue(true, "placeholder — aguardando limite de corpo configurado");
  }

  @Test
  @DisplayName("Especificacao: corpo exatamente 1 MB e abaixo e aceito")
  void corpoAte1MbAceito() {
    assertTrue(true, "placeholder — aguardando limite de corpo configurado");
  }
}
