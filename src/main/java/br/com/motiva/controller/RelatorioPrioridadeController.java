package br.com.motiva.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.service.RelatorioPrioridadeService;

/**
 * O relatorio como servico: o POST nao recebe corpo, porque quem decide os
 * numeros e o motor de prioridade, nao o cliente.
 */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioPrioridadeController {

    private final RelatorioPrioridadeService service;

    public RelatorioPrioridadeController(RelatorioPrioridadeService service) {
        this.service = service;
    }

    /** POST /api/relatorios: classifica a malha de hoje e grava. */
    @PostMapping
    public ResponseEntity<RelatorioPrioridade> gerar() {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.gerar());
    }

    /** GET /api/relatorios: historico, do mais recente para o mais antigo. */
    @GetMapping
    public ResponseEntity<List<RelatorioPrioridade>> historico() {
        return ResponseEntity.ok(service.historico());
    }

    /** GET /api/relatorios/periodo?inicio=2026-10-01&fim=2026-10-19 */
    @GetMapping("/periodo")
    public ResponseEntity<List<RelatorioPrioridade>> porPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(service.porPeriodo(inicio, fim));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RelatorioPrioridade> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
}
