package com.unifor.br.server_primary.service;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.unifor.br.server_primary.model.User;
import com.unifor.br.server_primary.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;

    // Construtor
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    // O método save agora faz APENAS o trabalho dele: salvar no próprio banco!
    public User save(User user) throws IOException {
        repository.save(user);
        log.info("🎯 Usuário {} salvo com sucesso no banco de dados do Servidor Principal!", user.getId());

        // Retiramos toda a lógica de RestTemplate e URLs daqui.
        // O Gateway já fez isso por nós antes de a mensagem chegar aqui!
        return user;
    }

    public List<User> findAll() throws IOException {
        return repository.findAll();
    }
}