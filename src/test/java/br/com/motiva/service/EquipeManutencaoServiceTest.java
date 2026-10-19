package br.com.motiva.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.TipoEquipe;
import br.com.motiva.repository.EquipeManutencaoRepository;
import br.com.motiva.repository.IntervencaoOperacionalRepository;

@ExtendWith(MockitoExtension.class)
class EquipeManutencaoServiceTest {

    @Mock
    private EquipeManutencaoRepository repository;

    @Mock
    private IntervencaoOperacionalRepository intervencaoRepository;

    private EquipeManutencaoService service;

    @BeforeEach
    void setUp() {
        service = new EquipeManutencaoService(repository, intervencaoRepository, ValidacaoDeTeste.criar());
    }

    private static EquipeManutencao equipeValida() {
        return new EquipeManutencao("Equipe Alfa", TipoEquipe.ROCADA, 6, true);
    }

    @Test
    void criarDescartaOIdVindoDoClienteESalva() {
        EquipeManutencao nova = equipeValida();
        nova.setId(7L);
        when(repository.save(nova)).thenReturn(nova);

        service.criar(nova);

        assertThat(nova.getId()).isNull();
        verify(repository).save(nova);
    }

    @Test
    void criarRecusaNomeEmBranco() {
        EquipeManutencao equipe = equipeValida();
        equipe.setNome("");

        assertThatThrownBy(() -> service.criar(equipe))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Nome");
        verify(repository, never()).save(any());
    }

    @Test
    void criarRecusaEquipeSemIntegrantes() {
        EquipeManutencao equipe = equipeValida();
        equipe.setQuantidadeIntegrantes(0);

        assertThatThrownBy(() -> service.criar(equipe))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("integrantes");
    }

    @Test
    void criarRecusaTipoAusente() {
        EquipeManutencao equipe = equipeValida();
        equipe.setTipo(null);

        assertThatThrownBy(() -> service.criar(equipe)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void atualizarIdInexistenteLancaNaoEncontrado() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(5L, equipeValida()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarCopiaOsCampos() {
        EquipeManutencao existente = equipeValida();
        existente.setId(1L);
        EquipeManutencao dados = new EquipeManutencao("Equipe Delta", TipoEquipe.MISTA, 8, false);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        EquipeManutencao salva = service.atualizar(1L, dados);

        assertThat(salva.getId()).isEqualTo(1L);
        assertThat(salva.getNome()).isEqualTo("Equipe Delta");
        assertThat(salva.getTipo()).isEqualTo(TipoEquipe.MISTA);
        assertThat(salva.getQuantidadeIntegrantes()).isEqualTo(8);
        assertThat(salva.isDisponivel()).isFalse();
    }

    @Test
    void excluirRecusaEquipeComHistorico() {
        when(repository.findById(1L)).thenReturn(Optional.of(equipeValida()));
        when(intervencaoRepository.existsByEquipeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void excluirEquipeSemHistorico() {
        EquipeManutencao equipe = equipeValida();
        when(repository.findById(1L)).thenReturn(Optional.of(equipe));
        when(intervencaoRepository.existsByEquipeId(1L)).thenReturn(false);

        service.excluir(1L);

        verify(repository).delete(equipe);
    }
}
