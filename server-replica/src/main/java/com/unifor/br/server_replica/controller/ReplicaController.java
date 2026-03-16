package com.unifor.br.server_replica.controller;

import com.unifor.br.server_replica.model.User;
import com.unifor.br.server_replica.service.NodeRoleService;
import com.unifor.br.server_replica.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ReplicaController {

    private final UserService service;
    private final NodeRoleService nodeRoleService;

    public ReplicaController(UserService service, NodeRoleService nodeRoleService) {
        this.service = service;
        this.nodeRoleService = nodeRoleService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("nodeId", nodeRoleService.getNodeId());
        response.put("role", nodeRoleService.getRole());
        response.put("status", "UP");
        response.put("userCount", service.countUsers());
        return response;
    }

    @PostMapping("/users")
    public User create(@RequestBody User user) throws IOException {
        return service.save(user);
    }

    @PostMapping("/replica/users")
    public User replicate(@RequestBody User user) throws IOException {
        return service.saveReplica(user);
    }

    @GetMapping({"/users", "/replica/users"})
    public List<User> list() throws IOException {
        return service.findAll();
    }

    @PostMapping("/internal/role")
    public Map<String, String> updateRole(@RequestBody Map<String, String> request) {
        nodeRoleService.updateRole(request.get("role"));

        Map<String, String> response = new LinkedHashMap<>();
        response.put("nodeId", nodeRoleService.getNodeId());
        response.put("role", nodeRoleService.getRole());
        return response;
    }

    @PostMapping("/internal/sync")
    public Map<String, Object> sync(@RequestBody List<User> users) throws IOException {
        service.replaceAll(users);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("nodeId", nodeRoleService.getNodeId());
        response.put("syncedUsers", users.size());
        response.put("role", nodeRoleService.getRole());
        return response;
    }
}