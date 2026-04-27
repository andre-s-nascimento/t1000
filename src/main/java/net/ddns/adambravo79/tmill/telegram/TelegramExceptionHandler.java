/* (c) 2026 */
package net.ddns.adambravo79.tmill.telegram;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.exception.AudioProcessingException;

@Slf4j
@Component
public class TelegramExceptionHandler {

    public void handle(Exception e, long chatId, TelegramSender sender) {
        log.error("Erro no fluxo Telegram - chatId={}", chatId, e);

        String mensagemUsuario = mapearMensagem(e);

        try {
            sender.enviar(chatId, mensagemUsuario);
        } catch (Exception sendError) {
            log.error("Erro ao enviar mensagem de erro para o usuário", sendError);
        }
    }

    private String mapearMensagem(Exception e) {

        if (e instanceof TelegramFileException) {
            return "⚠️ Não consegui baixar o áudio.";
        }

        if (e instanceof IOException) {
            return "📡 Problema de comunicação com o servidor.";
        }

        if (e instanceof AudioProcessingException) {
            return "🎧 Erro ao processar o áudio.";
        }

        if (e instanceof TelegramApiException) {
            return "⚠️ Erro ao falar com o Telegram.";
        }

        String msg = e.getMessage() != null ? e.getMessage() : "";

        // Limite de requisições
        if (msg.contains("429")) {
            return "⏳ Muitas requisições. Tente novamente em alguns segundos.";
        }

        // Problemas de rede
        if (e instanceof java.io.IOException) {
            return "📡 Problema de comunicação com o servidor. Tente novamente.";
        }

        // Autenticação inválida
        if (msg.contains("401") || msg.toLowerCase().contains("unauthorized")) {
            return "🔑 Token inválido ou expirado. Verifique suas credenciais.";
        }

        // Chat ou usuário não encontrado
        if (msg.contains("400") && msg.toLowerCase().contains("chat not found")) {
            return "❌ Não consegui encontrar este chat. Verifique se o ID está correto.";
        }

        // Timeout
        if (msg.toLowerCase().contains("timeout")) {
            return "⏱️ O servidor demorou a responder. Tente novamente em instantes.";
        }

        if (msg.contains("file is too big")) {
            return "📂 O arquivo enviado é muito grande. Tente reduzir o tamanho.";
        }

        if (msg.contains("wrong file type")) {
            return "🛑 Formato de arquivo não suportado. Envie em outro formato.";
        }

        // Erros genéricos do Telegram
        if (e instanceof TelegramApiException) {
            return "⚠️ Ocorreu um erro no envio da mensagem. Tente novamente.";
        }

        // Fallback
        return "⚠️ Ocorreu um erro inesperado.";
    }
}
