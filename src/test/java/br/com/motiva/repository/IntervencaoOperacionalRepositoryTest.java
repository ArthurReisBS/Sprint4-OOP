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

import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.Pulverizacao;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TipoEquipe;
import br.com.motiva.model.TrechoRodovia;
import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class IntervencaoOperacionalRepositoryTest {

    @Autowired
    private IntervencaoOperacionalRepository repository;

    @Autowired
    private TrechoRodoviaRepository trechoRepository;

    @Autowired
    private EquipeManutencaoRepository equipeRepository;

    @Autowired
    private EntityManager entityManager;

    private final LocalDate hoje = LocalDate.now();

    private TrechoRodovia km12;
    private TrechoRodovia km112;
    private EquipeManutencao alfa;
    private EquipeManutencao charlie;

    @BeforeEach
    void popular() {
        km12 = trechoRepository.save(new TrechoRodovia("SP-021", 12.0, 18.5,
                EspecieGrama.PASPALUM, 22.0, hoje.minusDays(8), hoje.minusDays(34)));
        km112 = trechoRepository.save(new TrechoRodovia("SP-021", 112.0, 118.0,
                EspecieGrama.CYNODON, 6.0, hoje.minusDays(4), hoje.minusDays(12)));
        alfa = equipeRepository.save(new EquipeManutencao("Equipe Alfa", TipoEquipe.ROCADA, 6, true));
        charlie = equipeRepository.save(
                new EquipeManutencao("Equipe Charlie", TipoEquipe.PULVERIZACAO, 4, true));

        repository.save(new RocadaMecanizada(km12, alfa, hoje.minusDays(34), 4850.0, 3.5, 10.0));
        // duas no km 112, salvas fora de ordem de data
        repository.save(new RocadaMecanizada(km112, alfa, hoje.minusDays(12), 3250.0, 2.5, 8.0));
        repository.save(new RocadaMecanizada(km112, alfa, hoje.minusDays(45), 3100.0, 2.5, 8.0));
        repository.save(new Pulverizacao(km12, charlie, hoje.minusDays(40), 3200.0));

        // esvazia o cache do Hibernate: as leituras abaixo vao ao banco de verdade
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void colunaTipoDevolveASubclasseCerta() {
        assertThat(repository.findByEquipeId(charlie.getId()))
                .singleElement()
                .isInstanceOf(Pulverizacao.class);
        assertThat(repository.findByEquipeId(alfa.getId()))
                .hasSize(3)
                .allSatisfy(i -> assertThat(i).isInstanceOf(RocadaMecanizada.class));
    }

    @Test
    void rocadaVoltaComAsColunasProprias() {
        RocadaMecanizada rocada = (RocadaMecanizada) repository
                .findByTrechoIdOrderByDataExecucaoAsc(km12.getId()).get(1);

        assertThat(rocada.getLarguraFaixaM()).isEqualTo(3.5);
        assertThat(rocada.getAlturaCorteCm()).isEqualTo(10.0);
    }

    @Test
    void historicoDoTrechoEmOrdemDeData() {
        assertThat(repository.findByTrechoIdOrderByDataExecucaoAsc(km112.getId()))
                .extracting(IntervencaoOperacional::getDataExecucao)
                .containsExactly(hoje.minusDays(45), hoje.minusDays(12));
    }

    @Test
    void findByDataExecucaoBetweenIncluiAsDuasPontas() {
        assertThat(repository.findByDataExecucaoBetween(hoje.minusDays(40), hoje.minusDays(12)))
                .extracting(IntervencaoOperacional::getDataExecucao)
                .containsExactlyInAnyOrder(hoje.minusDays(40), hoje.minusDays(34), hoje.minusDays(12));
    }

    @Test
    void findByCustoGreaterThanEqual() {
        assertThat(repository.findByCustoGreaterThanEqual(3250.0))
                .extracting(IntervencaoOperacional::getCusto)
                .containsExactlyInAnyOrder(4850.0, 3250.0);
    }

    @Test
    void existsPorTrechoEPorEquipe() {
        TrechoRodovia semHistorico = trechoRepository.saveAndFlush(new TrechoRodovia("SP-280", 1.0, 2.0,
                EspecieGrama.CYNODON, 1.0, hoje, null));

        assertThat(repository.existsByTrechoId(km12.getId())).isTrue();
        assertThat(repository.existsByTrechoId(semHistorico.getId())).isFalse();
        assertThat(repository.existsByEquipeId(charlie.getId())).isTrue();
    }

    @Test
    void bancoRecusaRocadaSemAsColunasProprias() {
        // o CHECK ck_interv_subtipo pega o que o Service deixasse passar
        RocadaMecanizada incompleta = new RocadaMecanizada(km12, alfa, hoje, 100.0, 1.0, 1.0);
        incompleta.setLarguraFaixaM(null);

        assertThatThrownBy(() -> repository.saveAndFlush(incompleta))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
