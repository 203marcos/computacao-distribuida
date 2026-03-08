package com.unifor.br.server_primary.service;

import com.unifor.br.server_primary.model.User;
import com.unifor.br.server_primary.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Service
public class UserService {

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
        restTemplate.postForObject(REPLICA_URL, user, User.class);
        restTemplate.postForObject(REPLICA2_URL, user, User.class);
        return user;
    }

    public List<User> findAll() throws IOException {
        return repository.findAll();
    }
}
