package br.com.motiva.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.motiva.model.RelatorioPrioridade;

@Repository
public interface RelatorioPrioridadeRepository extends JpaRepository<RelatorioPrioridade, Long> {

    // GET /api/relatorios: historico, do mais recente para o mais antigo
    List<RelatorioPrioridade> findAllByOrderByDataGeracaoDesc();

    // GET /api/relatorios/periodo: WHERE data_geracao BETWEEN ? AND ?
    List<RelatorioPrioridade> findByDataGeracaoBetweenOrderByDataGeracaoAsc(
            LocalDateTime inicio, LocalDateTime fim);

    // WHERE qt_urgente >= ?
    List<RelatorioPrioridade> findByQtUrgenteGreaterThanEqual(int minimo);
}
