/* (c) 2026 | 27/04/2026 */
package net.ddns.adambravo79.tmill.config;

import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

@Configuration
public class AppConfig {

    @Value("${bot.token}")
    private String botToken;

    @Bean
    public String botToken() {
        return this.botToken;
    }

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        // Isso força o Spring a usar Virtual Threads para qualquer @Async
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
