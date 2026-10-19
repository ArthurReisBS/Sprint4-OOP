package br.com.motiva.model;

/**
 * Especies de grama das margens da rodovia, com taxa de crescimento e porte.
 *
 * A taxa e o crescimento em condicoes ideais, sem modulacao por clima: serve
 * como limite superior, nao como previsao.
 */
public enum EspecieGrama {

    BRACHIARIA  ("Brachiaria (Urochloa)",         4.2,  90.0),
    CYNODON     ("Cynodon (grama-seda)",          1.5,  45.0),
    MEGATHYRSUS ("Megathyrsus (capim-colonião)",  9.5, 180.0),
    PENNISETUM  ("Pennisetum (capim-elefante)",  14.5, 250.0),
    PASPALUM    ("Paspalum (grama-batatais)",     1.1,  40.0);

    private final String nomeCompleto;
    private final double taxaBaseCmDia;
    private final double alturaMaximaCm;

    EspecieGrama(String nomeCompleto, double taxaBaseCmDia, double alturaMaximaCm) {
        this.nomeCompleto = nomeCompleto;
        this.taxaBaseCmDia = taxaBaseCmDia;
        this.alturaMaximaCm = alturaMaximaCm;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public double getTaxaBaseCmDia() {
        return taxaBaseCmDia;
    }

    /** Altura assintotica sem corte. */
    public double getAlturaMaximaCm() {
        return alturaMaximaCm;
    }
}
