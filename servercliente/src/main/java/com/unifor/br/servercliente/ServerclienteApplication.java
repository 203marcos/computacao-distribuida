package com.unifor.br.servercliente;

import com.unifor.br.servercliente.model.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ServerclienteApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerclienteApplication.class, args);

		String url = "http://localhost:8085/users";

		RestTemplate restTemplate = new RestTemplate();

		User usuario = new User(1L,"teste", "teste@hotmail.com");
		restTemplate.postForObject(url, usuario, String.class);
		System.out.println("Mensagem enviada ao gateway.");
	}

}

