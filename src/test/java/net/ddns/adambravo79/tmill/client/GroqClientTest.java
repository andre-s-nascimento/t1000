/* (c) 2026 | 02/05/2026 */
package net.ddns.adambravo79.tmill.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import net.ddns.adambravo79.tmill.model.ChatCompletionResponse;
import net.ddns.adambravo79.tmill.model.Choice;
import net.ddns.adambravo79.tmill.model.Message;
import net.ddns.adambravo79.tmill.model.TranscriptionResponse;

class GroqClientTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(uriSpec);
        lenient().doReturn(bodySpec).when(bodySpec).body(any(MultiValueMap.class));
        lenient().doReturn(bodySpec).when(bodySpec).body(any(Object.class));
        lenient().doReturn(responseSpec).when(bodySpec).retrieve();
    }

    private void stubTranscricaoUri() {
        when(uriSpec.uri("/openai/v1/audio/transcriptions")).thenReturn(bodySpec);
        lenient().doReturn(bodySpec).when(bodySpec).contentType(MediaType.MULTIPART_FORM_DATA);
    }

    @Test
    void deveTranscreverComSucesso() {
        stubTranscricaoUri();
        var resp = mock(TranscriptionResponse.class);
        when(resp.text()).thenReturn("Texto transcrito");
        when(responseSpec.body(TranscriptionResponse.class)).thenReturn(resp);
        assertThat(new GroqClient(restClient, 5000).transcrever(new File("teste.wav")))
                .isEqualTo("Texto transcrito");
    }

    @Test
    void deveFalharQuandoTranscricaoInvalida() {
        stubTranscricaoUri();
        when(responseSpec.body(TranscriptionResponse.class)).thenReturn(null);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(
                        () -> new GroqClient(restClient, 5000).transcrever(new File("teste.wav")))
                .withMessageContaining("Falha na transcrição");
    }

    private void stubRefinoUri() {
        when(uriSpec.uri("/openai/v1/chat/completions")).thenReturn(bodySpec);
        lenient().doReturn(bodySpec).when(bodySpec).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void deveRefinarTextoComSucesso() {
        stubRefinoUri();
        var resp =
                new ChatCompletionResponse(
                        List.of(new Choice(new Message("assistant", "Texto refinado"))));
        when(responseSpec.body(ChatCompletionResponse.class)).thenReturn(resp);
        assertThat(new GroqClient(restClient, 5000).refinarTexto("Texto bruto"))
                .isEqualTo("Texto refinado");
    }

    @Test
    void deveRetornarTextoBrutoQuandoRefinoInvalido() {
        stubRefinoUri();
        when(responseSpec.body(ChatCompletionResponse.class)).thenReturn(null);
        String resultado = new GroqClient(restClient, 5000).refinarTexto("Texto bruto");
        assertThat(resultado).isEqualTo("Texto bruto");
    }

    @Test
    void deveRetornarTextoBrutoComAvisoQuandoMuitoLongo() {
        String textoLongo = "x".repeat(6000);
        GroqClient client = new GroqClient(restClient, 5000);
        String resultado = client.refinarTexto(textoLongo);
        assertThat(resultado).startsWith("⚠️ *Aviso:* O texto excede o limite");
        assertThat(resultado).contains(textoLongo);
    }
}
