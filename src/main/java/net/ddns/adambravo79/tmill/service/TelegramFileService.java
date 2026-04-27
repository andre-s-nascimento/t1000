/* (c) 2026-2026 */
package net.ddns.adambravo79.tmill.service;

import java.io.File;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.tmill.exception.TelegramFileException;
import net.ddns.adambravo79.tmill.telegram.TelegramFacade;

/**
 * Serviço responsável por baixar arquivos do Telegram a partir de um fileId.
 *
 * <p>Principais responsabilidades: - Consultar metadados do arquivo via {@link TelegramFacade}. -
 * Realizar o download do arquivo para o sistema local. - Garantir que nunca retorne {@code null},
 * lançando {@link TelegramFileException} em caso de falha.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramFileService {

    private final TelegramFacade telegramFacade;

    /**
     * Baixa um arquivo do Telegram dado o fileId.
     *
     * @param fileId identificador único do arquivo no Telegram.
     * @return {@link File} baixado e armazenado localmente.
     * @throws TelegramFileException em caso de falha no download ou se o arquivo não existir.
     */
    public File baixarArquivo(String fileId) {
        try {
            log.debug("Baixando arquivo do Telegram fileId={}", fileId);

            org.telegram.telegrambots.meta.api.objects.File tgFile =
                    telegramFacade.getFile(new GetFile(fileId));

            File localFile = telegramFacade.downloadFile(tgFile);

            if (localFile == null || !localFile.exists()) {
                throw new TelegramFileException("Arquivo não encontrado após download", null);
            }

            return localFile;

        } catch (TelegramApiException e) {
            log.error("Erro ao baixar arquivo fileId={}", fileId, e);
            throw new TelegramFileException("Falha ao baixar arquivo do Telegram", e);
        }
    }
}
