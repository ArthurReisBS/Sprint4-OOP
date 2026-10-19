package br.com.motiva.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.RelatorioPrioridadeRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;

/**
 * O GeradorRelatorio da Sprint 3 sem o console: classifica todos os trechos,
 * monta o resumo e grava os contadores.
 */
@Service
public class RelatorioPrioridadeService {

    private final RelatorioPrioridadeRepository repository;
    private final TrechoRodoviaRepository trechoRepository;
    private final PrioridadeService prioridadeService;

    public RelatorioPrioridadeService(RelatorioPrioridadeRepository repository,
                                      TrechoRodoviaRepository trechoRepository,
                                      PrioridadeService prioridadeService) {
        this.repository = repository;
        this.trechoRepository = trechoRepository;
        this.prioridadeService = prioridadeService;
    }

    /** POST /api/relatorios: classifica a malha de hoje e grava o resultado. */
    @Transactional
    public RelatorioPrioridade gerar() {
        Map<Prioridade, List<TrechoRodovia>> porPrioridade =
                prioridadeService.agruparPorPrioridade(trechoRepository.findAll());

        RelatorioPrioridade relatorio = new RelatorioPrioridade(
                porPrioridade.get(Prioridade.URGENTE).size(),
                porPrioridade.get(Prioridade.CRITICO).size(),
                porPrioridade.get(Prioridade.ATENCAO).size(),
                porPrioridade.get(Prioridade.NORMAL).size(),
                montarResumo(porPrioridade));
        return repository.save(relatorio);
    }

    /** GET /api/relatorios: do mais recente para o mais antigo. */
    public List<RelatorioPrioridade> historico() {
        return repository.findAllByOrderByDataGeracaoDesc();
    }

    public RelatorioPrioridade buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatório", id));
    }

    /**
     * GET /api/relatorios/periodo. As datas sao dias inteiros: o fim vai ate
     * 23:59:59 dele, senao um relatorio gerado na tarde do ultimo dia ficaria
     * de fora.
     */
    public List<RelatorioPrioridade> porPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Início e fim do período são obrigatórios.");
        }
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("O fim do período (" + fim
                    + ") vem antes do início (" + inicio + ").");
        }
        return repository.findByDataGeracaoBetweenOrderByDataGeracaoAsc(
                inicio.atStartOfDay(), fim.atTime(LocalTime.MAX));
    }

    /**
     * Texto da coluna RESUMO.
     *
     * Nomeia os trechos urgentes em vez de so conta-los, para o resumo ser
     * legivel sozinho no historico.
     */
    private String montarResumo(Map<Prioridade, List<TrechoRodovia>> porPrioridade) {
        List<TrechoRodovia> urgentes = porPrioridade.get(Prioridade.URGENTE);
        int total = porPrioridade.values().stream().mapToInt(List::size).sum();

        String resumo;
        if (urgentes.isEmpty()) {
            int criticos = porPrioridade.get(Prioridade.CRITICO).size();
            resumo = criticos == 0
                    ? "Malha sob controle: nenhum trecho urgente ou crítico entre os "
                      + total + " monitorados."
                    : "Sem trechos urgentes. " + criticos + " em estado crítico, "
                      + "agendar para esta semana.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(urgentes.size()).append(" de ").append(total)
              .append(" trechos em estado urgente: ");
            for (int i = 0; i < urgentes.size(); i++) {
                if (i > 0) {
                    sb.append("; ");
                }
                TrechoRodovia t = urgentes.get(i);
                sb.append(String.format("%s (%.0f cm)",
                        t.identificacao(), prioridadeService.alturaEstimadaCm(t)));
            }
            sb.append(". Intervenção imediata recomendada.");
            resumo = sb.toString();
        }

        // a coluna RESUMO e VARCHAR2(500)
        return resumo.length() > RelatorioPrioridade.TAMANHO_MAXIMO_RESUMO
                ? resumo.substring(0, RelatorioPrioridade.TAMANHO_MAXIMO_RESUMO)
                : resumo;
    }
}
