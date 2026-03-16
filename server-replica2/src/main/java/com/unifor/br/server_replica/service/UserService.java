package com.unifor.br.server_replica.service;

import com.unifor.br.server_replica.model.User;
import com.unifor.br.server_replica.repository.UserRepositoryReplica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepositoryReplica repository;
    private final NodeRoleService nodeRoleService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${node.self-url}")
    private String selfUrl;

    @Value("${cluster.node-urls}")
    private List<String> clusterNodeUrls;

    public UserService(UserRepositoryReplica repository, NodeRoleService nodeRoleService) {
        this.repository = repository;
        this.nodeRoleService = nodeRoleService;
    }

    public User save(User user) throws IOException {
        ensureLeader();
        repository.save(user);
        replicateToPeers(user);
        return user;
    }

    public User saveReplica(User user) throws IOException {
        repository.save(user);
        return user;
    }

    public List<User> findAll() throws IOException {
        return repository.findAll();
    }

    public void replaceAll(List<User> users) throws IOException {
        repository.replaceAll(users);
    }

    public int countUsers() throws IOException {
        return findAll().size();
    }

    private void replicateToPeers(User user) {
        for (String nodeUrl : clusterNodeUrls) {
            if (nodeUrl.equalsIgnoreCase(selfUrl)) {
                continue;
            }

            try {
                restTemplate.postForEntity(nodeUrl + "/replica/users", user, Void.class);
                log.info("Replicated user {} to {}", user.getId(), nodeUrl);
            } catch (RestClientException e) {
                log.warn("Falha ao replicar para {}: {}", nodeUrl, e.getMessage());
            }
        }
    }

    private void ensureLeader() {
        if (!nodeRoleService.isLeader()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este nó está atuando como réplica.");
        }
    }
}
