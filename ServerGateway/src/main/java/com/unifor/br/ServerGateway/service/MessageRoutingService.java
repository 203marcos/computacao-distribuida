package com.unifor.br.ServerGateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRoutingService {

    private final RestTemplate restTemplate;

    @Value("${routing.primary-server-url}")
    private String primaryServerUrl;

    @Value("${routing.backup-server-urls}")
    private List<String> backupServerUrls;

    // Agora recebe um Object genérico, que representa qualquer JSON
    public boolean forwardMessage(Object payload) {

        // 1. Tenta enviar para o servidor principal
        try {
            log.info("A tentar rotear para o servidor principal: {}", primaryServerUrl);
            // Repassa para a rota /users do destino
            restTemplate.postForEntity(primaryServerUrl + "/users", payload, String.class);
            log.info("Mensagem entregue ao servidor principal com sucesso.");
            return true;
        } catch (RestClientException e) {
            log.warn("Falha ao contactar o servidor principal. Erro: {}", e.getMessage());
        }

        // 2. Se o principal falhar, tenta os backups em ordem
        for (String backupUrl : backupServerUrls) {
            try {
                log.info("A tentar rotear para o servidor de backup: {}", backupUrl);
                restTemplate.postForEntity(backupUrl + "/users", payload, String.class);
                log.info("Mensagem entregue com sucesso ao backup: {}", backupUrl);
                return true;
            } catch (RestClientException e) {
                log.warn("Servidor de backup {} indisponível. Erro: {}", backupUrl, e.getMessage());
            }
        }

        // 3. Se todos falharem
        log.error("Todos os servidores (principal e backups) estão offline.");
        return false;
    }
}