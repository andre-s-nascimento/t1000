package net.ddns.adambravo79.tmill.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscricaoCacheTest {

    @Test
    void deveSalvarRecuperarRemover() {
        var cache = new TranscricaoCache();

        cache.salvar(1L, "texto");

        assertThat(cache.recuperar(1L)).isEqualTo("texto");

        cache.remover(1L);

        assertThat(cache.recuperar(1L)).isNull();
    }
}