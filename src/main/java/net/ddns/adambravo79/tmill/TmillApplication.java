/* (c) 2026 */
package net.ddns.adambravo79.tmill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync // Habilita o @Async do AudioService
@SpringBootApplication(scanBasePackages = "net.ddns.adambravo79.tmill")
public class TmillApplication {
    public static void main(String[] args) {
        SpringApplication.run(TmillApplication.class, args);
    }
}
