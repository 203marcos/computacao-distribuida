package com.unifor.br.server_primary.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class HealthController {
    private boolean isHealthy = true;

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        if (isHealthy) {
            return ResponseEntity.ok("Servidor Online e Saudável!"); // Retorna HTTP 200
        } else {
            // Simula um erro interno do servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Simulando falha crítica no servidor!"); // Retorna HTTP 500
        }
    }
}
