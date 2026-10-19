package br.com.motiva.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import br.com.motiva.model.RelatorioPrioridade;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RelatorioPrioridadeRepositoryTest {

    @Autowired
    private RelatorioPrioridadeRepository repository;

    private final LocalDateTime agora = LocalDateTime.now().withNano(0);

    private RelatorioPrioridade relatorio(LocalDateTime geradoEm, int urgentes) {
        RelatorioPrioridade r = new RelatorioPrioridade(urgentes, 0, 0, 0, "gerado em " + geradoEm);
        r.setDataGeracao(geradoEm);
        return r;
    }

    @BeforeEach
    void popular() {
        repository.save(relatorio(agora.minusDays(14), 0));
        repository.save(relatorio(agora.minusDays(1), 3));
        repository.save(relatorio(agora.minusDays(7), 1));
        repository.flush();
    }

    @Test
    void prePersistPreencheADataDeGeracao() {
        RelatorioPrioridade salvo =
                repository.saveAndFlush(new RelatorioPrioridade(1, 2, 3, 4, "sem data"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getDataGeracao()).isNotNull();
    }

    @Test
    void historicoDoMaisRecenteParaOMaisAntigo() {
        assertThat(repository.findAllByOrderByDataGeracaoDesc())
                .extracting(RelatorioPrioridade::getDataGeracao)
                .containsExactly(agora.minusDays(1), agora.minusDays(7), agora.minusDays(14));
    }

    @Test
    void periodoEmOrdemCronologica() {
        assertThat(repository.findByDataGeracaoBetweenOrderByDataGeracaoAsc(
                agora.minusDays(8), agora))
                .extracting(RelatorioPrioridade::getDataGeracao)
                .containsExactly(agora.minusDays(7), agora.minusDays(1));
    }

    @Test
    void findByQtUrgenteGreaterThanEqual() {
        assertThat(repository.findByQtUrgenteGreaterThanEqual(1))
                .extracting(RelatorioPrioridade::getQtUrgente)
                .containsExactlyInAnyOrder(1, 3);
    }
}
