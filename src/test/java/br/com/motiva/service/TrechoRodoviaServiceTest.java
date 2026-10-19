package br.com.motiva.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
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
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.IntervencaoOperacionalRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;

@ExtendWith(MockitoExtension.class)
class TrechoRodoviaServiceTest {

    @Mock
    private TrechoRodoviaRepository repository;

    @Mock
    private IntervencaoOperacionalRepository intervencaoRepository;

    private TrechoRodoviaService service;

    @BeforeEach
    void setUp() {
        service = new TrechoRodoviaService(repository, intervencaoRepository, new PrioridadeService(),
                ValidacaoDeTeste.criar());
    }

    private static TrechoRodovia trechoValido() {
        return new TrechoRodovia("SP-021", 12.0, 18.5, EspecieGrama.PASPALUM, 22.0,
                LocalDate.now().minusDays(8), null);
    }

    @Test
    void criarDescartaOIdVindoDoClienteESalva() {
        TrechoRodovia novo = trechoValido();
        novo.setId(99L);
        when(repository.save(novo)).thenReturn(novo);

        service.criar(novo);

        assertThat(novo.getId()).isNull();
        verify(repository).save(novo);
    }

    @Test
    void criarRecusaFaixaRepetida() {
        when(repository.existsByRodoviaAndKmInicialAndKmFinal("SP-021", 12.0, 18.5)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(trechoValido()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe");
        verify(repository, never()).save(any());
    }

    @Test
    void criarRecusaAlturaNegativa() {
        TrechoRodovia trecho = trechoValido();
        trecho.setAlturaVegetacaoCm(-1.0);

        assertThatThrownBy(() -> service.criar(trecho))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("negativa");
        verify(repository, never()).save(any());
    }

    @Test
    void criarRecusaKmFinalMenorOuIgualAoInicial() {
        TrechoRodovia trecho = trechoValido();
        trecho.setKmFinal(12.0);

        assertThatThrownBy(() -> service.criar(trecho))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("km final");
    }

    @Test
    void criarRecusaRodoviaEmBranco() {
        TrechoRodovia trecho = trechoValido();
        trecho.setRodovia("  ");

        assertThatThrownBy(() -> service.criar(trecho)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void criarRecusaEspecieOuMedicaoAusentes() {
        TrechoRodovia semEspecie = trechoValido();
        semEspecie.setEspeciePredominante(null);
        TrechoRodovia semMedicao = trechoValido();
        semMedicao.setDataUltimaMedicao(null);

        assertThatThrownBy(() -> service.criar(semEspecie)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.criar(semMedicao)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void criarRecusaMedicaoNoFuturo() {
        TrechoRodovia trecho = trechoValido();
        trecho.setDataUltimaMedicao(LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> service.criar(trecho))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("futuro");
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarTambemValidaAsAnotacoes() {
        TrechoRodovia dados = trechoValido();
        dados.setAlturaVegetacaoCm(-2.0);
        when(repository.findById(1L)).thenReturn(Optional.of(trechoValido()));

        assertThatThrownBy(() -> service.atualizar(1L, dados))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("negativa");
        verify(repository, never()).save(any());
    }

    @Test
    void buscarIdInexistenteLancaNaoEncontrado() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(42L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("42");
    }

    @Test
    void atualizarCopiaOsCamposSemConsultarDuplicidadeSeAFaixaNaoMudou() {
        TrechoRodovia existente = trechoValido();
        existente.setId(1L);
        TrechoRodovia dados = trechoValido();
        dados.setAlturaVegetacaoCm(5.0);
        dados.setEspeciePredominante(EspecieGrama.CYNODON);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        TrechoRodovia salvo = service.atualizar(1L, dados);

        assertThat(salvo.getId()).isEqualTo(1L);
        assertThat(salvo.getAlturaVegetacaoCm()).isEqualTo(5.0);
        assertThat(salvo.getEspeciePredominante()).isEqualTo(EspecieGrama.CYNODON);
        verify(repository, never()).existsByRodoviaAndKmInicialAndKmFinal(anyString(), anyDouble(), anyDouble());
    }

    @Test
    void atualizarRecusaMudarParaFaixaDeOutroTrecho() {
        TrechoRodovia existente = trechoValido();
        TrechoRodovia dados = trechoValido();
        dados.setKmInicial(32.0);
        dados.setKmFinal(38.4);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByRodoviaAndKmInicialAndKmFinal("SP-021", 32.0, 38.4)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, dados)).isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void excluirRecusaTrechoComHistorico() {
        when(repository.findById(1L)).thenReturn(Optional.of(trechoValido()));
        when(intervencaoRepository.existsByTrechoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("histórico");
        verify(repository, never()).delete(any());
    }

    @Test
    void excluirTrechoSemHistorico() {
        TrechoRodovia trecho = trechoValido();
        when(repository.findById(1L)).thenReturn(Optional.of(trecho));
        when(intervencaoRepository.existsByTrechoId(1L)).thenReturn(false);

        service.excluir(1L);

        verify(repository).delete(trecho);
    }
}
