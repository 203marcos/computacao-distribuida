package com.unifor.br.ServerGateway.controller;

import com.unifor.br.ServerGateway.service.MessageRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GatewayStatusController {

    private final MessageRoutingService routingService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return routingService.gatewayHealth();
    }

    @GetMapping("/leader")
    public Map<String, Object> leader() {
        return routingService.leaderStatus();
    }
}

