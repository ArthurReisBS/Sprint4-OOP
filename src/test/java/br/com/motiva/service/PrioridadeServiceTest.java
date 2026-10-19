package br.com.motiva.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.TrechoRodovia;

class PrioridadeServiceTest {

    private final PrioridadeService service = new PrioridadeService();

    private static TrechoRodovia trecho(EspecieGrama especie, double alturaCm, int diasDesdeMedicao) {
        return new TrechoRodovia("SP-021", 0.0, 1.0, especie, alturaCm,
                LocalDate.now().minusDays(diasDesdeMedicao), null);
    }

    // os trechos do seu-script-dados.sql da Sprint 3, com a faixa calculada no comentario
    @ParameterizedTest(name = "{0} {1} cm há {2} dias -> {3} cm, {4}")
    @CsvSource({
            "PASPALUM,    22.0, 8, 30.8, URGENTE",
            "MEGATHYRSUS,  5.0, 3, 33.5, URGENTE",
            "PASPALUM,    20.0, 6, 26.6, CRITICO",
            "CYNODON,     22.0, 3, 26.5, CRITICO",
            "PASPALUM,    14.0, 5, 19.5, ATENCAO",
            "BRACHIARIA,   8.0, 3, 20.6, ATENCAO",
            "CYNODON,      6.0, 4, 12.0, NORMAL",
            "PASPALUM,     5.0, 3,  8.3, NORMAL",
            "BRACHIARIA,   2.0, 2, 10.4, NORMAL"
    })
    void classificaOsTrechosDosDadosDaSprint3(EspecieGrama especie, double alturaCm, int dias,
                                              double estimadaCm, Prioridade esperada) {
        TrechoRodovia t = trecho(especie, alturaCm, dias);

        assertThat(service.alturaEstimadaCm(t)).isCloseTo(estimadaCm, within(0.001));
        assertThat(service.prioridadeDo(t)).isEqualTo(esperada);
    }

    @ParameterizedTest(name = "{0} cm -> {1}")
    @CsvSource({
            " 0.0,  NORMAL",
            "15.0,  NORMAL",
            "15.01, ATENCAO",
            "25.0,  ATENCAO",
            "25.01, CRITICO",
            "29.99, CRITICO",
            "30.0,  URGENTE"
    })
    void limiaresIntermediariosSaoExclusivosEOCorteEInclusivo(double alturaCm, Prioridade esperada) {
        assertThat(service.classificar(alturaCm)).isEqualTo(esperada);
    }

    @Test
    void alturaNegativaERecusada() {
        assertThatThrownBy(() -> service.classificar(-0.1))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void estimativaParaNoPorteMaximoDaEspecie() {
        // 14,5 cm/dia x 100 dias passaria de 1400 cm; o capim-elefante para em 250
        TrechoRodovia t = trecho(EspecieGrama.PENNISETUM, 10.0, 100);

        assertThat(service.alturaEstimadaCm(t)).isEqualTo(250.0);
    }

    @Test
    void medicaoComDataFuturaNaoEncolheAAltura() {
        TrechoRodovia t = trecho(EspecieGrama.BRACHIARIA, 20.0, -5);

        assertThat(service.alturaEstimadaCm(t)).isEqualTo(20.0);
    }

    @Test
    void diasAteCorteArredondaParaCima() {
        // falta 24 cm para os 30, a 1,5 cm/dia: 16 dias
        assertThat(service.diasAteCorte(trecho(EspecieGrama.CYNODON, 6.0, 0))).isEqualTo(16);
        // 25 cm a 1,1 cm/dia: 22,7 -> 23 dias
        assertThat(service.diasAteCorte(trecho(EspecieGrama.PASPALUM, 5.0, 0))).isEqualTo(23);
    }

    @Test
    void diasAteCorteEZeroQuandoJaPassou() {
        assertThat(service.diasAteCorte(trecho(EspecieGrama.PASPALUM, 22.0, 8))).isZero();
    }

    @Test
    void agrupamentoTrazTodasAsFaixasMesmoVazias() {
        Map<Prioridade, List<TrechoRodovia>> mapa =
                service.agruparPorPrioridade(List.of(trecho(EspecieGrama.CYNODON, 6.0, 4)));

        assertThat(mapa.keySet()).containsExactly(Prioridade.values());
        assertThat(mapa.get(Prioridade.NORMAL)).hasSize(1);
        assertThat(mapa.get(Prioridade.URGENTE)).isEmpty();
    }

    @Test
    void dentroDaFaixaOMaisAltoVemPrimeiro() {
        TrechoRodovia urgenteMaisBaixo = trecho(EspecieGrama.PASPALUM, 22.0, 8);    // 30,8
        TrechoRodovia urgenteMaisAlto = trecho(EspecieGrama.MEGATHYRSUS, 5.0, 3);   // 33,5

        Map<Prioridade, List<TrechoRodovia>> mapa =
                service.agruparPorPrioridade(List.of(urgenteMaisBaixo, urgenteMaisAlto));

        assertThat(mapa.get(Prioridade.URGENTE)).containsExactly(urgenteMaisAlto, urgenteMaisBaixo);
    }
}
