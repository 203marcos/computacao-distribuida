package com.unifor.br.server_replica.service;

import com.unifor.br.server_replica.model.User;
import com.unifor.br.server_replica.repository.UserRepositoryReplica;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Service
public class UserService {

    private final UserRepositoryReplica repository;
    private final RestTemplate restTemplate = new RestTemplate();

    // URLs dos outros servidores
    private final String PRIMARY_URL = "http://localhost:8080/replica/users";
    private final String REPLICA_URL = "http://localhost:8081/replica/users";

    public UserService(UserRepositoryReplica repository) {
        this.repository = repository;
    }

    public User save(User user) throws IOException {
        repository.save(user);
        // Envia para os outros servidores
        restTemplate.postForObject(PRIMARY_URL, user, User.class);
        restTemplate.postForObject(REPLICA_URL, user, User.class);
        return user;
    }

    public List<User> findAll() throws IOException {
        return repository.findAll();
    }
}
