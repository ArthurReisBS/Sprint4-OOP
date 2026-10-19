package br.com.motiva.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RelatorioPrioridadeTest {

    @Test
    void totaisSomamAsFaixas() {
        RelatorioPrioridade relatorio = new RelatorioPrioridade(2, 2, 2, 3, "resumo");

        assertThat(relatorio.totalTrechos()).isEqualTo(9);
        assertThat(relatorio.totalAcionaveis()).isEqualTo(4);
    }

    @Test
    void prePersistPreencheADataQuandoVazia() {
        RelatorioPrioridade relatorio = new RelatorioPrioridade(0, 0, 0, 0, null);

        relatorio.preencherDataGeracao();

        assertThat(relatorio.getDataGeracao()).isNotNull();
    }

    @Test
    void prePersistNaoSobrescreveDataJaInformada() {
        LocalDateTime semanaPassada = LocalDateTime.now().minusDays(7);
        RelatorioPrioridade relatorio = new RelatorioPrioridade(0, 0, 0, 0, null);
        relatorio.setDataGeracao(semanaPassada);

        relatorio.preencherDataGeracao();

        assertThat(relatorio.getDataGeracao()).isEqualTo(semanaPassada);
    }
}
