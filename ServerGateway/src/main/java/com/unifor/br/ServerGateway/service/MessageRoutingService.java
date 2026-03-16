package com.unifor.br.ServerGateway.service;

import com.unifor.br.ServerGateway.model.NodeHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRoutingService {

    private final RestTemplate restTemplate;

    @Value("${routing.primary-server-url}")
    private String primaryServerUrl;

    @Value("${routing.replica1-server-url}")
    private String replica1ServerUrl;

    @Value("${routing.replica2-server-url}")
    private String replica2ServerUrl;

    private volatile String currentLeaderId;

    @Scheduled(fixedDelayString = "${routing.reconcile-delay-ms:3000}")
    public void scheduledClusterReconciliation() {
        reconcileCluster();
    }

    public boolean forwardMessage(Object payload) {
        ClusterNode leader = reconcileCluster();
        if (leader == null) {
            log.error("Nenhum líder disponível para receber a requisição.");
            return false;
        }

        try {
            routeToNode(payload, leader);
            return true;
        } catch (RestClientException e) {
            log.warn("Falha ao enviar para o líder {}: {}", leader.nodeId(), e.getMessage());
            currentLeaderId = null;
        }

        ClusterNode fallbackLeader = reconcileCluster();
        if (fallbackLeader == null) {
            return false;
        }

        try {
            routeToNode(payload, fallbackLeader);
            return true;
        } catch (RestClientException e) {
            log.error("Todos os servidores do cluster estão indisponíveis: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> gatewayHealth() {
        ClusterNode leader = reconcileCluster();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", leader == null ? "DEGRADED" : "UP");
        response.put("leaderId", leader == null ? null : leader.nodeId());
        response.put("leaderUrl", leader == null ? null : leader.url());
        response.put("clusterSize", clusterNodes().size());
        return response;
    }

    public Map<String, Object> leaderStatus() {
        ClusterNode leader = reconcileCluster();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("leaderId", leader == null ? null : leader.nodeId());
        response.put("leaderUrl", leader == null ? null : leader.url());
        response.put("status", leader == null ? "UNAVAILABLE" : "ACTIVE");
        return response;
    }

    private synchronized ClusterNode reconcileCluster() {
        Map<ClusterNode, NodeHealthResponse> healthyNodes = new LinkedHashMap<>();

        for (ClusterNode node : clusterNodes()) {
            NodeHealthResponse health = fetchHealth(node);
            if (health != null && "UP".equalsIgnoreCase(health.getStatus())) {
                healthyNodes.put(node, health);
            }
        }

        if (healthyNodes.isEmpty()) {
            currentLeaderId = null;
            return null;
        }

        ClusterNode leader = resolveLeader(healthyNodes.keySet());
        currentLeaderId = leader.nodeId();

        applyRoles(healthyNodes.keySet(), leader);
        synchronizeReplicas(healthyNodes.keySet(), leader);
        return leader;
    }

    private ClusterNode resolveLeader(Iterable<ClusterNode> healthyNodes) {
        if (currentLeaderId != null) {
            for (ClusterNode node : healthyNodes) {
                if (node.nodeId().equals(currentLeaderId)) {
                    return node;
                }
            }
        }

        return Optional.ofNullable(toPriorityLeader(healthyNodes))
                .orElseThrow(() -> new IllegalStateException("Nenhum nó saudável encontrado para eleição."));
    }

    private ClusterNode toPriorityLeader(Iterable<ClusterNode> healthyNodes) {
        return toList(healthyNodes).stream()
                .min(Comparator.comparingInt(ClusterNode::priority))
                .orElse(null);
    }

    private List<ClusterNode> toList(Iterable<ClusterNode> nodes) {
        return nodes instanceof List<ClusterNode> list ? list : List.copyOf((java.util.Collection<ClusterNode>) nodes);
    }

    private void applyRoles(Iterable<ClusterNode> healthyNodes, ClusterNode leader) {
        for (ClusterNode node : healthyNodes) {
            String role = node.nodeId().equals(leader.nodeId()) ? "LEADER" : "REPLICA";

            try {
                restTemplate.postForEntity(node.url() + "/internal/role", Map.of("role", role), Void.class);
            } catch (RestClientException e) {
                log.warn("Falha ao atualizar papel do nó {}: {}", node.nodeId(), e.getMessage());
            }
        }
    }

    private void synchronizeReplicas(Iterable<ClusterNode> healthyNodes, ClusterNode leader) {
        List<?> snapshot;

        try {
            snapshot = restTemplate.getForObject(leader.url() + "/users", List.class);
        } catch (RestClientException e) {
            log.warn("Falha ao obter snapshot do líder {}: {}", leader.nodeId(), e.getMessage());
            return;
        }

        if (snapshot == null) {
            return;
        }

        for (ClusterNode node : healthyNodes) {
            if (node.nodeId().equals(leader.nodeId())) {
                continue;
            }

            try {
                restTemplate.postForEntity(node.url() + "/internal/sync", snapshot, Void.class);
            } catch (RestClientException e) {
                log.warn("Falha ao sincronizar nó {}: {}", node.nodeId(), e.getMessage());
            }
        }
    }

    private NodeHealthResponse fetchHealth(ClusterNode node) {
        try {
            return restTemplate.getForObject(node.url() + "/health", NodeHealthResponse.class);
        } catch (RestClientException e) {
            log.warn("Nó {} indisponível para health check: {}", node.nodeId(), e.getMessage());
            return null;
        }
    }

    private void routeToNode(Object payload, ClusterNode leader) {
        log.info("Roteando requisição para o líder ativo {} ({})", leader.nodeId(), leader.url());
        restTemplate.postForEntity(leader.url() + "/users", payload, String.class);
    }

    private List<ClusterNode> clusterNodes() {
        return List.of(
                new ClusterNode("primary", primaryServerUrl, 0),
                new ClusterNode("replica1", replica1ServerUrl, 1),
                new ClusterNode("replica2", replica2ServerUrl, 2)
        );
    }

    private record ClusterNode(String nodeId, String url, int priority) {
    }
}