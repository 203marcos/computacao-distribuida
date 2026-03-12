package com.unifor.br.server_replica.repository;

import com.unifor.br.server_replica.model.User;
import org.springframework.stereotype.Repository;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryReplica {

    private final String FILE_NAME = "E:\\Projetos\\CD\\database-replica.txt";

    public void save(User user) throws IOException {
        FileWriter fw = new FileWriter(FILE_NAME, true);
        fw.write(user.getId() + "," + user.getName() + "," + user.getEmail() + "\n");
        fw.close();
    }

    public List<User> findAll() throws IOException {
        Path path = Paths.get(FILE_NAME);

        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        return Files.lines(path)
                .map(line -> {
                    String[] parts = line.split(",");
                    return new User(
                            Long.parseLong(parts[0]),
                            parts[1],
                            parts[2]
                    );
                })
                .collect(Collectors.toList());
    }
}
