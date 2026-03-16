# Sistema distribuído com replicação e eleição de líder

Projeto da atividade prática de cliente-servidor usando Java e Spring Boot.

## Estrutura

- `servercliente`: cliente de demonstração
- `ServerGateway`: gateway responsável por encaminhar as requisições e manter o líder ativo
- `server-primary`: nó inicial do cluster
- `server-replica`: réplica 1
- `server-replica2`: réplica 2

## Como o fluxo funciona

1. O cliente envia a mensagem para o gateway.
2. O gateway consulta o estado dos nós pelo endpoint `/health`.
3. A requisição é encaminhada para o líder atual.
4. O líder grava no arquivo local e replica o dado para os outros nós.
5. Se o líder cair, o gateway promove o próximo nó disponível.
6. Quando um nó volta, ele recebe sincronização e entra como réplica.

## Portas

- Gateway: `8085`
- Primária: `8080`
- Réplica 1: `8081`
- Réplica 2: `8083`
- Cliente: `8082`

## Endpoints principais

### Gateway

- `POST /users`
- `GET /health`
- `GET /leader`

### Nós do cluster

- `POST /users`
- `POST /replica/users`
- `GET /users`
- `GET /health`

## Subindo os serviços

Suba os nós nessa ordem para facilitar os testes:

```bash
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/server-replica
bash ./gradlew bootRun --no-daemon
```

```bash
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/server-replica2
bash ./gradlew bootRun --no-daemon
```

```bash
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/server-primary
bash ./gradlew bootRun --no-daemon
```

```bash
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/ServerGateway
bash ./gradlew bootRun --no-daemon
```

## Enviando uma mensagem pelo cliente

O cliente já faz um envio simples no `main`.

```bash
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/servercliente
bash ./gradlew bootRun --no-daemon
```

Se preferir testar manualmente:

```bash
curl -H 'Content-Type: application/json' \
  -d '{"id":1,"name":"teste","email":"teste@hotmail.com"}' \
  http://localhost:8085/users
```

## Consultas rápidas

```bash
curl http://localhost:8085/health
curl http://localhost:8085/leader
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8083/health
```

## Cenários de teste

### 1. Fluxo normal

Com todos os serviços no ar, envie uma mensagem e confira os arquivos:

- `database.txt`
- `database-replica.txt`
- `database-replica2.txt`

### 2. Queda da primária

Derrube a primária e consulte o líder atual:

```bash
fuser -k 8080/tcp
curl http://localhost:8085/leader
```

A liderança deve passar para a `replica1`. Se ela também estiver indisponível, a `replica2` assume.

### 3. Envio após failover

Com o novo líder ativo, envie outra mensagem pelo gateway. O sistema deve continuar aceitando escrita normalmente.

### 4. Retorno da primária antiga

Suba novamente o nó da primária. Ele deve voltar como réplica e receber sincronização automática.

## Rodando os testes

```bash
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/server-primary && bash ./gradlew test --no-daemon
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/server-replica && bash ./gradlew test --no-daemon
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/server-replica2 && bash ./gradlew test --no-daemon
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/ServerGateway && bash ./gradlew test --no-daemon
cd /home/marcos-dias/Documents/distribuidora/computacao-distribuida/servercliente && bash ./gradlew test --no-daemon
```

## Observações

- A eleição foi implementada por prioridade: `primary` -> `replica1` -> `replica2`.
- O gateway mantém o líder atual enquanto ele estiver saudável.
- A sincronização entre nós é feita por snapshot completo da lista de usuários.

