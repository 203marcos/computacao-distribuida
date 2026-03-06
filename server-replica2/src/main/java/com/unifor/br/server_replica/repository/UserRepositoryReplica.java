package com.unifor.br.server_replica.repository;

import com.unifor.br.server_replica.model.User;
import org.springframework.stereotype.Repository;

import java.io.FileWriter;
import java.io.IOException;

@Repository
public class UserRepositoryReplica {

    private final String FILE_NAME = "database-replica.txt";

    public void save(User user) throws IOException {
        FileWriter fw = new FileWriter(FILE_NAME, true);
        fw.write(user.getId() + "," + user.getName() + "," + user.getEmail() + "\n");
        fw.close();
    }
}
