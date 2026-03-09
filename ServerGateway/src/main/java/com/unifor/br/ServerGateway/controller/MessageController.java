package com.unifor.br.ServerGateway.controller;

import com.unifor.br.ServerGateway.service.MessageRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Importante
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j // Adicionando o Lombok para logs
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRoutingService routingService;

    @PostMapping
    public ResponseEntity<String> receiveAndRoute(@RequestBody Object payload) {

        // Essa linha vai imprimir no console assim que a mensagem bater no Roteador!
        log.info(">>> MENSAGEM RECEBIDA NO ROTEADOR! Conteúdo: {}", payload);

        boolean isDelivered = routingService.forwardMessage(payload);

        if (isDelivered) {
            return ResponseEntity.ok("Dados recebidos e roteados com sucesso.");
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Falha no roteamento: Todos os servidores de destino estão offline.");
        }
    }
}