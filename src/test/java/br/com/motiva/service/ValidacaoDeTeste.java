package br.com.motiva.service;

import jakarta.validation.Validation;

/** O validador de verdade, para os testes de Service que nao sobem o Spring. */
final class ValidacaoDeTeste {

    private ValidacaoDeTeste() {
    }

    static Validacao criar() {
        return new Validacao(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
