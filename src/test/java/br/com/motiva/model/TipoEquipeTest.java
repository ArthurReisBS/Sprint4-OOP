package br.com.motiva.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TipoEquipeTest {

    @Test
    void equipeMistaExecutaOsDoisTipos() {
        assertThat(TipoEquipe.MISTA.podeExecutar(TipoIntervencao.ROCADA)).isTrue();
        assertThat(TipoEquipe.MISTA.podeExecutar(TipoIntervencao.PULVERIZACAO)).isTrue();
    }

    @Test
    void equipeDeRocadaSoExecutaRocada() {
        assertThat(TipoEquipe.ROCADA.podeExecutar(TipoIntervencao.ROCADA)).isTrue();
        assertThat(TipoEquipe.ROCADA.podeExecutar(TipoIntervencao.PULVERIZACAO)).isFalse();
    }

    @Test
    void equipeDePulverizacaoSoExecutaPulverizacao() {
        assertThat(TipoEquipe.PULVERIZACAO.podeExecutar(TipoIntervencao.PULVERIZACAO)).isTrue();
        assertThat(TipoEquipe.PULVERIZACAO.podeExecutar(TipoIntervencao.ROCADA)).isFalse();
    }
}
