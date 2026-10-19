package br.com.motiva.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.IntervencaoOperacionalRepository;

@Service
public class IntervencaoOperacionalService {

    private final IntervencaoOperacionalRepository repository;
    private final TrechoRodoviaService trechoService;
    private final EquipeManutencaoService equipeService;
    private final Validacao validacao;

    public IntervencaoOperacionalService(IntervencaoOperacionalRepository repository,
                                         TrechoRodoviaService trechoService,
                                         EquipeManutencaoService equipeService,
                                         Validacao validacao) {
        this.repository = repository;
        this.trechoService = trechoService;
        this.equipeService = equipeService;
        this.validacao = validacao;
    }

    public List<IntervencaoOperacional> listar() {
        return repository.findAll();
    }

    public IntervencaoOperacional buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Intervenção", id));
    }

    /** Historico do trecho em ordem de data. */
    public List<IntervencaoOperacional> historicoDoTrecho(Long trechoId) {
        trechoService.buscarPorId(trechoId);
        return repository.findByTrechoIdOrderByDataExecucaoAsc(trechoId);
    }

    public List<IntervencaoOperacional> buscarPorEquipe(Long equipeId) {
        equipeService.buscarPorId(equipeId);
        return repository.findByEquipeId(equipeId);
    }

    public List<IntervencaoOperacional> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        return repository.findByDataExecucaoBetween(inicio, fim);
    }

    /**
     * Registra uma intervencao. Trecho e equipe chegam pelo ID e sao
     * carregados aqui: 404 se nao existirem.
     */
    @Transactional
    public IntervencaoOperacional criar(IntervencaoOperacional intervencao,
                                        Long trechoId, Long equipeId) {
        // a tabela usa GENERATED ALWAYS AS IDENTITY: um ID vindo do cliente
        // faria o Oracle recusar o INSERT
        intervencao.setId(null);
        vincular(intervencao, trechoId, equipeId);
        validar(intervencao);
        return repository.save(intervencao);
    }

    /**
     * Atualiza os dados de uma intervencao. O tipo nao muda: trocar roçada por
     * pulverizacao e outra intervencao, e nao uma correcao desta.
     */
    @Transactional
    public IntervencaoOperacional atualizar(Long id, IntervencaoOperacional dados,
                                            Long trechoId, Long equipeId) {
        IntervencaoOperacional existente = buscarPorId(id);
        if (existente.getTipo() != dados.getTipo()) {
            throw new RegraNegocioException("A intervenção " + id + " é " + existente.getTipo()
                    + " e não pode virar " + dados.getTipo() + ".");
        }

        vincular(existente, trechoId, equipeId);
        existente.setDataExecucao(dados.getDataExecucao());
        existente.setCusto(dados.getCusto());
        if (existente instanceof RocadaMecanizada rocada
                && dados instanceof RocadaMecanizada novos) {
            rocada.setLarguraFaixaM(novos.getLarguraFaixaM());
            rocada.setAlturaCorteCm(novos.getAlturaCorteCm());
        }

        validar(existente);
        return repository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarPorId(id));
    }

    private void vincular(IntervencaoOperacional intervencao, Long trechoId, Long equipeId) {
        if (trechoId == null || equipeId == null) {
            throw new RegraNegocioException("Trecho e equipe são obrigatórios.");
        }
        TrechoRodovia trecho = trechoService.buscarPorId(trechoId);
        EquipeManutencao equipe = equipeService.buscarPorId(equipeId);
        intervencao.setTrecho(trecho);
        intervencao.setEquipe(equipe);
    }

    /**
     * Validacoes que na Sprint 3 ficavam no construtor dos records. As de um
     * campo so (data, custo) estao anotadas na entidade; aqui ficam as que
     * dependem da equipe carregada e do tipo da intervencao.
     */
    private void validar(IntervencaoOperacional intervencao) {
        validacao.validar(intervencao);

        EquipeManutencao equipe = intervencao.getEquipe();
        if (!equipe.getTipo().podeExecutar(intervencao.getTipo())) {
            throw new RegraNegocioException("A equipe " + equipe.getNome() + " é de "
                    + equipe.getTipo() + " e não executa " + intervencao.getTipo() + ".");
        }

        if (intervencao instanceof RocadaMecanizada rocada) {
            // o @Positive aceita nulo, porque a coluna e da pulverizacao tambem
            if (rocada.getLarguraFaixaM() == null || rocada.getAlturaCorteCm() == null) {
                throw new RegraNegocioException(
                        "Roçada exige largura da faixa e altura de corte positivas.");
            }
        }
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Início e fim do período são obrigatórios.");
        }
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("O fim do período (" + fim
                    + ") vem antes do início (" + inicio + ").");
        }
    }
}
