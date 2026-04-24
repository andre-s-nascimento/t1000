package net.ddns.adambravo79.tmill.service;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class AudioPipelineServiceTest {

        @Test
        void deveProcessarFluxoCompleto() {
                var audio = mock(AudioService.class);
                var groq = mock(net.ddns.adambravo79.tmill.client.GroqClient.class);
                var cache = mock(TranscricaoCache.class);

                var service = new AudioPipelineService(audio, groq, cache);

                File input = spy(new File("a.oga"));
                File wav = spy(new File("a.wav"));

                when(audio.converterParaWav(input))
                                .thenReturn(CompletableFuture.completedFuture(wav));

                when(groq.transcrever(wav)).thenReturn("bruto");
                when(groq.refinarTexto("bruto")).thenReturn("refinado");

                BiConsumer<String, Boolean> callback = mock(BiConsumer.class);

                service.processarFluxoAudio(input, 1L, callback);

                verify(callback).accept(contains("Bruto"), eq(false));
                verify(callback).accept(contains("Refinado"), eq(true));
                verify(cache).salvar(1L, "refinado");

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

                BiConsumer<String, Boolean> callback = mock(BiConsumer.class);

                service.processarFluxoAudio(input, 1L, callback);

                verify(callback).accept(contains("Falha"), eq(false));
                verify(input).delete();
        }

        @Test
        void deveFalharQuandoTranscricaoFalhar() {
                var audio = mock(AudioService.class);
                var groq = mock(net.ddns.adambravo79.tmill.client.GroqClient.class);
                var cache = mock(TranscricaoCache.class);

                var service = new AudioPipelineService(audio, groq, cache);

                File input = spy(new File("a.oga"));
                File wav = spy(new File("a.wav"));

                when(audio.converterParaWav(input))
                                .thenReturn(CompletableFuture.completedFuture(wav));

                when(groq.transcrever(wav))
                                .thenThrow(new RuntimeException());

                BiConsumer<String, Boolean> callback = mock(BiConsumer.class);

                service.processarFluxoAudio(input, 1L, callback);

                verify(callback).accept(contains("Falha"), eq(false));
                verify(input).delete();
        }

        @Test
        void deveFalharQuandoRefinoFalhar() {
                var audio = mock(AudioService.class);
                var groq = mock(net.ddns.adambravo79.tmill.client.GroqClient.class);
                var cache = mock(TranscricaoCache.class);

                var service = new AudioPipelineService(audio, groq, cache);

                File input = spy(new File("a.oga"));
                File wav = spy(new File("a.wav"));

                when(audio.converterParaWav(input))
                                .thenReturn(CompletableFuture.completedFuture(wav));

                when(groq.transcrever(wav)).thenReturn("bruto");
                when(groq.refinarTexto("bruto"))
                                .thenThrow(new RuntimeException());

                BiConsumer<String, Boolean> callback = mock(BiConsumer.class);

                service.processarFluxoAudio(input, 1L, callback);

                verify(callback).accept(contains("Falha"), eq(false));
                verify(input).delete();
        }
}