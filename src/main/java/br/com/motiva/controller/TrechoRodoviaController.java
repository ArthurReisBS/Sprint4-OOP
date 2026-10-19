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

import br.com.motiva.model.Prioridade;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.service.TrechoRodoviaService;

/**
 * So HTTP: recebe, delega ao Service e escolhe o status. Os erros viram 404 e
 * 400 no GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/trechos")
public class TrechoRodoviaController {

    private final TrechoRodoviaService service;

    public TrechoRodoviaController(TrechoRodoviaService service) {
        this.service = service;
    }

    /** GET /api/trechos, ou /api/trechos?rodovia=SP-021 ordenado por km. */
    @GetMapping
    public ResponseEntity<List<TrechoRodovia>> listar(@RequestParam(required = false) String rodovia) {
        return ResponseEntity.ok(rodovia == null ? service.listar() : service.buscarPorRodovia(rodovia));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrechoRodovia> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    /** GET /api/trechos/prioridade/URGENTE: pela altura estimada de hoje. */
    @GetMapping("/prioridade/{prioridade}")
    public ResponseEntity<List<TrechoRodovia>> listarPorPrioridade(@PathVariable Prioridade prioridade) {
        return ResponseEntity.ok(service.listarPorPrioridade(prioridade));
    }

    @PostMapping
    public ResponseEntity<TrechoRodovia> criar(@RequestBody TrechoRodovia trecho) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(trecho));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrechoRodovia> atualizar(@PathVariable Long id,
                                                   @RequestBody TrechoRodovia trecho) {
        return ResponseEntity.ok(service.atualizar(id, trecho));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
