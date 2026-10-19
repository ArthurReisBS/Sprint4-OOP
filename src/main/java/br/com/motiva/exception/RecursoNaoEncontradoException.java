package br.com.motiva.exception;

/**
 * O ID pedido nao existe no banco. O Controller traduz para 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Long id) {
        super(recurso + " " + id + " não encontrado(a).");
    }
}
