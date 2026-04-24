package net.ddns.adambravo79.tmill.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import net.ddns.adambravo79.tmill.client.BloggerClient;
import net.ddns.adambravo79.tmill.model.*;
import net.ddns.adambravo79.tmill.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.util.List;
import java.util.Optional;

class TelegramControllerTest {

    private MovieService movieService;
    private AudioPipelineService audioService;
    private BloggerClient bloggerClient;
    private TranscricaoCache cache;
    private TelegramFileService fileService;

    private TelegramController controller;

    @BeforeEach
    void setup() {
        movieService = mock(MovieService.class);
        audioService = mock(AudioPipelineService.class);
        bloggerClient = mock(BloggerClient.class);
        cache = mock(TranscricaoCache.class);
        fileService = mock(TelegramFileService.class);

        controller = new TelegramController(
                "token",
                movieService,
                audioService,
                bloggerClient,
                cache,
                fileService);
    }

    // =========================
    // 🎬 FILMES (CORE FLOW)
    // =========================

    @Test
    void deveProcessarBuscaComUmResultado() {
        Long id = 1L;

        var search = new MovieSearchResponse(
                1, 1, 1,
                List.of(new MovieRecord(id, "Duna", "2021", "desc", 1.0, 1.0, "", List.of())));

        when(movieService.buscarFilme("duna")).thenReturn(search);
        when(movieService.buscarPorId(id))
                .thenReturn(new MovieOrchestrationResponse("ok", "url"));

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
        var search = new MovieSearchResponse(
                1, 1, 1,
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

    @Test
    void deveRetornarErroQuandoNaoEncontrarFilme() {
        when(movieService.buscarFilme("x")).thenReturn(null);

        var update = mock(Update.class);
        var message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);

        when(message.getChatId()).thenReturn(1L);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("t1000 buscar x");

        controller.consume(update);

        verify(movieService).buscarFilme("x");
    }

    @Test
    void deveBuscarFilmePorIdViaCallback() {
        var update = mock(Update.class);
        var cb = mock(CallbackQuery.class);
        var message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(cb);

        when(cb.getData()).thenReturn("id:1");
        when(cb.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(1L);

        when(movieService.buscarPorId(1L))
                .thenReturn(new MovieOrchestrationResponse("ok", "url"));

        controller.consume(update);

        verify(movieService).buscarPorId(1L);
    }

    // =========================
    // 📝 BLOGGER
    // =========================

    @Test
    void devePublicarComSucesso() {
        var update = mock(Update.class);
        var cb = mock(CallbackQuery.class);
        var message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(cb);

        when(cb.getData()).thenReturn("blogger:publicar");
        when(cb.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(1L);

        when(cache.recuperar(1L)).thenReturn("texto");
        when(bloggerClient.criarRascunho(any(), any())).thenReturn("url");

        controller.consume(update);

        verify(cache).remover(1L);
    }

    @Test
    void deveFalharAoPublicar() {
        var update = mock(Update.class);
        var cb = mock(CallbackQuery.class);
        var message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(cb);

        when(cb.getData()).thenReturn("blogger:publicar");
        when(cb.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(1L);

        when(cache.recuperar(1L)).thenReturn("texto");
        when(bloggerClient.criarRascunho(any(), any())).thenReturn(null);

        controller.consume(update);

        verify(bloggerClient).criarRascunho(any(), any());
    }

    @Test
    void deveCancelarPublicacao() {
        var update = mock(Update.class);
        var cb = mock(CallbackQuery.class);
        var message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(cb);

        when(cb.getData()).thenReturn("blogger:cancelar");
        when(cb.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(1L);

        controller.consume(update);

        verify(cache).remover(1L);
    }

    // =========================
    // 🎙️ AUDIO (CRÍTICO)
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

        when(fileService.baixarArquivo(any(), any()))
                .thenReturn(Optional.of(new File("audio.oga")));

        // 🔥 EXECUTA O CALLBACK (ESSENCIAL PRA COVERAGE)
        doAnswer(inv -> {
            var cb = inv.getArgument(2, java.util.function.BiConsumer.class);
            cb.accept("bruto", false);
            cb.accept("refinado", true);
            return null;
        }).when(audioService).processarFluxoAudio(any(), anyLong(), any());

        controller.consume(update);

        verify(audioService).processarFluxoAudio(any(), anyLong(), any());
    }

    @Test
    void naoDeveProcessarAudioQuandoDesativado() {
        var update = mock(Update.class);
        var message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasVoice()).thenReturn(true);

        ReflectionTestUtils.setField(controller, "transcriptionEnabled", false);

        controller.consume(update);

        verifyNoInteractions(audioService);
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

        when(fileService.baixarArquivo(any(), any()))
                .thenReturn(Optional.empty());

        controller.consume(update);

        verifyNoInteractions(audioService);
    }

    // =========================
    // 🧩 UTIL
    // =========================

    @Test
    void deveDividirMensagemGrande() {
        String texto = "a ".repeat(5000);

        @SuppressWarnings("unchecked")
        List<String> partes = (List<String>) ReflectionTestUtils.invokeMethod(controller, "dividirMensagem", texto,
                4000);

        assertThat(partes).isNotNull();
        assertThat(partes.size()).isGreaterThan(1);
    }

    @Test
    void deveTratarListaVaziaComoNaoEncontrado() {
        var search = new MovieSearchResponse(1, 1, 1, List.of());

        when(movieService.buscarFilme("x")).thenReturn(search);

        var update = mock(Update.class);
        var message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);

        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("t1000 buscar x");

        controller.consume(update);

        verify(movieService).buscarFilme("x");
    }

    @Test
    void naoDevePublicarSemTextoNoCache() {
        var update = mock(Update.class);
        var cb = mock(CallbackQuery.class);
        var message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(cb);

        when(cb.getData()).thenReturn("blogger:publicar");
        when(cb.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(1L);

        when(cache.recuperar(1L)).thenReturn(null);

        controller.consume(update);

        verify(bloggerClient, never()).criarRascunho(any(), any());
    }

    @Test
    void deveFalharQuandoCallbackIdInvalido() {
        var update = mock(Update.class);
        var cb = mock(CallbackQuery.class);
        var message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(cb);

        when(cb.getData()).thenReturn("id:abc");
        when(cb.getMessage()).thenReturn(message);

        assertThatThrownBy(() -> controller.consume(update))
                .isInstanceOf(NumberFormatException.class);

        verifyNoInteractions(movieService);
    }

    @Test
    void deveAnexarFluxoCompletoQuandoOwner() {
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
        ReflectionTestUtils.setField(controller, "ownerId", 1L);

        when(fileService.baixarArquivo(any(), any()))
                .thenReturn(Optional.of(new File("a.oga")));

        // Simula pipeline chamando callback final
        doAnswer(inv -> {
            var cb = inv.getArgument(2, java.util.function.BiConsumer.class);
            cb.accept("texto refinado", true);
            return null;
        }).when(audioService).processarFluxoAudio(any(), anyLong(), any());

        controller.consume(update);

        // ✅ o que REALMENTE importa aqui
        verify(audioService).processarFluxoAudio(any(), eq(1L), any());
    }

    @Test
    void deveIgnorarMensagemSemConteudo() {
        var update = mock(Update.class);
        var message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);

        when(message.getChatId()).thenReturn(1L); // 👈 evita NPE em alguns fluxos
        when(message.hasText()).thenReturn(false);
        when(message.hasVoice()).thenReturn(false);

        controller.consume(update);

        verifyNoInteractions(movieService, audioService);
    }

    @Test
    void deveProcessarCallbackIdValido() {
        var update = mock(Update.class);
        var cb = mock(CallbackQuery.class);
        var message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(cb);

        when(cb.getData()).thenReturn("id:10");
        when(cb.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(1L);

        when(movieService.buscarPorId(10L))
                .thenReturn(new MovieOrchestrationResponse("ok", "url"));

        controller.consume(update);

        verify(movieService).buscarPorId(10L);
    }

    @Test
    void deveFalharQuandoIdInvalido() {
        var update = mock(Update.class);
        var cb = mock(CallbackQuery.class);
        var message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(cb);

        when(cb.getData()).thenReturn("id:abc"); // inválido
        when(cb.getMessage()).thenReturn(message);

        assertThatThrownBy(() -> controller.consume(update))
                .isInstanceOf(NumberFormatException.class);
    }

}