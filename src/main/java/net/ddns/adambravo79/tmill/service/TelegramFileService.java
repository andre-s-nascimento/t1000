package net.ddns.adambravo79.tmill.service;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Slf4j
@Service
public class TelegramFileService {

    public Optional<File> baixarArquivo(TelegramClient telegramClient, String fileId) {
        try {
            var telegramFile = telegramClient.execute(new GetFile(fileId));
            File localTemp = telegramClient.downloadFile(telegramFile);

            File finalFile = new File("temp_audio/" + localTemp.getName() + ".oga");
            Files.createDirectories(finalFile.getParentFile().toPath());
            Files.move(localTemp.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return Optional.of(finalFile);
        } catch (Exception e) {
            log.error("Erro ao baixar arquivo do Telegram", e);
            return Optional.empty();
        }
    }
}