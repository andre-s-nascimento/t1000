package net.ddns.adambravo79.tmill.model;

/**
 * Transporta o texto formatado e a URL da foto corretos para o filme.
 */
public record MovieOrchestrationResponse(
        String textoFormatado,
        String urlFoto) {
}