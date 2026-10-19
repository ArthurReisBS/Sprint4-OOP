package br.com.motiva.service;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.motiva.exception.RegraNegocioException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Confere as anotacoes de Bean Validation de um objeto dentro do Service.
 *
 * O @Valid do Controller so protege quem chega pela API; chamando este
 * validador, os Services aplicam as mesmas regras a qualquer chamada, e as
 * anotacoes continuam sendo o unico lugar onde essas regras estao escritas.
 */
@Component
public class Validacao {

    private final Validator validator;

    public Validacao(Validator validator) {
        this.validator = validator;
    }

    /** Lanca RegraNegocioException com todas as violacoes, em ordem de campo. */
    public <T> void validar(T objeto) {
        Set<ConstraintViolation<T>> violacoes = validator.validate(objeto);
        if (violacoes.isEmpty()) {
            return;
        }
        String mensagem = violacoes.stream()
                .sorted(Comparator.comparing((ConstraintViolation<T> v) -> v.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(" "));
        throw new RegraNegocioException(mensagem);
    }
}
