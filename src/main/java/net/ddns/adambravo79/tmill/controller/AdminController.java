/* (c) 2026 | 06/05/2026 */
package net.ddns.adambravo79.tmill.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.ddns.adambravo79.tmill.service.EasterEggService;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final EasterEggService easterEggService;

    @PostMapping("/reload-easter-eggs")
    public ResponseEntity<String> reloadEasterEggs() {
        easterEggService.reload();
        return ResponseEntity.ok("Easter eggs recarregados");
    }
}
