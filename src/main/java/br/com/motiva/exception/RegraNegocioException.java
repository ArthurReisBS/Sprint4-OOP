package br.com.motiva.exception;

/**
 * Dado invalido ou operacao que fere uma regra do MOTIVA. O Controller
 * traduz para 400.
 *
 * Unchecked para que o @Transactional desfaca a transacao ao ser lancada.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
