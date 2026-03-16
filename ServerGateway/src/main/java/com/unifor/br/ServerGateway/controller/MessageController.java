package com.unifor.br.ServerGateway.controller;

import com.unifor.br.ServerGateway.service.MessageRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRoutingService routingService;

    @PostMapping
    public ResponseEntity<String> receiveAndRoute(@RequestBody Object payload) {
        log.info("Requisição recebida no gateway: {}", payload);

        boolean isDelivered = routingService.forwardMessage(payload);

        if (isDelivered) {
            return ResponseEntity.ok("Dados recebidos com sucesso.");
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Nenhum nó disponível para processar a requisição.");
        }
    }
}