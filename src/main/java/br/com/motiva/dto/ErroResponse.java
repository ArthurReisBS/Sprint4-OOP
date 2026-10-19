package br.com.motiva.dto;

import java.time.LocalDateTime;

/** Corpo de toda resposta de erro da API. */
public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho
) {
}
