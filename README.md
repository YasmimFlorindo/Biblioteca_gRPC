# Sistema de Biblioteca com gRPC

Projeto composto por:

- `server-java`: servidor Java usando gRPC + SQLite.
- `client-python`: cliente Python/Tkinter com telas de usuários, gêneros, livros, empréstimos, devoluções e multas.
- `proto/biblioteca.proto`: contrato de comunicação.

## Arquitetura

```text
┌──────────────────────────┐
│ Cliente Python           │
│ Tkinter                  │
│ biblioteca_pb2_grpc.py   │
└────────────┬─────────────┘
             │
             │ gRPC / HTTP/2
             │ localhost:50051
             ▼
┌──────────────────────────┐
│ Servidor Java            │
│ BibliotecaServiceImpl    │
│                          │
│ Regras de empréstimo     │
│ Regras de multa          │
│ Acesso ao SQLite         │
└────────────┬─────────────┘
             ▼
       biblioteca_server.db
```

## Instalações

### 1. Java
Instale um JDK 17 ou superior e configure o `PATH`.

Teste:

```powershell
java -version
```

### 2. Maven
Instale o Apache Maven e teste:

```powershell
mvn -version
```

### 3. VS Code
Extensões recomendadas:

- Extension Pack for Java
- Python
- Pylance

### 4. Python/gRPC

Dentro de `client-python`:

```powershell
python -m pip install -r requirements.txt
```

## Gerar os arquivos gRPC do cliente

Dentro de `client-python`:

```powershell
python -m grpc_tools.protoc -I../proto --python_out=. --grpc_python_out=. ../proto/biblioteca.proto
```

Serão gerados:

```text
biblioteca_pb2.py
biblioteca_pb2_grpc.py
```

## Executar o servidor

No terminal:

```powershell
cd server-java
mvn clean compile
mvn exec:java
```

O servidor ficará em:

```text
localhost:50051
```

## Executar o cliente

Abra outro terminal:

```powershell
cd client-python
python cliente.py
```

## Teste principal exigido pelo trabalho

A funcionalidade de consulta de livro é um exemplo de operação real no servidor:

```text
Cliente Python
    |
    | ConsultarLivro(id=1)
    v
Servidor Java
    |
    | SELECT no SQLite
    v
Livro
    |
    | resposta gRPC
    v
Cliente Python
```

O servidor também executa as regras de negócio de empréstimo, devolução e multas.

A multa é:

```text
R$ 1,00 por dia de atraso
```

e o servidor bloqueia novos empréstimos quando o usuário possui multa pendente.
