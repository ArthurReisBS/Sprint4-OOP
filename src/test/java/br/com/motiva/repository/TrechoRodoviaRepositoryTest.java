package br.com.motiva.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.TrechoRodovia;
import jakarta.validation.ConstraintViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TrechoRodoviaRepositoryTest {

    @Autowired
    private TrechoRodoviaRepository repository;

    private final LocalDate hoje = LocalDate.now();

    private TrechoRodovia trecho(String rodovia, double kmInicial, EspecieGrama especie,
                                 double alturaCm, LocalDate ultimaIntervencao) {
        return new TrechoRodovia(rodovia, kmInicial, kmInicial + 6, especie, alturaCm,
                hoje.minusDays(3), ultimaIntervencao);
    }

    @BeforeEach
    void popular() {
        // fora de ordem de km de proposito
        repository.save(trecho("SP-021", 47.0, EspecieGrama.PASPALUM, 20.0, hoje.minusDays(40)));
        repository.save(trecho("SP-021", 12.0, EspecieGrama.PASPALUM, 22.0, hoje.minusDays(34)));
        repository.save(trecho("SP-021", 32.0, EspecieGrama.MEGATHYRSUS, 5.0, null));
        repository.save(trecho("SP-280", 10.0, EspecieGrama.CYNODON, 6.0, hoje.minusDays(12)));
        repository.flush();
    }

    @Test
    void findByRodoviaOrdenaPeloKmInicial() {
        assertThat(repository.findByRodoviaOrderByKmInicialAsc("SP-021"))
                .extracting(TrechoRodovia::getKmInicial)
                .containsExactly(12.0, 32.0, 47.0);
    }

    @Test
    void findByEspeciePredominante() {
        assertThat(repository.findByEspeciePredominante(EspecieGrama.PASPALUM)).hasSize(2);
        assertThat(repository.findByEspeciePredominante(EspecieGrama.PENNISETUM)).isEmpty();
    }

    @Test
    void findByAlturaVegetacaoGreaterThanEqualIncluiOLimite() {
        assertThat(repository.findByAlturaVegetacaoCmGreaterThanEqual(20.0))
                .extracting(TrechoRodovia::getAlturaVegetacaoCm)
                .containsExactlyInAnyOrder(20.0, 22.0);
    }

    @Test
    void findByDataUltimaIntervencaoIsNull() {
        assertThat(repository.findByDataUltimaIntervencaoIsNull())
                .extracting(TrechoRodovia::getKmInicial)
                .containsExactly(32.0);
    }

    @Test
    void existsPelaFaixaCompleta() {
        assertThat(repository.existsByRodoviaAndKmInicialAndKmFinal("SP-021", 12.0, 18.0)).isTrue();
        assertThat(repository.existsByRodoviaAndKmInicialAndKmFinal("SP-021", 12.0, 19.0)).isFalse();
        assertThat(repository.existsByRodoviaAndKmInicialAndKmFinal("SP-280", 12.0, 18.0)).isFalse();
    }

    @Test
    void bancoRecusaFaixaRepetida() {
        assertThatThrownBy(() -> repository.saveAndFlush(
                trecho("SP-021", 12.0, EspecieGrama.CYNODON, 1.0, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void hibernateConfereAsAnotacoesAntesDoInsert() {
        // gravando direto pelo Repository, sem passar pelo Service: o Hibernate
        // valida a entidade e nem chega a mandar o INSERT
        TrechoRodovia negativo = trecho("SP-280", 50.0, EspecieGrama.CYNODON, -1.0, null);

        assertThatThrownBy(() -> repository.saveAndFlush(negativo))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Altura da vegetação não pode ser negativa.");
    }

    @Test
    void bancoRecusaKmFinalMenorQueOInicial() {
        TrechoRodovia invertido = new TrechoRodovia("SP-021", 90.0, 80.0, EspecieGrama.CYNODON,
                1.0, hoje, null);

        assertThatThrownBy(() -> repository.saveAndFlush(invertido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
