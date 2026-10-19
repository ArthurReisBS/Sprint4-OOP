package br.com.motiva.controller;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.motiva.dto.ErroResponse;
import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Traduz as excecoes em status HTTP num lugar so, para os Controllers nao
 * repetirem try/catch em cada endpoint.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e,
                                                      HttpServletRequest request) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e,
                                                     HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /**
     * O @Valid de um @RequestBody falhou. Junta a mensagem de todos os campos
     * invalidos, e nao so do primeiro, em ordem de campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(MethodArgumentNotValidException e,
                                                      HttpServletRequest request) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(FieldError::getDefaultMessage))
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return erro(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    /**
     * Uma entidade invalida chegou ao Hibernate, que confere as mesmas
     * anotacoes antes de cada INSERT e UPDATE. Os Services validam antes,
     * entao isto so acontece se alguem gravar sem passar por eles.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> entidadeInvalida(ConstraintViolationException e,
                                                         HttpServletRequest request) {
        String mensagem = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .sorted()
                .collect(Collectors.joining(" "));
        return erro(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    /** JSON malformado, enum com valor que nao existe, data em formato errado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e,
                                                      HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado.", request);
    }

    /** /api/trechos/abc, ?inicio=ontem, /prioridade/AZUL. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException e,
                                                          HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST,
                "Valor inválido para '" + e.getName() + "': " + e.getValue(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException e,
                                                         HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST,
                "Parâmetro obrigatório ausente: " + e.getParameterName(), request);
    }

    /** O que escapar dos Services e esbarrar numa CHECK, FK ou UNIQUE do Oracle. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException e,
                                                    HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "Os dados violam uma restrição do banco.", request);
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String mensagem,
                                              HttpServletRequest request) {
        ErroResponse corpo = new ErroResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
