package br.com.motiva.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.TrechoRodovia;

class ValidacaoTest {

    private final Validacao validacao = ValidacaoDeTeste.criar();

    private static TrechoRodovia trechoValido() {
        return new TrechoRodovia("SP-021", 12.0, 18.5, EspecieGrama.PASPALUM, 22.0,
                LocalDate.now(), null);
    }

    @Test
    void objetoValidoPassaSemExcecao() {
        assertThatCode(() -> validacao.validar(trechoValido())).doesNotThrowAnyException();
    }

    @Test
    void juntaTodasAsMensagensEmOrdemDeCampo() {
        TrechoRodovia trecho = trechoValido();
        trecho.setRodovia("");
        trecho.setAlturaVegetacaoCm(-1.0);

        // alturaVegetacaoCm vem antes de rodovia na ordem alfabetica dos campos
        assertThatThrownBy(() -> validacao.validar(trecho))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Altura da vegetação não pode ser negativa. Rodovia é obrigatória.");
    }
}
