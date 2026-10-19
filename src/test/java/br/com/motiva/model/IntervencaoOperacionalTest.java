package br.com.motiva.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntervencaoOperacionalTest {

    private final TrechoRodovia trecho = new TrechoRodovia("SP-021", 12.0, 18.5,
            EspecieGrama.PASPALUM, 22.0, LocalDate.now(), null);
    private final EquipeManutencao equipe =
            new EquipeManutencao("Alfa", TipoEquipe.MISTA, 6, true);

    private Locale localeOriginal;

    @BeforeEach
    void fixarLocale() {
        localeOriginal = Locale.getDefault();
        Locale.setDefault(new Locale("pt", "BR"));
    }

    @AfterEach
    void restaurarLocale() {
        Locale.setDefault(localeOriginal);
    }

    @Test
    void cadaSubclasseInformaOProprioTipo() {
        IntervencaoOperacional rocada =
                new RocadaMecanizada(trecho, equipe, LocalDate.now(), 4850.0, 3.5, 10.0);
        IntervencaoOperacional pulverizacao =
                new Pulverizacao(trecho, equipe, LocalDate.now(), 3200.0);

        assertThat(rocada.getTipo()).isEqualTo(TipoIntervencao.ROCADA);
        assertThat(pulverizacao.getTipo()).isEqualTo(TipoIntervencao.PULVERIZACAO);
    }

    @Test
    void descricaoDaRocadaTrazFaixaEAlturaDeCorte() {
        RocadaMecanizada rocada =
                new RocadaMecanizada(trecho, equipe, LocalDate.now(), 4850.0, 3.5, 10.0);

        assertThat(rocada.descricaoTecnica()).isEqualTo("Roçada de 3,5 m de faixa, corte a 10 cm");
    }

    @Test
    void pulverizacaoNaoTemColunasDaRocada() {
        Pulverizacao pulverizacao = new Pulverizacao(trecho, equipe, LocalDate.now(), 3200.0);

        assertThat(pulverizacao.descricaoTecnica()).isEqualTo("Pulverização");
    }

    @Test
    void custoPorKmDivideOCustoPelaExtensaoDoTrecho() {
        // 6,5 km de trecho
        IntervencaoOperacional pulverizacao =
                new Pulverizacao(trecho, equipe, LocalDate.now(), 3250.0);

        assertThat(pulverizacao.custoPorKm()).isEqualTo(500.0);
    }
}
