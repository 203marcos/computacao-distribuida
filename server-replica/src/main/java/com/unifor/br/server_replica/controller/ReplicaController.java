package com.unifor.br.server_replica.controller;

import com.unifor.br.server_replica.model.User;
import com.unifor.br.server_replica.repository.UserRepositoryReplica;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/replica/users")
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
}