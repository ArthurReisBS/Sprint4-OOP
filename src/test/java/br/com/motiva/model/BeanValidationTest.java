package br.com.motiva.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** As anotacoes das entidades, conferidas direto pelo validador, sem Spring nem banco. */
class BeanValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static TrechoRodovia trechoValido() {
        return new TrechoRodovia("SP-021", 12.0, 18.5, EspecieGrama.PASPALUM, 22.0,
                LocalDate.now().minusDays(8), null);
    }

    private <T> Set<String> mensagens(T objeto) {
        Set<ConstraintViolation<T>> violacoes = validator.validate(objeto);
        return violacoes.stream().map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void trechoValidoNaoTemViolacao() {
        assertThat(mensagens(trechoValido())).isEmpty();
    }

    @Test
    void alturaNegativaViolaOPositiveOrZero() {
        TrechoRodovia trecho = trechoValido();
        trecho.setAlturaVegetacaoCm(-0.5);

        assertThat(mensagens(trecho)).containsExactly("Altura da vegetação não pode ser negativa.");
    }

    @Test
    void alturaZeroEValida() {
        TrechoRodovia trecho = trechoValido();
        trecho.setAlturaVegetacaoCm(0.0);

        assertThat(mensagens(trecho)).isEmpty();
    }

    @Test
    void todasAsViolacoesAparecemJuntas() {
        TrechoRodovia trecho = trechoValido();
        trecho.setRodovia(" ");
        trecho.setEspeciePredominante(null);
        trecho.setDataUltimaMedicao(null);

        assertThat(mensagens(trecho)).containsExactlyInAnyOrder(
                "Rodovia é obrigatória.",
                "Espécie predominante é obrigatória.",
                "Data da última medição é obrigatória.");
    }

    @Test
    void medicaoNoFuturoEInvalida() {
        TrechoRodovia trecho = trechoValido();
        trecho.setDataUltimaMedicao(LocalDate.now().plusDays(1));

        assertThat(mensagens(trecho)).containsExactly("Data da última medição não pode estar no futuro.");
    }

    @Test
    void rodoviaMaiorQueAColunaEInvalida() {
        TrechoRodovia trecho = trechoValido();
        trecho.setRodovia("X".repeat(21));

        assertThat(mensagens(trecho)).containsExactly("Rodovia passa de 20 caracteres.");
    }

    @Test
    void equipePrecisaDe1A999Integrantes() {
        EquipeManutencao vazia = new EquipeManutencao("Equipe", TipoEquipe.ROCADA, 0, true);
        EquipeManutencao grande = new EquipeManutencao("Equipe", TipoEquipe.ROCADA, 1000, true);
        EquipeManutencao certa = new EquipeManutencao("Equipe", TipoEquipe.ROCADA, 999, true);

        assertThat(mensagens(vazia)).containsExactly("Quantidade de integrantes deve ser de 1 a 999.");
        assertThat(mensagens(grande)).containsExactly("Quantidade de integrantes deve ser de 1 a 999.");
        assertThat(mensagens(certa)).isEmpty();
    }

    @Test
    void intervencaoComCustoNegativoELarguraZeroEInvalida() {
        EquipeManutencao equipe = new EquipeManutencao("Alfa", TipoEquipe.ROCADA, 6, true);
        RocadaMecanizada rocada = new RocadaMecanizada(trechoValido(), equipe, LocalDate.now(),
                -10.0, 0.0, 10.0);

        assertThat(mensagens(rocada)).containsExactlyInAnyOrder(
                "Custo não pode ser negativo.",
                "Largura da faixa deve ser positiva.");
    }

    @Test
    void pulverizacaoSemColunasDaRocadaEValida() {
        EquipeManutencao equipe = new EquipeManutencao("Charlie", TipoEquipe.PULVERIZACAO, 4, true);

        assertThat(mensagens(new Pulverizacao(trechoValido(), equipe, LocalDate.now(), 3200.0))).isEmpty();
    }
}
