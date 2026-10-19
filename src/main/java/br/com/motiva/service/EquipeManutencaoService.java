package br.com.motiva.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.TipoEquipe;
import br.com.motiva.repository.EquipeManutencaoRepository;
import br.com.motiva.repository.IntervencaoOperacionalRepository;

@Service
public class EquipeManutencaoService {

    private final EquipeManutencaoRepository repository;
    private final IntervencaoOperacionalRepository intervencaoRepository;

    public EquipeManutencaoService(EquipeManutencaoRepository repository,
                                   IntervencaoOperacionalRepository intervencaoRepository) {
        this.repository = repository;
        this.intervencaoRepository = intervencaoRepository;
    }

    public List<EquipeManutencao> listar() {
        return repository.findAll();
    }

    public EquipeManutencao buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe", id));
    }

    public List<EquipeManutencao> buscarPorTipo(TipoEquipe tipo) {
        return repository.findByTipo(tipo);
    }

    public List<EquipeManutencao> listarDisponiveis() {
        return repository.findByDisponivelTrue();
    }

    @Transactional
    public EquipeManutencao criar(EquipeManutencao equipe) {
        // a tabela usa GENERATED ALWAYS AS IDENTITY: um ID vindo do cliente
        // faria o Oracle recusar o INSERT
        equipe.setId(null);
        validar(equipe);
        return repository.save(equipe);
    }

    @Transactional
    public EquipeManutencao atualizar(Long id, EquipeManutencao dados) {
        EquipeManutencao existente = buscarPorId(id);
        validar(dados);

        existente.setNome(dados.getNome());
        existente.setTipo(dados.getTipo());
        existente.setQuantidadeIntegrantes(dados.getQuantidadeIntegrantes());
        existente.setDisponivel(dados.isDisponivel());
        return repository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        EquipeManutencao equipe = buscarPorId(id);
        if (intervencaoRepository.existsByEquipeId(id)) {
            throw new RegraNegocioException(
                    "Equipe " + id + " tem intervenções no histórico e não pode ser excluída.");
        }
        repository.delete(equipe);
    }

    /** Validacoes que na Sprint 3 ficavam no construtor do record. */
    private void validar(EquipeManutencao equipe) {
        if (equipe.getNome() == null || equipe.getNome().isBlank()) {
            throw new RegraNegocioException("Nome da equipe é obrigatório.");
        }
        if (equipe.getNome().length() > 80) {
            throw new RegraNegocioException("Nome da equipe passa de 80 caracteres.");
        }
        if (equipe.getTipo() == null) {
            throw new RegraNegocioException("Tipo da equipe é obrigatório.");
        }
        int integrantes = equipe.getQuantidadeIntegrantes();
        if (integrantes <= 0 || integrantes > 999) {
            throw new RegraNegocioException(
                    "Equipe precisa de 1 a 999 integrantes, veio " + integrantes + ".");
        }
    }
}
