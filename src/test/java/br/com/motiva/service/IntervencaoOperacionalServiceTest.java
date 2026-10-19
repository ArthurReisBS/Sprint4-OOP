package br.com.motiva.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.Pulverizacao;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TipoEquipe;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.IntervencaoOperacionalRepository;

@ExtendWith(MockitoExtension.class)
class IntervencaoOperacionalServiceTest {

    @Mock
    private IntervencaoOperacionalRepository repository;

    @Mock
    private TrechoRodoviaService trechoService;

    @Mock
    private EquipeManutencaoService equipeService;

    private IntervencaoOperacionalService service;

    private final TrechoRodovia trecho = new TrechoRodovia("SP-021", 12.0, 18.5,
            EspecieGrama.PASPALUM, 22.0, LocalDate.now(), null);

    @BeforeEach
    void setUp() {
        service = new IntervencaoOperacionalService(repository, trechoService, equipeService);
    }

    private void comEquipe(TipoEquipe tipo) {
        when(trechoService.buscarPorId(1L)).thenReturn(trecho);
        when(equipeService.buscarPorId(2L))
                .thenReturn(new EquipeManutencao("Equipe " + tipo, tipo, 5, true));
    }

    private static RocadaMecanizada rocada() {
        return new RocadaMecanizada(null, null, LocalDate.now(), 4850.0, 3.5, 10.0);
    }

    @Test
    void criarCarregaTrechoEEquipeEDescartaOId() {
        comEquipe(TipoEquipe.ROCADA);
        RocadaMecanizada nova = rocada();
        nova.setId(50L);
        when(repository.save(nova)).thenReturn(nova);

        IntervencaoOperacional salva = service.criar(nova, 1L, 2L);

        assertThat(salva.getId()).isNull();
        assertThat(salva.getTrecho()).isSameAs(trecho);
        assertThat(salva.getEquipe().getTipo()).isEqualTo(TipoEquipe.ROCADA);
    }

    @Test
    void equipeMistaPodeRegistrarPulverizacao() {
        comEquipe(TipoEquipe.MISTA);
        Pulverizacao nova = new Pulverizacao(null, null, LocalDate.now(), 3200.0);
        when(repository.save(nova)).thenReturn(nova);

        service.criar(nova, 1L, 2L);

        verify(repository).save(nova);
    }

    @Test
    void equipeDePulverizacaoNaoRegistraRocada() {
        comEquipe(TipoEquipe.PULVERIZACAO);

        assertThatThrownBy(() -> service.criar(rocada(), 1L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não executa");
        verify(repository, never()).save(any());
    }

    @Test
    void rocadaSemLarguraDeFaixaERecusada() {
        comEquipe(TipoEquipe.ROCADA);
        RocadaMecanizada semLargura = rocada();
        semLargura.setLarguraFaixaM(null);

        assertThatThrownBy(() -> service.criar(semLargura, 1L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("largura");
    }

    @Test
    void custoNegativoERecusado() {
        comEquipe(TipoEquipe.ROCADA);
        RocadaMecanizada cara = rocada();
        cara.setCusto(-1.0);

        assertThatThrownBy(() -> service.criar(cara, 1L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Custo");
    }

    @Test
    void criarSemTrechoOuEquipeERecusado() {
        assertThatThrownBy(() -> service.criar(rocada(), null, 2L))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void trechoInexistentePropagaNaoEncontrado() {
        when(trechoService.buscarPorId(1L)).thenThrow(new RecursoNaoEncontradoException("Trecho", 1L));

        assertThatThrownBy(() -> service.criar(rocada(), 1L, 2L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarNaoTrocaOTipoDaIntervencao() {
        when(repository.findById(10L)).thenReturn(Optional.of(rocada()));
        Pulverizacao dados = new Pulverizacao(null, null, LocalDate.now(), 3200.0);

        assertThatThrownBy(() -> service.atualizar(10L, dados, 1L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode virar");
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarRocadaCopiaAsColunasProprias() {
        comEquipe(TipoEquipe.ROCADA);
        RocadaMecanizada existente = rocada();
        RocadaMecanizada dados = new RocadaMecanizada(null, null, LocalDate.now(), 5000.0, 2.5, 8.0);
        when(repository.findById(10L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        RocadaMecanizada salva = (RocadaMecanizada) service.atualizar(10L, dados, 1L, 2L);

        assertThat(salva.getCusto()).isEqualTo(5000.0);
        assertThat(salva.getLarguraFaixaM()).isEqualTo(2.5);
        assertThat(salva.getAlturaCorteCm()).isEqualTo(8.0);
    }

    @Test
    void periodoComFimAntesDoInicioERecusado() {
        LocalDate hoje = LocalDate.now();

        assertThatThrownBy(() -> service.buscarPorPeriodo(hoje, hoje.minusDays(1)))
                .isInstanceOf(RegraNegocioException.class);
    }
}
