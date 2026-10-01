# Cliente Python da Biblioteca

Este cliente contém as telas em Tkinter e usa gRPC para conversar com o servidor Java.

## Instalação

No terminal do VS Code:

```powershell
python -m pip install -r requirements.txt
```

Depois gere os arquivos gRPC a partir do `.proto`:

```powershell
python -m grpc_tools.protoc -I../proto --python_out=. --grpc_python_out=. ../proto/biblioteca.proto
```

Isso deve criar:

- `biblioteca_pb2.py`
- `biblioteca_pb2_grpc.py`

## Execução

Primeiro inicie o servidor Java.

Depois:

```powershell
python cliente.py
```

O cliente conecta em `localhost:50051`.
