package com.unifor.br.server_replica.repository;

import com.unifor.br.server_replica.model.User;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class UserRepositoryReplica {

    private static final Path FILE_PATH = Paths.get("database-replica2.txt");

    public synchronized void save(User user) throws IOException {
        Files.writeString(
                FILE_PATH,
                serialize(user) + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    public synchronized void replaceAll(List<User> users) throws IOException {
        List<String> lines = users.stream()
                .map(this::serialize)
                .collect(Collectors.toList());

        Files.write(
                FILE_PATH,
                lines,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    public synchronized List<User> findAll() throws IOException {
        if (!Files.exists(FILE_PATH)) {
            return new ArrayList<>();
        }

        try (Stream<String> lines = Files.lines(FILE_PATH)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::deserialize)
                    .collect(Collectors.toList());
        }
    }

    private String serialize(User user) {
        return user.getId() + "," + user.getName() + "," + user.getEmail();
    }

    private User deserialize(String line) {
        String[] parts = line.split(",", 3);
        return new User(
                Long.parseLong(parts[0]),
                parts[1],
                parts[2]
        );
    }
}
