package br.com.motiva.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.motiva.model.IntervencaoOperacional;

/**
 * Um repositorio so para a hierarquia: as consultas devolvem RocadaMecanizada
 * e Pulverizacao misturadas, e o Hibernate escolhe a subclasse pela coluna TIPO.
 */
@Repository
public interface IntervencaoOperacionalRepository
        extends JpaRepository<IntervencaoOperacional, Long> {

    // WHERE trecho_id = ? ORDER BY data_execucao  (usa o ix_interv_trecho_data)
    List<IntervencaoOperacional> findByTrechoIdOrderByDataExecucaoAsc(Long trechoId);

    // WHERE equipe_id = ?
    List<IntervencaoOperacional> findByEquipeId(Long equipeId);

    // WHERE data_execucao BETWEEN ? AND ?
    List<IntervencaoOperacional> findByDataExecucaoBetween(LocalDate inicio, LocalDate fim);

    // WHERE custo >= ?
    List<IntervencaoOperacional> findByCustoGreaterThanEqual(double custoMinimo);

    // o DELETE de trecho e de equipe esbarra na FK se houver historico
    boolean existsByTrechoId(Long trechoId);

    boolean existsByEquipeId(Long equipeId);
}
