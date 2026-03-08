package com.unifor.br.server_primary.service;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.unifor.br.server_primary.model.User;
import com.unifor.br.server_primary.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    // URLs dos outros servidores
    private final String REPLICA_URL = "http://localhost:8081/replica/users";
    private final String REPLICA2_URL = "http://localhost:8083/replica/users";

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User save(User user) throws IOException {
        repository.save(user);
        // Envia para os outros servidores
        try {
            restTemplate.postForObject(REPLICA_URL, user, User.class);
            log.info("Replicated user {} to {}", user.getId(), REPLICA_URL);
        } catch (Exception e) {
            log.error("Falha ao replicar para {}: {}", REPLICA_URL, e.getMessage());
        }

        try {
            restTemplate.postForObject(REPLICA2_URL, user, User.class);
            log.info("Replicated user {} to {}", user.getId(), REPLICA2_URL);
        } catch (Exception e) {
            log.error("Falha ao replicar para {}: {}", REPLICA2_URL, e.getMessage());
        }

        return user;
    }

    public List<User> findAll() throws IOException {
        return repository.findAll();
    }
}
