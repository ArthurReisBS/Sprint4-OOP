package br.com.motiva.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EspecieGrama;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.IntervencaoOperacionalRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;

@Service
public class TrechoRodoviaService {

    private final TrechoRodoviaRepository repository;
    private final IntervencaoOperacionalRepository intervencaoRepository;
    private final PrioridadeService prioridadeService;

    public TrechoRodoviaService(TrechoRodoviaRepository repository,
                                IntervencaoOperacionalRepository intervencaoRepository,
                                PrioridadeService prioridadeService) {
        this.repository = repository;
        this.intervencaoRepository = intervencaoRepository;
        this.prioridadeService = prioridadeService;
    }

    public List<TrechoRodovia> listar() {
        return repository.findAll();
    }

    public TrechoRodovia buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trecho", id));
    }

    public List<TrechoRodovia> buscarPorRodovia(String rodovia) {
        return repository.findByRodoviaOrderByKmInicialAsc(rodovia);
    }

    public List<TrechoRodovia> buscarPorEspecie(EspecieGrama especie) {
        return repository.findByEspeciePredominante(especie);
    }

    public List<TrechoRodovia> listarNuncaIntervindos() {
        return repository.findByDataUltimaIntervencaoIsNull();
    }

    /** Trechos de uma faixa, pela altura estimada de hoje. */
    public List<TrechoRodovia> listarPorPrioridade(Prioridade prioridade) {
        return prioridadeService.agruparPorPrioridade(repository.findAll()).get(prioridade);
    }

    @Transactional
    public TrechoRodovia criar(TrechoRodovia trecho) {
        // a tabela usa GENERATED ALWAYS AS IDENTITY: um ID vindo do cliente
        // faria o Oracle recusar o INSERT
        trecho.setId(null);
        validar(trecho);
        if (repository.existsByRodoviaAndKmInicialAndKmFinal(
                trecho.getRodovia(), trecho.getKmInicial(), trecho.getKmFinal())) {
            throw new RegraNegocioException("Já existe um trecho " + trecho.identificacao() + ".");
        }
        return repository.save(trecho);
    }

    @Transactional
    public TrechoRodovia atualizar(Long id, TrechoRodovia dados) {
        TrechoRodovia existente = buscarPorId(id);
        validar(dados);

        boolean mudouFaixa = !existente.getRodovia().equals(dados.getRodovia())
                || existente.getKmInicial() != dados.getKmInicial()
                || existente.getKmFinal() != dados.getKmFinal();
        if (mudouFaixa && repository.existsByRodoviaAndKmInicialAndKmFinal(
                dados.getRodovia(), dados.getKmInicial(), dados.getKmFinal())) {
            throw new RegraNegocioException("Já existe um trecho " + dados.identificacao() + ".");
        }

        existente.setRodovia(dados.getRodovia());
        existente.setKmInicial(dados.getKmInicial());
        existente.setKmFinal(dados.getKmFinal());
        existente.setEspeciePredominante(dados.getEspeciePredominante());
        existente.setAlturaVegetacaoCm(dados.getAlturaVegetacaoCm());
        existente.setDataUltimaMedicao(dados.getDataUltimaMedicao());
        existente.setDataUltimaIntervencao(dados.getDataUltimaIntervencao());
        return repository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        TrechoRodovia trecho = buscarPorId(id);
        if (intervencaoRepository.existsByTrechoId(id)) {
            throw new RegraNegocioException(
                    "Trecho " + id + " tem intervenções no histórico e não pode ser excluído.");
        }
        repository.delete(trecho);
    }

    /**
     * Validacoes que na Sprint 3 ficavam no construtor do record.
     *
     * Ficam aqui, e nao no Controller, porque sao regra do dominio: valem para
     * qualquer porta de entrada, nao so para a requisicao HTTP.
     */
    private void validar(TrechoRodovia trecho) {
        if (trecho.getRodovia() == null || trecho.getRodovia().isBlank()) {
            throw new RegraNegocioException("Rodovia é obrigatória.");
        }
        if (trecho.getRodovia().length() > 20) {
            throw new RegraNegocioException("Rodovia passa de 20 caracteres.");
        }
        if (trecho.getEspeciePredominante() == null) {
            throw new RegraNegocioException("Espécie predominante é obrigatória.");
        }
        if (trecho.getKmFinal() <= trecho.getKmInicial()) {
            throw new RegraNegocioException("km final (" + trecho.getKmFinal()
                    + ") deve ser maior que o inicial (" + trecho.getKmInicial() + ").");
        }
        if (trecho.getAlturaVegetacaoCm() < 0) {
            throw new RegraNegocioException(
                    "Altura da vegetação não pode ser negativa: " + trecho.getAlturaVegetacaoCm());
        }
        if (trecho.getDataUltimaMedicao() == null) {
            throw new RegraNegocioException("Data da última medição é obrigatória.");
        }
    }
}
