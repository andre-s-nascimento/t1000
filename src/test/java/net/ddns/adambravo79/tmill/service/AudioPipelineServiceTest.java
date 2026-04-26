/* (c) 2026 */
package net.ddns.adambravo79.tmill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

class AudioPipelineServiceTest {

  @Test
  void deveProcessarFluxoCompleto() {
    var audio = mock(AudioService.class);
    var groq = mock(net.ddns.adambravo79.tmill.client.GroqClient.class);
    var cache = mock(TranscricaoCache.class);

    var service = new AudioPipelineService(audio, groq, cache);

    File input = spy(new File("a.oga"));
    File wav = spy(new File("a.wav"));

    when(audio.converterParaWav(input)).thenReturn(CompletableFuture.completedFuture(wav));
    when(groq.transcrever(wav)).thenReturn("Bruto");
    when(groq.refinarTexto("Bruto")).thenReturn("Refinado");

    List<String> chamadas = new ArrayList<>();
    List<Boolean> finais = new ArrayList<>();

    BiConsumer<String, Boolean> callback =
        (texto, isUltima) -> {
          chamadas.add(texto);
          finais.add(isUltima);
        };

    service.processarFluxoAudio(input, 1L, callback);

    assertThat(chamadas).contains("🎙️ *Bruto:* \n_Bruto_", "✨ *Refinado:* \nRefinado");
    assertThat(finais).containsExactly(false, true);

    verify(cache).salvar(1L, "Refinado");
    verify(wav).delete();
    verify(input).delete();
  }

  @Test
  void deveFalharQuandoConversaoFalhar() {
    var audio = mock(AudioService.class);
    var groq = mock(net.ddns.adambravo79.tmill.client.GroqClient.class);
    var cache = mock(TranscricaoCache.class);

    var service = new AudioPipelineService(audio, groq, cache);

    File input = spy(new File("a.oga"));

    when(audio.converterParaWav(input))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException()));

    List<String> chamadas = new ArrayList<>();
    List<Boolean> finais = new ArrayList<>();

    BiConsumer<String, Boolean> callback =
        (texto, isUltima) -> {
          chamadas.add(texto);
          finais.add(isUltima);
        };

    service.processarFluxoAudio(input, 1L, callback);

    assertThat(chamadas).contains("⚠️ Falha no processamento do áudio.");
    assertThat(finais).contains(false);

    verify(input).delete();
    verifyNoInteractions(cache);
  }

  @Test
  void deveFalharQuandoTranscricaoFalhar() {
    var audio = mock(AudioService.class);
    var groq = mock(net.ddns.adambravo79.tmill.client.GroqClient.class);
    var cache = mock(TranscricaoCache.class);

    var service = new AudioPipelineService(audio, groq, cache);

    File input = spy(new File("a.oga"));
    File wav = spy(new File("a.wav"));

    when(audio.converterParaWav(input)).thenReturn(CompletableFuture.completedFuture(wav));
    when(groq.transcrever(wav)).thenThrow(new RuntimeException());

    List<String> chamadas = new ArrayList<>();
    List<Boolean> finais = new ArrayList<>();

    BiConsumer<String, Boolean> callback =
        (texto, isUltima) -> {
          chamadas.add(texto);
          finais.add(isUltima);
        };

    service.processarFluxoAudio(input, 1L, callback);

    assertThat(chamadas).contains("⚠️ Falha no processamento do áudio.");
    assertThat(finais).contains(false);

    // Apenas o arquivo de entrada é deletado
    verify(input).delete();
    verifyNoInteractions(cache);
  }

  @Test
  void deveFalharQuandoRefinoFalhar() {
    var audio = mock(AudioService.class);
    var groq = mock(net.ddns.adambravo79.tmill.client.GroqClient.class);
    var cache = mock(TranscricaoCache.class);

    var service = new AudioPipelineService(audio, groq, cache);

    File input = spy(new File("a.oga"));
    File wav = spy(new File("a.wav"));

    when(audio.converterParaWav(input)).thenReturn(CompletableFuture.completedFuture(wav));
    when(groq.transcrever(wav)).thenReturn("Bruto");
    when(groq.refinarTexto("Bruto")).thenThrow(new RuntimeException());

    List<String> chamadas = new ArrayList<>();
    List<Boolean> finais = new ArrayList<>();

    BiConsumer<String, Boolean> callback =
        (texto, isUltima) -> {
          chamadas.add(texto);
          finais.add(isUltima);
        };

    service.processarFluxoAudio(input, 1L, callback);

    assertThat(chamadas).contains("🎙️ *Bruto:* \n_Bruto_", "⚠️ Falha no processamento do áudio.");
    assertThat(finais).contains(false);

    verify(input).delete();
    verifyNoInteractions(cache);
  }
}
