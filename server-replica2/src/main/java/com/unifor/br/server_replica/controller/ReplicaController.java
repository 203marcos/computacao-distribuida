package com.unifor.br.server_replica.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unifor.br.server_replica.model.User;
import com.unifor.br.server_replica.repository.UserRepositoryReplica;

@RestController
@RequestMapping("/replica2/users")
public class ReplicaController {

    private final UserRepositoryReplica repository;

    public ReplicaController(UserRepositoryReplica repository) {
        this.repository = repository;
    }

    @PostMapping
    public User replicate(@RequestBody User user) throws IOException {
        repository.save(user);
        return user;
    }

    @GetMapping
    public List<User> list() throws IOException {
        return repository.findAll();
    }
}