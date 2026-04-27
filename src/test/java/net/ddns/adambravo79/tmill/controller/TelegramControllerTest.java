/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import net.ddns.adambravo79.tmill.cache.TranscricaoCache;
import net.ddns.adambravo79.tmill.client.BloggerClient;
import net.ddns.adambravo79.tmill.model.*;
import net.ddns.adambravo79.tmill.service.*;
import net.ddns.adambravo79.tmill.telegram.core.TelegramAction;
import net.ddns.adambravo79.tmill.telegram.core.TelegramFacade;
import net.ddns.adambravo79.tmill.telegram.core.TelegramSafeExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;

class TelegramControllerTest {

  private MovieService movieService;
  private AudioPipelineService audioService;
  private BloggerClient bloggerClient;
  private TranscricaoCache cache;
  private TelegramFileService fileService;
  private TelegramFacade telegramFacade;
  private TelegramSafeExecutor safeExecutor;

  private TelegramController controller;

  @BeforeEach
  void setup() {
    movieService = mock(MovieService.class);
    audioService = mock(AudioPipelineService.class);
    bloggerClient = mock(BloggerClient.class);
    cache = mock(TranscricaoCache.class);
    fileService = mock(TelegramFileService.class);
    telegramFacade = mock(TelegramFacade.class);
    safeExecutor = mock(TelegramSafeExecutor.class);

    // ✅ Replica o comportamento real: executa o action e absorve exceções
    doAnswer(
            inv -> {
              TelegramAction action = inv.getArgument(2);
              try {
                action.run();
              } catch (Exception e) {
                // absorve — igual ao TelegramSafeExecutor real
              }
              return null;
            })
        .when(safeExecutor)
        .run(anyLong(), any(), any(TelegramAction.class));

    controller =
        new TelegramController(
            movieService,
            audioService,
            bloggerClient,
            cache,
            fileService,
            telegramFacade,
            safeExecutor);
  }

  // =========================
  // 🎬 FILMES
  // =========================

  @Test
  void deveProcessarBuscaComUmResultado() {
    Long id = 1L;
    var search =
        new MovieSearchResponse(
            1, 1, 1, List.of(new MovieRecord(id, "Duna", "2021", "desc", 1.0, 1.0, "", List.of())));

    when(movieService.buscarFilme("duna")).thenReturn(search);
    when(movieService.buscarPorId(id)).thenReturn(new MovieOrchestrationResponse("ok", "url"));

    controller.consume(buildTextUpdate(1L, "t1000 buscar duna"));

    verify(movieService).buscarFilme("duna");
    verify(movieService).buscarPorId(id);
  }

  @Test
  void deveChamarDesambiguacaoQuandoMultiplosResultados() {
    var search =
        new MovieSearchResponse(
            1,
            1,
            1,
            List.of(
                new MovieRecord(1L, "A", "2020", "", 1.0, 1.0, "", List.of()),
                new MovieRecord(2L, "B", "2021", "", 1.0, 1.0, "", List.of())));

    when(movieService.buscarFilme("teste")).thenReturn(search);

    controller.consume(buildTextUpdate(1L, "t1000 buscar teste"));

    verify(movieService).buscarFilme("teste");
    verify(movieService, never()).buscarPorId(anyLong());
  }

  @Test
  void deveInformarQuandoFilmeNaoEncontrado() {
    when(movieService.buscarFilme("inexistente"))
        .thenReturn(new MovieSearchResponse(1, 0, 0, List.of()));

    controller.consume(buildTextUpdate(1L, "t1000 buscar inexistente"));

    verify(telegramFacade).enviarMensagem(1L, "❌ Filme não encontrado.");
  }

  // =========================
  // 🎙️ AUDIO
  // =========================

  @Test
  void deveProcessarAudioCompleto() {
    ReflectionTestUtils.setField(controller, "transcriptionEnabled", true);

    when(fileService.baixarArquivo(any())).thenReturn(new File("audio.oga"));
    doAnswer(
            inv -> {
              BiConsumer<String, Boolean> cb = inv.getArgument(2);
              cb.accept("bruto", false);
              cb.accept("refinado", true);
              return null;
            })
        .when(audioService)
        .processarFluxoAudio(any(), anyLong(), any());

    controller.consume(buildVoiceUpdate(1L, "file-id", 1L));

    verify(audioService).processarFluxoAudio(any(), anyLong(), any());
  }

  @Test
  void naoDeveProcessarSeDownloadFalhar() {
    ReflectionTestUtils.setField(controller, "transcriptionEnabled", true);

    when(fileService.baixarArquivo(any())).thenThrow(new RuntimeException("falha"));

    controller.consume(buildVoiceUpdate(1L, "file-id", 1L));

    verifyNoInteractions(audioService);
  }

  @Test
  void deveDividirMensagemGrandeEEnviarPartes() {
    ReflectionTestUtils.setField(controller, "transcriptionEnabled", true);

    when(fileService.baixarArquivo(any())).thenReturn(new File("audio.oga"));
    doAnswer(
            inv -> {
              BiConsumer<String, Boolean> cb = inv.getArgument(2);
              cb.accept("a ".repeat(5000), true);
              return null;
            })
        .when(audioService)
        .processarFluxoAudio(any(), anyLong(), any());

    controller.consume(buildVoiceUpdate(1L, "file-id", 1L));

    verify(telegramFacade, atLeast(2)).enviarMensagem(eq(1L), any());
  }

  @Test
  void deveAvisarQuandoTranscricaoDesativada() {
    ReflectionTestUtils.setField(controller, "transcriptionEnabled", false);

    controller.consume(buildVoiceUpdate(1L, "file-id", 1L));

    verify(telegramFacade).enviarMensagem(1L, "🔇 Transcrição desativada.");
  }

  // =========================
  // 🔘 CALLBACKS
  // =========================

  @Test
  void deveProcessarCallbackPublicar() {
    when(cache.recuperar(1L)).thenReturn("texto");
    when(bloggerClient.criarRascunho(any(), any())).thenReturn("url");

    controller.consume(buildCallbackUpdate(1L, "blogger:publicar"));

    verify(bloggerClient).criarRascunho("Post automático", "texto");
    verify(telegramFacade).enviarMensagem(1L, "✅ Publicado: url");
  }

  @Test
  void deveProcessarCallbackPublicarComTexto() {
    when(cache.recuperar(1L)).thenReturn("texto");
    when(bloggerClient.criarRascunho(any(), eq("texto"))).thenReturn("url");

    controller.consume(buildCallbackUpdate(1L, "blogger:publicar"));

    verify(bloggerClient).criarRascunho("Post automático", "texto");
    verify(telegramFacade).enviarMensagem(1L, "✅ Publicado: url");
    verify(cache).remover(1L);
  }

  @Test
  void deveProcessarCallbackPublicarSemTexto() {
    when(cache.recuperar(1L)).thenReturn(null);

    controller.consume(buildCallbackUpdate(1L, "blogger:publicar"));

    verify(telegramFacade).enviarMensagem(1L, "⚠️ Nenhuma transcrição disponível.");
  }

  @Test
  void deveProcessarCallbackCancelar() {
    controller.consume(buildCallbackUpdate(1L, "blogger:cancelar"));

    verify(cache).remover(1L);
    verify(telegramFacade).enviarMensagem(1L, "❌ Publicação cancelada.");
  }

  @Test
  void deveProcessarCallbackId() {
    when(movieService.buscarPorId(123L)).thenReturn(new MovieOrchestrationResponse("texto", "url"));

    controller.consume(buildCallbackUpdate(1L, "id:123"));

    verify(movieService).buscarPorId(123L);
    verify(telegramFacade).enviarFoto(eq(1L), anyString(), anyString());
  }

  // =========================
  // 🧩 UTIL
  // =========================

  @Test
  void deveDividirMensagemGrande() {
    String texto = "a ".repeat(5000);
    @SuppressWarnings("unchecked")
    List<String> partes =
        (List<String>) ReflectionTestUtils.invokeMethod(controller, "dividirMensagem", texto, 4000);

    assertThat(partes).isNotNull().hasSizeGreaterThan(1);
  }

  @Test
  void deveIgnorarMensagemSemConteudo() {
    var update = mock(Update.class);
    var message = mock(Message.class);
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(false);
    when(message.hasVoice()).thenReturn(false);

    controller.consume(update);

    verifyNoInteractions(movieService, audioService);
  }

  @Test
  void deveConsumirListaDeUpdates() {
    var update = mock(Update.class);
    when(update.hasMessage()).thenReturn(false);

    controller.consume(List.of(update));

    verifyNoInteractions(movieService, audioService);
  }

  @Test
  void deveIgnorarListaVaziaOuNula() {
    controller.consume(List.of());
    controller.consume((List<Update>) null);

    verifyNoInteractions(
        movieService, audioService, bloggerClient, cache, fileService, telegramFacade);
  }

  @Test
  void deveFecharMarkdownQuandoAsteriscosImpares() {
    String fechado =
        (String)
            ReflectionTestUtils.invokeMethod(
                controller, "fecharMarkdown", "*texto* com *asterisco");

    assertThat(fechado).endsWith("*");
  }

  @Test
  void deveEnviarRespostaComBotoesBlogger() {
    ReflectionTestUtils.invokeMethod(controller, "enviarRespostaComBotoesBlogger", 1L, "texto");

    verify(cache).salvar(1L, "texto");
    verify(telegramFacade).enviarComBotoes(eq(1L), eq("texto"), any());
  }

  // =========================
  // 🏗️ BUILDERS
  // =========================

  private Update buildTextUpdate(long chatId, String text) {
    var update = mock(Update.class);
    var message = mock(Message.class);
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(chatId);
    when(message.hasText()).thenReturn(true);
    when(message.getText()).thenReturn(text);
    return update;
  }

  private Update buildVoiceUpdate(long chatId, String fileId, long userId) {
    var update = mock(Update.class);
    var message = mock(Message.class);
    var voice = mock(Voice.class);
    var user = mock(User.class);
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(chatId);
    when(message.hasVoice()).thenReturn(true);
    when(message.getVoice()).thenReturn(voice);
    when(voice.getFileId()).thenReturn(fileId);
    when(message.getFrom()).thenReturn(user);
    when(user.getId()).thenReturn(userId);
    return update;
  }

  private Update buildCallbackUpdate(long chatId, String data) {
    var update = mock(Update.class);
    var cb = mock(CallbackQuery.class);
    var message = mock(Message.class);
    when(update.hasCallbackQuery()).thenReturn(true);
    when(update.getCallbackQuery()).thenReturn(cb);
    when(cb.getData()).thenReturn(data);
    when(cb.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(chatId);
    return update;
  }
}
