package br.com.motiva.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.TipoEquipe;

@Repository
public interface EquipeManutencaoRepository extends JpaRepository<EquipeManutencao, Long> {

    // WHERE tipo = ?
    List<EquipeManutencao> findByTipo(TipoEquipe tipo);

    // WHERE disponivel = 1
    List<EquipeManutencao> findByDisponivelTrue();

    // WHERE qt_integrantes >= ?
    List<EquipeManutencao> findByQuantidadeIntegrantesGreaterThanEqual(int minimo);
}
