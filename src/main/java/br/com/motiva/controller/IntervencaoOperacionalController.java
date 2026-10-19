package br.com.motiva.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import jakarta.validation.Valid;

import br.com.motiva.dto.IntervencaoRequest;
import br.com.motiva.dto.IntervencaoResponse;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.service.IntervencaoOperacionalService;

/**
 * Entra e sai por DTO: a entidade e abstrata e carrega trecho e equipe
 * inteiros, nenhum dos dois serve direto como JSON.
 */
@RestController
@RequestMapping("/api/intervencoes")
public class IntervencaoOperacionalController {

    private final IntervencaoOperacionalService service;

    public IntervencaoOperacionalController(IntervencaoOperacionalService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<IntervencaoResponse>> listar() {
        return ResponseEntity.ok(paraResposta(service.listar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IntervencaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(IntervencaoResponse.de(service.buscarPorId(id)));
    }

    /** Historico do trecho em ordem de data. */
    @GetMapping("/trecho/{trechoId}")
    public ResponseEntity<List<IntervencaoResponse>> historicoDoTrecho(@PathVariable Long trechoId) {
        return ResponseEntity.ok(paraResposta(service.historicoDoTrecho(trechoId)));
    }

    @GetMapping("/equipe/{equipeId}")
    public ResponseEntity<List<IntervencaoResponse>> buscarPorEquipe(@PathVariable Long equipeId) {
        return ResponseEntity.ok(paraResposta(service.buscarPorEquipe(equipeId)));
    }

    /** GET /api/intervencoes/periodo?inicio=2026-09-01&fim=2026-10-19 */
    @GetMapping("/periodo")
    public ResponseEntity<List<IntervencaoResponse>> buscarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(paraResposta(service.buscarPorPeriodo(inicio, fim)));
    }

    @PostMapping
    public ResponseEntity<IntervencaoResponse> criar(@Valid @RequestBody IntervencaoRequest request) {
        IntervencaoOperacional salva =
                service.criar(request.paraEntidade(), request.trechoId(), request.equipeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(IntervencaoResponse.de(salva));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IntervencaoResponse> atualizar(@PathVariable Long id,
                                                         @Valid @RequestBody IntervencaoRequest request) {
        IntervencaoOperacional salva =
                service.atualizar(id, request.paraEntidade(), request.trechoId(), request.equipeId());
        return ResponseEntity.ok(IntervencaoResponse.de(salva));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private static List<IntervencaoResponse> paraResposta(List<IntervencaoOperacional> intervencoes) {
        return intervencoes.stream().map(IntervencaoResponse::de).toList();
    }
}
