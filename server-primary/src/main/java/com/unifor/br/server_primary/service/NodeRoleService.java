package com.unifor.br.server_primary.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NodeRoleService {

    private final AtomicReference<String> role = new AtomicReference<>("REPLICA");

    @Value("${node.id}")
    private String nodeId;

    @Value("${node.role}")
    private String initialRole;

    @PostConstruct
    public void initialize() {
        updateRole(initialRole);
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getRole() {
        return role.get();
    }

    public boolean isLeader() {
        return "LEADER".equals(role.get());
    }

    public void updateRole(String newRole) {
        role.set(normalizeRole(newRole));
    }

    private String normalizeRole(String value) {
        if (value == null) {
            return "REPLICA";
        }

        return "LEADER".equals(value.trim().toUpperCase(Locale.ROOT))
                ? "LEADER"
                : "REPLICA";
    }
}

