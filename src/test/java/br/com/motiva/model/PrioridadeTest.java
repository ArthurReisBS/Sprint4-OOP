package br.com.motiva.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrioridadeTest {

    @Test
    void urgenteECriticoFicamVermelhosNoMapa() {
        assertThat(Prioridade.URGENTE.corNoMapa()).isEqualTo("vermelho");
        assertThat(Prioridade.CRITICO.corNoMapa()).isEqualTo("vermelho");
        assertThat(Prioridade.ATENCAO.corNoMapa()).isEqualTo("amarelo");
        assertThat(Prioridade.NORMAL.corNoMapa()).isEqualTo("verde");
    }

    @Test
    void soUrgenteECriticoEntramNoCronograma() {
        assertThat(Prioridade.URGENTE.exigeIntervencao()).isTrue();
        assertThat(Prioridade.CRITICO.exigeIntervencao()).isTrue();
        assertThat(Prioridade.ATENCAO.exigeIntervencao()).isFalse();
        assertThat(Prioridade.NORMAL.exigeIntervencao()).isFalse();
    }

    @Test
    void limiaresSobemDaFaixaVerdeAteOCorte() {
        assertThat(Prioridade.FAIXA_VERDE_MAX_CM)
                .isLessThan(Prioridade.FAIXA_AMARELA_MAX_CM)
                .isLessThan(Prioridade.ALTURA_CORTE_RECOMENDADO_CM);
    }

    @Test
    void ordemDoEnumEADoRelatorio() {
        assertThat(Prioridade.values()).containsExactly(
                Prioridade.URGENTE, Prioridade.CRITICO, Prioridade.ATENCAO, Prioridade.NORMAL);
    }
}
