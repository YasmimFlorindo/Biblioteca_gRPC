import tkinter as tk
from tkinter import ttk, messagebox
import grpc

import biblioteca_pb2 as pb
import biblioteca_pb2_grpc as rpc


class BibliotecaClient:
    def __init__(self):
        self.channel = grpc.insecure_channel("localhost:50051")
        self.stub = rpc.BibliotecaServiceStub(self.channel)


cliente = BibliotecaClient()

ROXO = "#6C4AB6"
ROXO_ESCURO = "#4B2E83"
FUNDO = "#F4F4F8"
BRANCO = "#FFFFFF"

janela = tk.Tk()
janela.title("Biblioteca - Cliente gRPC")
janela.geometry("1150x680")
janela.configure(bg=FUNDO)

menu = tk.Frame(janela, bg=ROXO_ESCURO, width=220)
menu.pack(side="left", fill="y")
menu.pack_propagate(False)

conteudo = tk.Frame(janela, bg=FUNDO)
conteudo.pack(side="right", fill="both", expand=True)


def limpar():
    for w in conteudo.winfo_children():
        w.destroy()


def titulo(texto):
    tk.Label(conteudo, text=texto, font=("Arial", 24, "bold"),
             bg=FUNDO, fg=ROXO_ESCURO).pack(anchor="w", padx=30, pady=25)


def erro_grpc(e):
    messagebox.showerror("Erro gRPC", e.details() if hasattr(e, "details") else str(e))


def dashboard():
    limpar()
    titulo("Biblioteca - Cliente gRPC")

    texto = (
        "Este programa é o cliente.\n\n"
        "As operações são executadas pelo servidor Java através de gRPC.\n\n"
        "• Python/Tkinter: telas\n"
        "• Java: servidor\n"
        "• SQLite: banco no servidor\n"
        "• gRPC: comunicação entre cliente e servidor"
    )
    tk.Label(conteudo, text=texto, justify="left", font=("Arial", 14),
             bg=FUNDO).pack(anchor="w", padx=35)


def usuarios():
    limpar()
    titulo("Usuários")

    frame = tk.Frame(conteudo, bg=FUNDO)
    frame.pack(fill="x", padx=30)

    tk.Label(frame, text="Nome:", bg=FUNDO).grid(row=0, column=0)
    nome = tk.Entry(frame, width=28)
    nome.grid(row=0, column=1, padx=5)

    tk.Label(frame, text="Matrícula:", bg=FUNDO).grid(row=0, column=2)
    matricula = tk.Entry(frame, width=18)
    matricula.grid(row=0, column=3, padx=5)

    tabela = ttk.Treeview(conteudo, columns=("ID", "Nome", "Matrícula"),
                          show="headings")
    for c in ("ID", "Nome", "Matrícula"):
        tabela.heading(c, text=c)
    tabela.pack(fill="both", expand=True, padx=30, pady=25)

    def atualizar():
        tabela.delete(*tabela.get_children())
        try:
            resp = cliente.stub.ListarUsuarios(pb.Empty())
            for u in resp.usuarios:
                tabela.insert("", "end", values=(u.id, u.nome, u.matricula))
        except grpc.RpcError as e:
            erro_grpc(e)

    def cadastrar():
        try:
            resp = cliente.stub.CadastrarUsuario(
                pb.CadastrarUsuarioRequest(nome=nome.get(), matricula=matricula.get())
            )
            messagebox.showinfo("Sucesso", resp.mensagem)
            nome.delete(0, "end")
            matricula.delete(0, "end")
            atualizar()
        except grpc.RpcError as e:
            erro_grpc(e)

    tk.Button(frame, text="Cadastrar", bg=ROXO, fg="white",
              command=cadastrar).grid(row=0, column=4, padx=10)
    atualizar()


def livros():
    limpar()
    titulo("Livros")

    frame = tk.Frame(conteudo, bg=FUNDO)
    frame.pack(fill="x", padx=30)

    tk.Label(frame, text="ID do livro:", bg=FUNDO).grid(row=0, column=0)
    entrada_id = tk.Entry(frame, width=10)
    entrada_id.grid(row=0, column=1, padx=5)

    resultado = tk.Label(frame, text="", bg=FUNDO, justify="left",
                         font=("Arial", 11))
    resultado.grid(row=1, column=0, columnspan=4, sticky="w", pady=12)

    def consultar():
        try:
            r = cliente.stub.ConsultarLivro(
                pb.ConsultarLivroRequest(livro_id=int(entrada_id.get()))
            )
            status = "Disponível" if r.disponivel else "Emprestado"
            resultado.config(
                text=f"ID: {r.id}\nTítulo: {r.titulo}\nAutor: {r.autor}\n"
                     f"Gênero: {r.genero}\nStatus: {status}"
            )
        except (grpc.RpcError, ValueError) as e:
            erro_grpc(e)

    tk.Button(frame, text="Consultar no servidor", bg=ROXO, fg="white",
              command=consultar).grid(row=0, column=2, padx=10)

    tabela = ttk.Treeview(conteudo,
                          columns=("ID", "Título", "Autor", "Gênero", "Status"),
                          show="headings")
    for c in ("ID", "Título", "Autor", "Gênero", "Status"):
        tabela.heading(c, text=c)
    tabela.pack(fill="both", expand=True, padx=30, pady=20)

    try:
        resp = cliente.stub.ListarLivros(pb.Empty())
        for l in resp.livros:
            tabela.insert("", "end", values=(
                l.id, l.titulo, l.autor, l.genero,
                "Disponível" if l.disponivel else "Emprestado"
            ))
    except grpc.RpcError as e:
        erro_grpc(e)


def generos():
    limpar()
    titulo("Gêneros Literários")

    frame = tk.Frame(conteudo, bg=FUNDO)
    frame.pack(fill="x", padx=30)

    tk.Label(frame, text="Nome:", bg=FUNDO).grid(row=0, column=0)
    nome = tk.Entry(frame, width=30)
    nome.grid(row=0, column=1, padx=8)

    tabela = ttk.Treeview(conteudo, columns=("ID", "Gênero"), show="headings")
    tabela.heading("ID", text="ID")
    tabela.heading("Gênero", text="Gênero")
    tabela.pack(fill="both", expand=True, padx=30, pady=25)

    def atualizar():
        tabela.delete(*tabela.get_children())
        # O contrato atual não possui ListarGeneros; a tela mantém o cadastro
        # e o servidor valida os gêneros usados nos livros.
        tabela.insert("", "end", values=("-", "Cadastros são enviados ao servidor"))

    def cadastrar():
        try:
            r = cliente.stub.CadastrarGenero(
                pb.CadastrarGeneroRequest(nome=nome.get())
            )
            messagebox.showinfo("Gênero", r.mensagem)
            nome.delete(0, "end")
            atualizar()
        except grpc.RpcError as e:
            erro_grpc(e)

    tk.Button(frame, text="Cadastrar", bg=ROXO, fg="white",
              command=cadastrar).grid(row=0, column=2, padx=10)
    atualizar()


def cadastrar_livro():
    limpar()
    titulo("Cadastro de Livro")

    frame = tk.Frame(conteudo, bg=FUNDO)
    frame.pack(fill="x", padx=30)

    campos = {}
    for i, (rotulo, chave) in enumerate([
        ("Título:", "titulo"), ("Autor:", "autor"), ("ID do gênero:", "genero")
    ]):
        tk.Label(frame, text=rotulo, bg=FUNDO).grid(row=i, column=0, sticky="w", pady=6)
        campos[chave] = tk.Entry(frame, width=35)
        campos[chave].grid(row=i, column=1, padx=10, pady=6)

    resultado = tk.Label(frame, text="", bg=FUNDO, justify="left")
    resultado.grid(row=4, column=0, columnspan=3, sticky="w", pady=15)

    def cadastrar():
        try:
            r = cliente.stub.CadastrarLivro(
                pb.CadastrarLivroRequest(
                    titulo=campos["titulo"].get(),
                    autor=campos["autor"].get(),
                    genero_id=int(campos["genero"].get())
                )
            )
            resultado.config(text=f"Livro cadastrado: ID {r.id} - {r.titulo}")
            for e in campos.values():
                e.delete(0, "end")
        except (grpc.RpcError, ValueError) as e:
            erro_grpc(e)

    tk.Button(frame, text="Cadastrar livro", bg=ROXO, fg="white",
              command=cadastrar).grid(row=3, column=1, sticky="w", padx=10, pady=10)


def emprestimos():
    limpar()
    titulo("Empréstimos")

    frame = tk.Frame(conteudo, bg=FUNDO)
    frame.pack(fill="x", padx=30)

    tk.Label(frame, text="ID usuário:", bg=FUNDO).grid(row=0, column=0)
    usuario = tk.Entry(frame, width=10)
    usuario.grid(row=0, column=1, padx=5)

    tk.Label(frame, text="ID livro:", bg=FUNDO).grid(row=0, column=2)
    livro = tk.Entry(frame, width=10)
    livro.grid(row=0, column=3, padx=5)

    def realizar():
        try:
            r = cliente.stub.RealizarEmprestimo(
                pb.EmprestimoRequest(
                    usuario_id=int(usuario.get()),
                    livro_id=int(livro.get())
                )
            )
            (messagebox.showinfo if r.sucesso else messagebox.showwarning)(
                "Empréstimo", r.mensagem
            )
            atualizar()
        except (grpc.RpcError, ValueError) as e:
            erro_grpc(e)

    tk.Button(frame, text="Realizar empréstimo", bg=ROXO, fg="white",
              command=realizar).grid(row=0, column=4, padx=10)

    tabela = ttk.Treeview(conteudo,
                          columns=("ID", "Usuário", "Livro", "Empréstimo", "Prazo"),
                          show="headings")
    for c in ("ID", "Usuário", "Livro", "Empréstimo", "Prazo"):
        tabela.heading(c, text=c)
    tabela.pack(fill="both", expand=True, padx=30, pady=25)

    def atualizar():
        tabela.delete(*tabela.get_children())
        try:
            r = cliente.stub.ListarEmprestimosAtivos(pb.Empty())
            for e in r.emprestimos:
                tabela.insert("", "end", values=(
                    e.id, e.usuario, e.livro,
                    e.data_emprestimo, e.data_devolucao_prevista
                ))
        except grpc.RpcError as e:
            erro_grpc(e)

    atualizar()


def devolucoes():
    limpar()
    titulo("Devolução")

    tabela = ttk.Treeview(conteudo,
                          columns=("ID", "Usuário", "Livro", "Prazo"),
                          show="headings")
    for c in ("ID", "Usuário", "Livro", "Prazo"):
        tabela.heading(c, text=c)
    tabela.pack(fill="both", expand=True, padx=30, pady=20)

    def atualizar():
        tabela.delete(*tabela.get_children())
        try:
            r = cliente.stub.ListarEmprestimosAtivos(pb.Empty())
            for e in r.emprestimos:
                tabela.insert("", "end", values=(
                    e.id, e.usuario, e.livro, e.data_devolucao_prevista
                ))
        except grpc.RpcError as e:
            erro_grpc(e)

    def devolver():
        sel = tabela.selection()
        if not sel:
            messagebox.showwarning("Atenção", "Selecione um empréstimo.")
            return
        dados = tabela.item(sel[0])["values"]
        try:
            r = cliente.stub.DevolverLivro(
                pb.DevolucaoRequest(emprestimo_id=int(dados[0]))
            )
            (messagebox.showinfo if r.valor_multa == 0 else messagebox.showwarning)(
                "Devolução", r.mensagem
            )
            atualizar()
        except grpc.RpcError as e:
            erro_grpc(e)

    tk.Button(conteudo, text="Devolver livro", bg=ROXO, fg="white",
              command=devolver).pack(pady=8)
    atualizar()


def multas():
    limpar()
    titulo("Multas")

    tabela = ttk.Treeview(conteudo,
                          columns=("ID", "Usuário", "Valor", "Dias", "Status"),
                          show="headings")
    for c in ("ID", "Usuário", "Valor", "Dias", "Status"):
        tabela.heading(c, text=c)
    tabela.pack(fill="both", expand=True, padx=30, pady=20)

    def atualizar():
        tabela.delete(*tabela.get_children())
        try:
            r = cliente.stub.ListarMultas(pb.Empty())
            for m in r.multas:
                tabela.insert("", "end", values=(
                    m.id, m.usuario, f"R$ {m.valor:.2f}",
                    m.dias_atraso, "Paga" if m.paga else "Pendente"
                ))
        except grpc.RpcError as e:
            erro_grpc(e)

    def quitar():
        sel = tabela.selection()
        if not sel:
            messagebox.showwarning("Atenção", "Selecione uma multa.")
            return
        dados = tabela.item(sel[0])["values"]
        try:
            r = cliente.stub.QuitarMulta(
                pb.QuitarMultaRequest(multa_id=int(dados[0]))
            )
            messagebox.showinfo("Multa", r.mensagem)
            atualizar()
        except grpc.RpcError as e:
            erro_grpc(e)

    tk.Button(conteudo, text="Quitar multa", bg="#3C9D5F", fg="white",
              command=quitar).pack(pady=8)
    atualizar()


tk.Label(menu, text="📚 Biblioteca", font=("Arial", 20, "bold"),
         bg=ROXO_ESCURO, fg="white").pack(pady=30)


def botao(texto, comando):
    tk.Button(menu, text=texto, anchor="w", padx=20, pady=11,
              bg=ROXO_ESCURO, fg="white", relief="flat",
              activebackground=ROXO, activeforeground="white",
              command=comando).pack(fill="x", padx=10, pady=2)


botao("🏠  Início", dashboard)
botao("👥  Usuários", usuarios)
botao("🏷️  Gêneros", generos)
botao("➕  Cadastrar livro", cadastrar_livro)
botao("📚  Livros", livros)
botao("📖  Empréstimos", emprestimos)
botao("↩  Devoluções", devolucoes)
botao("💰  Multas", multas)
botao("🚪  Sair", janela.destroy)

dashboard()
janela.mainloop()
