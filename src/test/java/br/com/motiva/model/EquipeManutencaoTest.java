package br.com.motiva.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EquipeManutencaoTest {

    @Test
    void equipeDisponivelECompativelPodeAtender() {
        EquipeManutencao equipe = new EquipeManutencao("Alfa", TipoEquipe.ROCADA, 6, true);

        assertThat(equipe.podeAtender(TipoIntervencao.ROCADA)).isTrue();
    }

    @Test
    void equipeIndisponivelNaoAtendeMesmoSendoCompativel() {
        EquipeManutencao equipe = new EquipeManutencao("Delta", TipoEquipe.MISTA, 8, false);

        assertThat(equipe.podeAtender(TipoIntervencao.ROCADA)).isFalse();
    }

    @Test
    void equipeDisponivelMasIncompativelNaoAtende() {
        EquipeManutencao equipe = new EquipeManutencao("Charlie", TipoEquipe.PULVERIZACAO, 4, true);

        assertThat(equipe.podeAtender(TipoIntervencao.ROCADA)).isFalse();
    }
}
