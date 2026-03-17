package com.unifor.br.ServerGateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRoutingService {

    private final RestTemplate restTemplate;

    @Value("${routing.primary-server-url}")
    private String truePrimaryUrl;

    @Value("${routing.backup-server-urls}")
    private List<String> trueBackupUrls;

    @Value("${routing.primary-server-path}")
    private String primaryServerPath;

    @Value("${routing.backup-server-paths}")
    private List<String> backupServerPaths;

    // Controlam o estado atual da rede
    private String activePrimaryUrl;
    private List<String> onlineBackups = new ArrayList<>();

    //Map que guarda a fila de mensagens atrasadas de cada servidor
    private Map<String, List<Object>> filasDeEspera = new ConcurrentHashMap<>();

    private String getPathForUrl(String serverUrl) {
        if (serverUrl.equals(truePrimaryUrl)) {
            return primaryServerPath;
        }
        int index = trueBackupUrls.indexOf(serverUrl);
        if (index >= 0 && index < backupServerPaths.size()) {
            return backupServerPaths.get(index);
        }
        return "/users";
    }

    @PostConstruct
    public void init() {
        activePrimaryUrl = truePrimaryUrl;
        log.info("🚀 Gateway Iniciado! O Servidor Principal padrão assumiu o posto: {}", activePrimaryUrl);


        filasDeEspera.put(truePrimaryUrl, new CopyOnWriteArrayList<>());
        for (String backup : trueBackupUrls) {
            filasDeEspera.put(backup, new CopyOnWriteArrayList<>());
        }
    }

    // Roda a cada 5 segundos (5000 milissegundos) para checar a saúde
    @Scheduled(fixedRate = 5000)
    public void monitorServerHealth() {
        // Verifica o Principal Verdadeiro
        boolean isTruePrimaryOnline = pingServer(truePrimaryUrl);

        // Verifica os Backups e atualiza a lista dos que estão online
        List<String> currentOnlineBackups = new ArrayList<>();
        for (String backupUrl : trueBackupUrls) {
            if (pingServer(backupUrl)) {
                currentOnlineBackups.add(backupUrl);
            }
        }
        this.onlineBackups = currentOnlineBackups;

        // Lógica de Eleição e Failback
        if (isTruePrimaryOnline) {
            if (!truePrimaryUrl.equals(activePrimaryUrl)) {
                log.info("🌟 RECUPERAÇÃO: O Principal original ({}) voltou online! Retomando o posto.", truePrimaryUrl);
            }
            activePrimaryUrl = truePrimaryUrl;
        } else {
            if (activePrimaryUrl == null || activePrimaryUrl.equals(truePrimaryUrl) || !onlineBackups.contains(activePrimaryUrl)) {
                if (!onlineBackups.isEmpty()) {
                    activePrimaryUrl = onlineBackups.get(0);
                    log.warn("⚠️ FAILOVER: Principal offline! Elegendo o backup ({}) como novo Líder.", activePrimaryUrl);
                } else {
                    if (activePrimaryUrl != null) {
                        log.error("🚨 CRÍTICO: Todos os servidores estão offline (HTTP 500 ou indisponíveis)!");
                    }
                    activePrimaryUrl = null;
                }
            }
        }

        // Tenta enviar mensagens paradas na fila para quem estiver online
        sincronizarServidor(truePrimaryUrl);
        for (String backupUrl : trueBackupUrls) {
            sincronizarServidor(backupUrl);
        }
    }

    // Retira da fila e envia
    private void sincronizarServidor(String serverUrl) {
        List<Object> fila = filasDeEspera.get(serverUrl);

        if (fila != null && !fila.isEmpty() && pingServer(serverUrl)) {
            log.info("🔄 INICIANDO SINCRONIZAÇÃO: Enviando {} mensagens atrasadas para {}...", fila.size(), serverUrl);
            List<Object> mensagensEntregues = new ArrayList<>();

            for (Object msg : fila) {
                try {
                    String path = getPathForUrl(serverUrl);
                    restTemplate.postForEntity(serverUrl + path, msg, String.class);
                    mensagensEntregues.add(msg);
                } catch (Exception e) {
                    log.warn("⚠️ Falha ao sincronizar mensagem com {}. Pausando sync. Tentaremos no próximo ciclo.", serverUrl);
                    break;
                }
            }
            // Limpa da fila apenas as que deram certo
            fila.removeAll(mensagensEntregues);

            if (fila.isEmpty()) {
                log.info("✅ Sincronização concluída! O servidor {} está 100% atualizado e idêntico aos demais.", serverUrl);
            }
        }
    }

    private boolean pingServer(String serverUrl) {
        try {
            restTemplate.getForEntity(serverUrl + "/health", String.class);
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }


    public boolean forwardMessage(Object payload) {
        java.util.Set<String> todosOsServidores = new java.util.LinkedHashSet<>();
        todosOsServidores.add(truePrimaryUrl);
        todosOsServidores.addAll(trueBackupUrls);

        log.info("📩 Nova mensagem chegou! Distribuindo para toda a rede...");

        for (String serverUrl : todosOsServidores) {
            try {
                String path = getPathForUrl(serverUrl);
                restTemplate.postForEntity(serverUrl + path, payload, String.class);
                log.info("✅ Entregue ao vivo para: {}{}", serverUrl, path);
            } catch (Exception e) {
                log.warn("⚠️ Erro ao entregar ao vivo para {} (Motivo: {}). Guardando na Fila de Espera...", serverUrl, e.getMessage());

                // Salva na fila para tentar de novo mais tarde
                filasDeEspera.get(serverUrl).add(payload);
            }
        }

        return true;
    }
}