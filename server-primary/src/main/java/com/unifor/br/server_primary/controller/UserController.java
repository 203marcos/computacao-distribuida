package com.unifor.br.server_primary.controller;

import com.unifor.br.server_primary.model.User;
import com.unifor.br.server_primary.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public User create(@RequestBody User user) throws IOException {
        return service.save(user);
    }

    @GetMapping
    public List<User> list() throws IOException {
        return service.findAll();
    }
}