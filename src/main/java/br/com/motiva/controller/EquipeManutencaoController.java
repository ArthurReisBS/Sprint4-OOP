package br.com.motiva.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.TipoEquipe;
import br.com.motiva.service.EquipeManutencaoService;

@RestController
@RequestMapping("/api/equipes")
public class EquipeManutencaoController {

    private final EquipeManutencaoService service;

    public EquipeManutencaoController(EquipeManutencaoService service) {
        this.service = service;
    }

    /** GET /api/equipes, ou /api/equipes?tipo=ROCADA. */
    @GetMapping
    public ResponseEntity<List<EquipeManutencao>> listar(@RequestParam(required = false) TipoEquipe tipo) {
        return ResponseEntity.ok(tipo == null ? service.listar() : service.buscarPorTipo(tipo));
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<EquipeManutencao>> listarDisponiveis() {
        return ResponseEntity.ok(service.listarDisponiveis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipeManutencao> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<EquipeManutencao> criar(@RequestBody EquipeManutencao equipe) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(equipe));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipeManutencao> atualizar(@PathVariable Long id,
                                                      @RequestBody EquipeManutencao equipe) {
        return ResponseEntity.ok(service.atualizar(id, equipe));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
