package br.com.motiva.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.TipoEquipe;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class EquipeManutencaoRepositoryTest {

    @Autowired
    private EquipeManutencaoRepository repository;

    @BeforeEach
    void popular() {
        repository.save(new EquipeManutencao("Equipe Alfa", TipoEquipe.ROCADA, 6, true));
        repository.save(new EquipeManutencao("Equipe Bravo", TipoEquipe.ROCADA, 5, true));
        repository.save(new EquipeManutencao("Equipe Charlie", TipoEquipe.PULVERIZACAO, 4, true));
        repository.save(new EquipeManutencao("Equipe Delta", TipoEquipe.MISTA, 8, false));
        repository.flush();
    }

    @Test
    void bancoGeraOIdNoInsert() {
        EquipeManutencao salva =
                repository.saveAndFlush(new EquipeManutencao("Equipe Eco", TipoEquipe.MISTA, 3, true));

        assertThat(salva.getId()).isNotNull();
    }

    @Test
    void findByTipo() {
        assertThat(repository.findByTipo(TipoEquipe.ROCADA))
                .extracting(EquipeManutencao::getNome)
                .containsExactlyInAnyOrder("Equipe Alfa", "Equipe Bravo");
    }

    @Test
    void findByDisponivelTrueDeixaDeForaAIndisponivel() {
        assertThat(repository.findByDisponivelTrue())
                .extracting(EquipeManutencao::getNome)
                .doesNotContain("Equipe Delta")
                .hasSize(3);
    }

    @Test
    void findByQuantidadeIntegrantesGreaterThanEqualIncluiOLimite() {
        assertThat(repository.findByQuantidadeIntegrantesGreaterThanEqual(6))
                .extracting(EquipeManutencao::getNome)
                .containsExactlyInAnyOrder("Equipe Alfa", "Equipe Delta");
    }
}
