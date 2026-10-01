# RELATÓRIO — SISTEMA DE BIBLIOTECA COM gRPC

## 1. Introdução

Este projeto apresenta a adaptação de um sistema de biblioteca para uma arquitetura cliente-servidor utilizando gRPC.

O cliente foi desenvolvido em Python com Tkinter e possui as telas de interação com o usuário. O servidor foi desenvolvido em Java, atendendo ao requisito de que o servidor não seja implementado na mesma linguagem da aplicação cliente.

A comunicação entre as duas aplicações é realizada por gRPC. O contrato das operações é definido no arquivo `biblioteca.proto`, utilizando Protocol Buffers.

## 2. Arquitetura

A arquitetura é composta por três partes:

1. Cliente Python/Tkinter;
2. Servidor Java/gRPC;
3. Banco SQLite utilizado pelo servidor.

O cliente não acessa diretamente o banco de dados. Quando uma operação precisa de informação ou alteração, o cliente envia uma requisição gRPC ao servidor.

Fluxo:

```text
Usuário
   |
   v
Interface Tkinter
   |
   v
Stub gRPC do Python
   |
   | gRPC / HTTP/2
   v
Servidor Java
   |
   v
BibliotecaServiceImpl
   |
   v
SQLite
```

## 3. Tecnologias

### Cliente

- Python
- Tkinter
- grpcio
- grpcio-tools
- Protocol Buffers

### Servidor

- Java
- gRPC Java
- Maven
- SQLite JDBC
- Protocol Buffers

## 4. Definição do serviço

O arquivo `biblioteca.proto` define o serviço:

```proto
service BibliotecaService {
  rpc CadastrarUsuario(CadastrarUsuarioRequest) returns (UsuarioResponse);
  rpc CadastrarGenero(CadastrarGeneroRequest) returns (GeneroResponse);
  rpc CadastrarLivro(CadastrarLivroRequest) returns (LivroResponse);

  rpc ListarUsuarios(Empty) returns (ListaUsuariosResponse);
  rpc ListarLivros(Empty) returns (ListaLivrosResponse);

  rpc ConsultarLivro(ConsultarLivroRequest) returns (LivroResponse);
  rpc ConsultarMultaUsuario(ConsultarMultaUsuarioRequest) returns (MultaUsuarioResponse);

  rpc RealizarEmprestimo(EmprestimoRequest) returns (OperacaoResponse);
  rpc DevolverLivro(DevolucaoRequest) returns (DevolucaoResponse);
  rpc QuitarMulta(QuitarMultaRequest) returns (OperacaoResponse);
}
```

O arquivo `.proto` funciona como contrato entre cliente e servidor.

## 5. Geração do código

A partir do arquivo `.proto`, o compilador Protocol Buffers e os plugins gRPC geram os códigos necessários para a comunicação.

No cliente Python são gerados:

```text
biblioteca_pb2.py
biblioteca_pb2_grpc.py
```

O primeiro contém as classes das mensagens Protocol Buffers e o segundo contém o stub usado para chamar os métodos remotos.

No servidor Java, o Maven executa a geração durante a compilação e cria as classes Java correspondentes.

## 6. Comunicação

O cliente cria um canal:

```python
channel = grpc.insecure_channel("localhost:50051")
```

e cria o stub:

```python
stub = BibliotecaServiceStub(channel)
```

Depois pode chamar uma operação remota:

```python
resposta = stub.ConsultarLivro(
    biblioteca_pb2.ConsultarLivroRequest(livro_id=1)
)
```

Embora a chamada tenha aparência semelhante a uma função local, ela é enviada para o servidor por gRPC.

## 7. Funcionalidade além de listar nomes

O sistema possui várias funcionalidades que realmente são executadas pelo servidor.

Um exemplo é:

```text
ConsultarLivro(id)
```

O cliente envia o ID do livro e o servidor consulta o banco de dados e retorna:

- ID;
- título;
- autor;
- gênero;
- disponibilidade.

Portanto, a comunicação não é apenas uma troca de nomes de alunos.

## 8. Empréstimos

Para realizar um empréstimo, o cliente envia:

```text
usuario_id
livro_id
```

O servidor verifica:

1. se o usuário existe;
2. se o usuário possui multas pendentes;
3. se o livro existe;
4. se o livro está disponível.

Se houver multa pendente, o servidor bloqueia o empréstimo.

## 9. Multas

O sistema considera sete dias para o empréstimo.

Quando o livro é devolvido, o servidor compara a data atual com a data prevista.

A regra é:

```text
valor da multa = dias de atraso × R$ 1,00
```

Exemplo:

```text
Dias de atraso: 4

Multa:
4 × R$ 1,00 = R$ 4,00
```

A multa é registrada no banco de dados pelo servidor.

## 10. Quitação

O cliente pode solicitar a quitação de uma multa informando o ID da multa.

O servidor altera o estado da multa para paga.

Depois da quitação, o valor deixa de ser considerado na verificação de bloqueio de empréstimos.

## 11. Ausência de comunicação direta por sockets

O cliente não implementa sockets TCP manualmente.

A comunicação é realizada pela camada gRPC. O gRPC utiliza HTTP/2 para o transporte e Protocol Buffers para serialização das mensagens.

Dessa forma, a comunicação de rede necessária ao sistema fica encapsulada nas chamadas RPC.

## 12. Execução

Primeiramente o servidor deve ser iniciado:

```powershell
cd server-java
mvn clean compile
mvn exec:java
```

Depois o cliente é executado em outro terminal:

```powershell
cd client-python
python cliente.py
```

O servidor escuta a porta:

```text
50051
```

## 13. Teste de comunicação

Um teste pode ser realizado cadastrando um livro e posteriormente consultando-o pela tela de livros.

O cliente envia:

```text
ConsultarLivro(id=1)
```

O servidor executa a consulta no SQLite e retorna os dados do livro.

Outro teste é realizar um empréstimo para um usuário que possui multa pendente. Nesse caso, a decisão é tomada pelo servidor e o cliente recebe uma mensagem informando que o empréstimo foi bloqueado.

## 14. Conclusão

A implementação atende à arquitetura cliente-servidor proposta utilizando duas linguagens diferentes: Python no cliente e Java no servidor.

O gRPC estabelece um contrato comum por meio do Protocol Buffers, permitindo que aplicações escritas em linguagens diferentes troquem mensagens de maneira estruturada.

Além da consulta de informações, o servidor concentra regras de negócio importantes do sistema, como empréstimos, devoluções, cálculo de multas e bloqueio de usuários com pendências.

Assim, o projeto demonstra uma aplicação prática de comunicação RPC em uma aplicação de biblioteca.
