/* (c) 2026 */
package net.ddns.adambravo79.tmill.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import net.ddns.adambravo79.tmill.client.BloggerClient;
import net.ddns.adambravo79.tmill.model.*;
import net.ddns.adambravo79.tmill.service.*;
import net.ddns.adambravo79.tmill.telegram.TelegramFacade;

class TelegramControllerTest {

  private MovieService movieService;
  private AudioPipelineService audioService;
  private BloggerClient bloggerClient;
  private TranscricaoCache cache;
  private TelegramFileService fileService;
  private TelegramFacade telegramFacade;

  private TelegramController controller;

  @BeforeEach
  void setup() {
    movieService = mock(MovieService.class);
    audioService = mock(AudioPipelineService.class);
    bloggerClient = mock(BloggerClient.class);
    cache = mock(TranscricaoCache.class);
    fileService = mock(TelegramFileService.class);
    telegramFacade = mock(TelegramFacade.class);

    controller =
        new TelegramController(
            movieService, audioService, bloggerClient, cache, fileService, telegramFacade);
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

    var update = mock(Update.class);
    var message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);

    when(message.getChatId()).thenReturn(1L);
    when(message.hasText()).thenReturn(true);
    when(message.getText()).thenReturn("t1000 buscar duna");

    controller.consume(update);

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

    var update = mock(Update.class);
    var message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);

    when(message.getChatId()).thenReturn(1L);
    when(message.hasText()).thenReturn(true);
    when(message.getText()).thenReturn("t1000 buscar teste");

    controller.consume(update);

    verify(movieService).buscarFilme("teste");
    verify(movieService, never()).buscarPorId(anyLong());
  }

  // =========================
  // 🎙️ AUDIO
  // =========================

  @Test
  void deveProcessarAudioCompleto() {
    var update = mock(Update.class);
    var message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);

    when(message.hasVoice()).thenReturn(true);
    when(message.getChatId()).thenReturn(1L);

    var voice = mock(Voice.class);
    when(voice.getFileId()).thenReturn("file-id");
    when(message.getVoice()).thenReturn(voice);

    var user = mock(User.class);
    when(user.getId()).thenReturn(1L);
    when(message.getFrom()).thenReturn(user);

    ReflectionTestUtils.setField(controller, "transcriptionEnabled", true);

    when(fileService.baixarArquivo(any())).thenReturn(new File("audio.oga"));

    doAnswer(
            inv -> {
              @SuppressWarnings("unchecked")
              BiConsumer<String, Boolean> cb = (BiConsumer<String, Boolean>) inv.getArgument(2);

              cb.accept("bruto", false);
              cb.accept("refinado", true);
              return null;
            })
        .when(audioService)
        .processarFluxoAudio(any(), anyLong(), any());

    controller.consume(update);

    verify(audioService).processarFluxoAudio(any(), anyLong(), any());
  }

  @Test
  void naoDeveProcessarSeDownloadFalhar() {
    var update = mock(Update.class);
    var message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);

    when(message.hasVoice()).thenReturn(true);

    var voice = mock(Voice.class);
    when(voice.getFileId()).thenReturn("file-id");
    when(message.getVoice()).thenReturn(voice);

    ReflectionTestUtils.setField(controller, "transcriptionEnabled", true);

    when(fileService.baixarArquivo(any())).thenThrow(new RuntimeException("falha"));

    controller.consume(update);

    verifyNoInteractions(audioService);
  }

  @Test
  void deveDividirMensagemGrandeEEnviarPartes() {
    var update = mock(Update.class);
    var message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);

    when(message.hasVoice()).thenReturn(true);
    when(message.getChatId()).thenReturn(1L);

    var voice = mock(Voice.class);
    when(voice.getFileId()).thenReturn("file-id");
    when(message.getVoice()).thenReturn(voice);

    var user = mock(User.class);
    when(user.getId()).thenReturn(1L);
    when(message.getFrom()).thenReturn(user);

    ReflectionTestUtils.setField(controller, "transcriptionEnabled", true);

    when(fileService.baixarArquivo(any())).thenReturn(new File("audio.oga"));

    doAnswer(
            inv -> {
              @SuppressWarnings("unchecked")
              BiConsumer<String, Boolean> cb = (BiConsumer<String, Boolean>) inv.getArgument(2);

              cb.accept("a ".repeat(5000), true);
              return null;
            })
        .when(audioService)
        .processarFluxoAudio(any(), anyLong(), any());

    controller.consume(update);

    verify(telegramFacade, atLeast(2)).enviarMensagem(eq(1L), any());
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
  void deveProcessarCallbackPublicar() {
    var cb = mock(CallbackQuery.class);
    var message = mock(Message.class);
    when(cb.getData()).thenReturn("blogger:publicar");
    when(cb.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(1L);

    when(cache.recuperar(1L)).thenReturn("texto");
    when(bloggerClient.criarRascunho(any(), any())).thenReturn("url");

    var update = mock(Update.class);
    when(update.hasCallbackQuery()).thenReturn(true);
    when(update.getCallbackQuery()).thenReturn(cb);

    controller.consume(update);

    verify(bloggerClient).criarRascunho("Post automático", "texto");
    verify(telegramFacade).enviarMensagem(1L, "✅ Publicado: url");
  }

  @Test
  void deveProcessarCallbackId() {
    var cb = mock(CallbackQuery.class);
    var message = mock(Message.class);
    when(cb.getData()).thenReturn("id:123");
    when(cb.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(1L);

    var update = mock(Update.class);
    when(update.hasCallbackQuery()).thenReturn(true);
    when(update.getCallbackQuery()).thenReturn(cb);

    when(movieService.buscarPorId(123L)).thenReturn(new MovieOrchestrationResponse("texto", "url"));

    controller.consume(update);

    verify(movieService).buscarPorId(123L);
    verify(telegramFacade).enviarFoto(eq(1L), anyString(), anyString());
  }

  @Test
  void deveProcessarCallbackCancelar() {
    var cb = mock(CallbackQuery.class);
    var message = mock(Message.class);
    when(cb.getData()).thenReturn("blogger:cancelar");
    when(cb.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(1L);

    var update = mock(Update.class);
    when(update.hasCallbackQuery()).thenReturn(true);
    when(update.getCallbackQuery()).thenReturn(cb);

    controller.consume(update);

    verify(cache).remover(1L);
    verify(telegramFacade).enviarMensagem(1L, "❌ Publicação cancelada.");
  }

  @Test
  void deveProcessarCallbackPublicarComTexto() {
    var cb = mock(CallbackQuery.class);
    var message = mock(Message.class);
    when(cb.getData()).thenReturn("blogger:publicar");
    when(cb.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(1L);

    var update = mock(Update.class);
    when(update.hasCallbackQuery()).thenReturn(true);
    when(update.getCallbackQuery()).thenReturn(cb);

    when(cache.recuperar(1L)).thenReturn("texto");
    when(bloggerClient.criarRascunho(any(), eq("texto"))).thenReturn("url");

    controller.consume(update);

    verify(bloggerClient).criarRascunho("Post automático", "texto");
    verify(telegramFacade).enviarMensagem(1L, "✅ Publicado: url");
    verify(cache).remover(1L);
  }

  @Test
  void deveProcessarCallbackPublicarSemTexto() {
    var cb = mock(CallbackQuery.class);
    var message = mock(Message.class);
    when(cb.getData()).thenReturn("blogger:publicar");
    when(cb.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(1L);

    var update = mock(Update.class);
    when(update.hasCallbackQuery()).thenReturn(true);
    when(update.getCallbackQuery()).thenReturn(cb);

    when(cache.recuperar(1L)).thenReturn(null);

    controller.consume(update);

    verify(telegramFacade).enviarMensagem(1L, "⚠️ Nenhuma transcrição disponível.");
  }

  @Test
  void deveConsumirListaDeUpdates() {
    var update = mock(Update.class);
    when(update.hasMessage()).thenReturn(false);

    controller.consume(List.of(update));

    // apenas garante que não lança exceção
    verifyNoInteractions(movieService, audioService);
  }

  @Test
  void deveIgnorarListaVaziaOuNula() {
    // lista vazia
    controller.consume(List.of());
    // lista nula
    controller.consume((List<Update>) null);

    // garante que não houve interação com os serviços
    verifyNoInteractions(
        movieService, audioService, bloggerClient, cache, fileService, telegramFacade);
  }

  @Test
  void deveInformarQuandoFilmeNaoEncontrado() {
    when(movieService.buscarFilme("inexistente"))
        .thenReturn(new MovieSearchResponse(1, 0, 0, List.of()));

    var update = mock(Update.class);
    var message = mock(Message.class);
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(1L);
    when(message.hasText()).thenReturn(true);
    when(message.getText()).thenReturn("t1000 buscar inexistente");

    controller.consume(update);

    verify(telegramFacade).enviarMensagem(1L, "❌ Filme não encontrado.");
  }

  @Test
  void deveAvisarQuandoTranscricaoDesativada() {
    var update = mock(Update.class);
    var message = mock(Message.class);
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasVoice()).thenReturn(true);
    when(message.getChatId()).thenReturn(1L);

    ReflectionTestUtils.setField(controller, "transcriptionEnabled", false);

    controller.consume(update);

    verify(telegramFacade).enviarMensagem(1L, "🔇 Transcrição desativada.");
  }

  @Test
  void deveFecharMarkdownQuandoAsteriscosImpares() {
    String texto = "*texto* com *asterisco";
    String fechado = (String) ReflectionTestUtils.invokeMethod(controller, "fecharMarkdown", texto);
    assertThat(fechado).endsWith("*");
  }

  @Test
  void deveEnviarRespostaComBotoesBlogger() {
    ReflectionTestUtils.invokeMethod(controller, "enviarRespostaComBotoesBlogger", 1L, "texto");
    verify(cache).salvar(1L, "texto");
    verify(telegramFacade).enviarComBotoes(eq(1L), eq("texto"), any());
  }
}
