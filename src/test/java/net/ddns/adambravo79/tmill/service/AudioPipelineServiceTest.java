package net.ddns.adambravo79.tmill.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.ddns.adambravo79.tmill.client.GroqClient;
import net.ddns.adambravo79.tmill.model.MovieRecord;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AudioPipelineServiceTest {

    @Mock
    private AudioService audioService;

    @Mock
    private GroqClient groqClient;

    @Mock
    private TranscricaoCache cache;

    @InjectMocks
    private AudioPipelineService service;

    @Test
    void deveExecutarPipelineCompleto() {
        File oga = new File("audio.oga");
        File wav = new File("audio.wav");

        when(audioService.converterParaWav(oga))
                .thenReturn(CompletableFuture.completedFuture(wav));

        when(groqClient.transcrever(wav))
                .thenReturn("texto bruto");

        when(groqClient.refinarTexto("texto bruto"))
                .thenReturn("texto refinado");

        List<String> mensagens = new ArrayList<>();

        service.processarFluxoAudio(oga, 1L, (msg, ultima) -> {
            mensagens.add(msg);
        });

        assertThat(mensagens).hasSize(2);
        assertThat(mensagens.get(0)).contains("Bruto");
        assertThat(mensagens.get(1)).contains("Refinado");

        verify(cache).salvar(1L, "texto refinado");
    }

    @Test
    void deveMapearJsonParaMovieRecord() throws Exception {
        String json = """
                {
                  "id": 1,
                  "title": "Duna",
                  "release_date": "2021-01-01",
                  "vote_average": 8.5,
                  "poster_path": "/img",
                  "origin_country": ["US"]
                }
                """;

        ObjectMapper mapper = new ObjectMapper();

        MovieRecord record = mapper.readValue(json, MovieRecord.class);

        assertThat(record.title()).isEqualTo("Duna");
        assertThat(record.originCountry()).contains("US");
    }
}
