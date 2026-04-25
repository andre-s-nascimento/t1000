/* (c) 2026 */
package net.ddns.adambravo79.tmill.controller;

import java.io.File;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.client.BloggerClient;
import net.ddns.adambravo79.tmill.model.MovieRecord;
import net.ddns.adambravo79.tmill.service.*;
import net.ddns.adambravo79.tmill.telegram.TelegramFacade;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramController implements LongPollingUpdateConsumer {

  private final MovieService movieService;
  private final AudioPipelineService audioService;
  private final BloggerClient bloggerClient;
  private final TranscricaoCache cache;
  private final TelegramFileService fileService;
  private final TelegramFacade telegramFacade;

  @Value("${app.transcription.enabled:true}")
  private boolean transcriptionEnabled;

  @Value("${app.owner.id:0}")
  private long ownerId;

  private static final int TELEGRAM_LIMIT = 4000;

  @Override
  public void consume(List<Update> updates) {
    updates.forEach(this::consumeSingle);
  }

  public void consumeSingle(Update update) {

    if (update.hasCallbackQuery()) {
      tratarCallback(update.getCallbackQuery());
      return;
    }

    if (!update.hasMessage()) return;

    var message = update.getMessage();
    long chatId = message.getChatId();

    if (message.hasVoice() || message.hasAudio()) {
      tratarFluxoAudio(message, chatId);
      return;
    }

    if (!message.hasText()) return;

    tratarTexto(message, chatId);
  }

  // =========================
  // 🎬 TEXTO / FILMES
  // =========================

  private void tratarTexto(Message message, long chatId) {
    String texto = message.getText().toLowerCase();

    if (!texto.startsWith("t1000 buscar")) return;

    String nome = texto.replace("t1000 buscar", "").trim();

    var busca = movieService.buscarFilme(nome);

    if (busca == null || busca.results() == null || busca.results().isEmpty()) {
      telegramFacade.enviarMensagem(chatId, "❌ Filme não encontrado.");
      return;
    }

    if (busca.results().size() == 1) {
      var id = busca.results().get(0).id();
      var resposta = movieService.buscarPorId(id);

      telegramFacade.enviarFoto(chatId, resposta.urlFoto(), resposta.textoFormatado());
      return;
    }

    enviarOpcoesDesambiguacao(chatId, busca.results());
  }

  // =========================
  // 🎙️ AUDIO
  // =========================

  private void tratarFluxoAudio(Message message, long chatId) {

    if (!transcriptionEnabled) {
      telegramFacade.enviarMensagem(chatId, "🔇 Transcrição desativada.");
      return;
    }

    String fileId =
        message.hasVoice() ? message.getVoice().getFileId() : message.getAudio().getFileId();

    // 🔥 Sem Optional, sem null, sem try/catch — erro sobe para handler
    File file = fileService.baixarArquivo(fileId);

    long userId = message.getFrom().getId();
    boolean isOwner = userId == ownerId;

    audioService.processarFluxoAudio(
        file,
        chatId,
        (texto, isUltima) -> {

          // 🔹 Quebra mensagens grandes
          if (texto.length() > TELEGRAM_LIMIT) {
            dividirMensagem(texto, TELEGRAM_LIMIT).stream()
                .map(this::fecharMarkdown)
                .forEach(parte -> telegramFacade.enviarMensagem(chatId, parte));
            return;
          }

          // 🔹 Última mensagem + owner → botões Blogger
          if (isUltima && isOwner) {
            enviarRespostaComBotoesBlogger(chatId, texto);
          } else {
            telegramFacade.enviarMensagem(chatId, texto);
          }
        });
  }

  // =========================
  // 🔘 CALLBACKS
  // =========================

  private void tratarCallback(CallbackQuery cb) {

    String data = cb.getData();
    long chatId = cb.getMessage().getChatId();

    if (data.startsWith("id:")) {
      long id = Long.parseLong(data.replace("id:", ""));
      var resposta = movieService.buscarPorId(id);

      telegramFacade.enviarFoto(chatId, resposta.urlFoto(), resposta.textoFormatado());
      return;
    }

    if ("blogger:cancelar".equals(data)) {
      cache.remover(chatId);
      telegramFacade.enviarMensagem(chatId, "❌ Publicação cancelada.");
      return;
    }

    if ("blogger:publicar".equals(data)) {
      String texto = cache.recuperar(chatId);

      if (texto == null) {
        telegramFacade.enviarMensagem(chatId, "⚠️ Nenhuma transcrição disponível.");
        return;
      }

      String url = bloggerClient.criarRascunho("Post automático", texto);

      if (url != null) {
        telegramFacade.enviarMensagem(chatId, "✅ Publicado: " + url);
        cache.remover(chatId);
      } else {
        telegramFacade.enviarMensagem(chatId, "❌ Falha ao publicar.");
      }
    }
  }

  // =========================
  // 🧩 UI HELPERS
  // =========================

  private void enviarOpcoesDesambiguacao(long chatId, List<MovieRecord> resultados) {

    List<InlineKeyboardRow> rows = new ArrayList<>();
    InlineKeyboardRow currentRow = new InlineKeyboardRow();

    var lista = resultados.stream().limit(10).toList();

    for (int i = 0; i < lista.size(); i++) {
      var filme = lista.get(i);

      String ano =
          (filme.releaseDate() != null && filme.releaseDate().length() >= 4)
              ? " (" + filme.releaseDate().substring(0, 4) + ")"
              : " (S/A)";

      currentRow.add(
          InlineKeyboardButton.builder()
              .text(filme.title() + ano)
              .callbackData("id:" + filme.id())
              .build());

      if ((i + 1) % 2 == 0 || (i + 1) == lista.size()) {
        rows.add(currentRow);
        currentRow = new InlineKeyboardRow();
      }
    }

    var markup = InlineKeyboardMarkup.builder().keyboard(rows).build();

    telegramFacade.enviarComBotoes(
        chatId, "🧐 Encontrei vários resultados. Qual você quer?", markup);
  }

  private void enviarRespostaComBotoesBlogger(long chatId, String texto) {

    cache.salvar(chatId, texto);

    InlineKeyboardMarkup markup =
        InlineKeyboardMarkup.builder()
            .keyboard(
                List.of(
                    new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                            .text("📝 Publicar")
                            .callbackData("blogger:publicar")
                            .build(),
                        InlineKeyboardButton.builder()
                            .text("❌ Cancelar")
                            .callbackData("blogger:cancelar")
                            .build())))
            .build();

    telegramFacade.enviarComBotoes(chatId, texto, markup);
  }

  private List<String> dividirMensagem(String texto, int limite) {
    List<String> partes = new ArrayList<>();

    while (texto.length() > limite) {

      int corte = encontrarMelhorCorte(texto, limite);

      partes.add(texto.substring(0, corte).trim());
      texto = texto.substring(corte).trim();
    }

    if (!texto.isEmpty()) {
      partes.add(texto);
    }

    return partes;
  }

  private int encontrarMelhorCorte(String texto, int limite) {

    // 1. Tenta quebrar por parágrafo
    int corte = texto.lastIndexOf("\n\n", limite);
    if (corte > 0) return corte;

    // 2. Tenta quebra de linha
    corte = texto.lastIndexOf("\n", limite);
    if (corte > 0) return corte;

    // 3. Tenta espaço
    corte = texto.lastIndexOf(" ", limite);
    if (corte > 0) return corte;

    // 4. fallback bruto
    return limite;
  }

  private String fecharMarkdown(String texto) {
    // exemplo simples: fecha *
    long count = texto.chars().filter(c -> c == '*').count();
    if (count % 2 != 0) {
      texto += "*";
    }
    return texto;
  }
}
