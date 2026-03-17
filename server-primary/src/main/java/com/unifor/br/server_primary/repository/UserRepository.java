package com.unifor.br.server_primary.repository;

import com.unifor.br.server_primary.model.User;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    private final String FILE_NAME = "database.txt";

    public void save(User user) {
        try (FileWriter fw = new FileWriter(FILE_NAME, true)) {
            fw.write(user.getId() + "," + user.getName() + "," + user.getEmail() + "\n");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar no arquivo", e);
        }
    }

    public List<User> findAll() {
        Path path = Paths.get(FILE_NAME);

        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        try {
            return Files.lines(path)
                    .filter(line -> !line.trim().isEmpty()) // Evita linhas vazias
                    .map(line -> {
                        String[] parts = line.split(",");
                        return new User(
                                Long.parseLong(parts[0]),
                                parts[1],
                                parts[2]
                        );
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    //Apaga o conteúdo do arquivo para a sincronização
    public void deleteAll() {
        try {
            Files.deleteIfExists(Paths.get(FILE_NAME));
            // Cria um arquivo vazio novo
            new File(FILE_NAME).createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao limpar o banco de dados", e);
        }
    }
}