package br.com.motiva.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.RelatorioPrioridadeRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;

@ExtendWith(MockitoExtension.class)
class RelatorioPrioridadeServiceTest {

    @Mock
    private RelatorioPrioridadeRepository repository;

    @Mock
    private TrechoRodoviaRepository trechoRepository;

    private RelatorioPrioridadeService service;

    @BeforeEach
    void setUp() {
        // o motor e o de verdade: o relatorio depende da classificacao correta
        service = new RelatorioPrioridadeService(repository, trechoRepository, new PrioridadeService());
    }

    private static TrechoRodovia trecho(double km, EspecieGrama especie, double alturaCm, int dias) {
        return new TrechoRodovia("SP-021", km, km + 5, especie, alturaCm,
                LocalDate.now().minusDays(dias), null);
    }

    // os 9 trechos do seu-script-dados.sql: 2 urgentes, 2 criticos, 2 em atencao, 3 normais
    private static List<TrechoRodovia> malhaDaSprint3() {
        return List.of(
                trecho(12, EspecieGrama.PASPALUM, 22.0, 8),
                trecho(32, EspecieGrama.MEGATHYRSUS, 5.0, 3),
                trecho(47, EspecieGrama.PASPALUM, 20.0, 6),
                trecho(61, EspecieGrama.CYNODON, 22.0, 3),
                trecho(79, EspecieGrama.PASPALUM, 14.0, 5),
                trecho(94, EspecieGrama.BRACHIARIA, 8.0, 3),
                trecho(112, EspecieGrama.CYNODON, 6.0, 4),
                trecho(128, EspecieGrama.PASPALUM, 5.0, 3),
                trecho(145, EspecieGrama.BRACHIARIA, 2.0, 2));
    }

    @Test
    void gerarContaCadaFaixaEGravaORelatorio() {
        when(trechoRepository.findAll()).thenReturn(malhaDaSprint3());
        when(repository.save(any(RelatorioPrioridade.class))).thenAnswer(inv -> inv.getArgument(0));

        RelatorioPrioridade relatorio = service.gerar();

        assertThat(relatorio.getQtUrgente()).isEqualTo(2);
        assertThat(relatorio.getQtCritico()).isEqualTo(2);
        assertThat(relatorio.getQtAtencao()).isEqualTo(2);
        assertThat(relatorio.getQtNormal()).isEqualTo(3);
        verify(repository).save(relatorio);
    }

    @Test
    void resumoNomeiaOsUrgentesDoMaisAltoParaOMaisBaixo() {
        when(trechoRepository.findAll()).thenReturn(malhaDaSprint3());
        when(repository.save(any(RelatorioPrioridade.class))).thenAnswer(inv -> inv.getArgument(0));

        String resumo = service.gerar().getResumo();

        assertThat(resumo).startsWith("2 de 9 trechos em estado urgente");
        // km 32 (33,5 cm) antes de km 12 (30,8 cm)
        assertThat(resumo.indexOf("km 32")).isLessThan(resumo.indexOf("km 12"));
        assertThat(resumo).endsWith("Intervenção imediata recomendada.");
    }

    @Test
    void semUrgentesNemCriticosOResumoDizQueAMalhaEstaSobControle() {
        when(trechoRepository.findAll()).thenReturn(List.of(trecho(112, EspecieGrama.CYNODON, 6.0, 4)));
        when(repository.save(any(RelatorioPrioridade.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.gerar().getResumo()).startsWith("Malha sob controle");
    }

    @Test
    void semUrgentesMasComCriticosOResumoPedeAgendamento() {
        when(trechoRepository.findAll()).thenReturn(List.of(trecho(47, EspecieGrama.PASPALUM, 20.0, 6)));
        when(repository.save(any(RelatorioPrioridade.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.gerar().getResumo()).startsWith("Sem trechos urgentes. 1 em estado crítico");
    }

    @Test
    void resumoNuncaPassaDoTamanhoDaColuna() {
        // 40 trechos urgentes nomeados estouram os 500 caracteres
        List<TrechoRodovia> muitos = java.util.stream.IntStream.range(0, 40)
                .mapToObj(i -> trecho(i * 10, EspecieGrama.PENNISETUM, 40.0, 0))
                .toList();
        when(trechoRepository.findAll()).thenReturn(muitos);
        when(repository.save(any(RelatorioPrioridade.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.gerar().getResumo()).hasSize(RelatorioPrioridade.TAMANHO_MAXIMO_RESUMO);
    }

    @Test
    void periodoVaiDoInicioDoPrimeiroDiaAoFimDoUltimo() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fim = LocalDate.of(2026, 10, 19);
        ArgumentCaptor<LocalDateTime> de = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> ate = ArgumentCaptor.forClass(LocalDateTime.class);

        service.porPeriodo(inicio, fim);

        verify(repository).findByDataGeracaoBetweenOrderByDataGeracaoAsc(de.capture(), ate.capture());
        assertThat(de.getValue()).isEqualTo(inicio.atStartOfDay());
        assertThat(ate.getValue()).isEqualTo(fim.atTime(LocalTime.MAX));
    }

    @Test
    void periodoInvalidoERecusado() {
        LocalDate hoje = LocalDate.now();

        assertThatThrownBy(() -> service.porPeriodo(hoje, hoje.minusDays(1)))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.porPeriodo(null, hoje))
                .isInstanceOf(RegraNegocioException.class);
    }
}
