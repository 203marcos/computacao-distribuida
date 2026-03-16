package com.unifor.br.ServerGateway.model;

import lombok.Data;

@Data
public class NodeHealthResponse {
	private String nodeId;
	private String role;
	private String status;
	private int userCount;
}

