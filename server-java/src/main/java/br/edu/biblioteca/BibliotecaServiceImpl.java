package br.edu.biblioteca;

import br.edu.biblioteca.proto.*;
import io.grpc.stub.StreamObserver;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class BibliotecaServiceImpl extends BibliotecaServiceGrpc.BibliotecaServiceImplBase {

    private void fail(StreamObserver<?> obs, String message) {
        obs.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException());
    }

    @Override
    public void cadastrarUsuario(CadastrarUsuarioRequest req, StreamObserver<UsuarioResponse> obs) {
        if (req.getNome().isBlank() || req.getMatricula().isBlank()) {
            fail(obs, "Nome e matrícula são obrigatórios.");
            return;
        }

        String sql = "INSERT INTO usuarios(nome, matricula) VALUES(?, ?)";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, req.getNome());
            ps.setString(2, req.getMatricula());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                int id = rs.getInt(1);
                obs.onNext(UsuarioResponse.newBuilder()
                        .setSucesso(true).setMensagem("Usuário cadastrado.")
                        .setId(id).setNome(req.getNome()).setMatricula(req.getMatricula()).build());
            }
            obs.onCompleted();
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                fail(obs, "A matrícula já está cadastrada.");
            } else {
                fail(obs, e.getMessage());
            }
        }
    }

    @Override
    public void cadastrarGenero(CadastrarGeneroRequest req, StreamObserver<GeneroResponse> obs) {
        if (req.getNome().isBlank()) {
            fail(obs, "Nome do gênero é obrigatório.");
            return;
        }

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO generos(nome) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, req.getNome());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                obs.onNext(GeneroResponse.newBuilder()
                        .setSucesso(true).setMensagem("Gênero cadastrado.")
                        .setId(rs.getInt(1)).setNome(req.getNome()).build());
            }
            obs.onCompleted();
        } catch (SQLException e) {
            fail(obs, e.getMessage().contains("UNIQUE") ? "Esse gênero já existe." : e.getMessage());
        }
    }

    @Override
    public void cadastrarLivro(CadastrarLivroRequest req, StreamObserver<LivroResponse> obs) {
        try (Connection c = Database.connect()) {
            try (PreparedStatement check = c.prepareStatement("SELECT nome FROM generos WHERE id=?")) {
                check.setInt(1, req.getGeneroId());
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        fail(obs, "Gênero não encontrado.");
                        return;
                    }
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO livros(titulo, autor, genero_id) VALUES(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, req.getTitulo());
                ps.setString(2, req.getAutor());
                ps.setInt(3, req.getGeneroId());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    obs.onNext(LivroResponse.newBuilder()
                            .setSucesso(true).setMensagem("Livro cadastrado.")
                            .setId(rs.getInt(1)).setTitulo(req.getTitulo())
                            .setAutor(req.getAutor()).setDisponivel(true).build());
                }
            }
            obs.onCompleted();
        } catch (SQLException e) {
            fail(obs, e.getMessage());
        }
    }

    @Override
    public void listarUsuarios(Empty req, StreamObserver<ListaUsuariosResponse> obs) {
        ListaUsuariosResponse.Builder out = ListaUsuariosResponse.newBuilder();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT id,nome,matricula FROM usuarios ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.addUsuarios(UsuarioResponse.newBuilder()
                        .setSucesso(true).setId(rs.getInt("id"))
                        .setNome(rs.getString("nome"))
                        .setMatricula(rs.getString("matricula")).build());
            }
            obs.onNext(out.build());
            obs.onCompleted();
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }

    @Override
    public void listarLivros(Empty req, StreamObserver<ListaLivrosResponse> obs) {
        ListaLivrosResponse.Builder out = ListaLivrosResponse.newBuilder();
        String sql = """
            SELECT l.id,l.titulo,l.autor,g.nome,l.disponivel
            FROM livros l JOIN generos g ON g.id=l.genero_id
            ORDER BY l.titulo
        """;
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.addLivros(LivroResponse.newBuilder()
                        .setSucesso(true).setId(rs.getInt(1))
                        .setTitulo(rs.getString(2)).setAutor(rs.getString(3))
                        .setGenero(rs.getString(4)).setDisponivel(rs.getInt(5) == 1).build());
            }
            obs.onNext(out.build());
            obs.onCompleted();
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }

    @Override
    public void consultarLivro(ConsultarLivroRequest req, StreamObserver<LivroResponse> obs) {
        String sql = """
            SELECT l.id,l.titulo,l.autor,g.nome,l.disponivel
            FROM livros l JOIN generos g ON g.id=l.genero_id WHERE l.id=?
        """;
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, req.getLivroId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    fail(obs, "Livro não encontrado.");
                    return;
                }
                obs.onNext(LivroResponse.newBuilder()
                        .setSucesso(true).setId(rs.getInt(1))
                        .setTitulo(rs.getString(2)).setAutor(rs.getString(3))
                        .setGenero(rs.getString(4)).setDisponivel(rs.getInt(5) == 1).build());
                obs.onCompleted();
            }
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }

    private double multaPendente(Connection c, int usuarioId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COALESCE(SUM(valor),0) FROM multas WHERE usuario_id=? AND paga=0")) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    @Override
    public void consultarMultaUsuario(ConsultarMultaUsuarioRequest req,
                                      StreamObserver<MultaUsuarioResponse> obs) {
        try (Connection c = Database.connect()) {
            double total = multaPendente(c, req.getUsuarioId());
            obs.onNext(MultaUsuarioResponse.newBuilder()
                    .setSucesso(true)
                    .setMensagem(total > 0 ? "Usuário possui multa pendente." : "Sem multas pendentes.")
                    .setTotalPendente(total).build());
            obs.onCompleted();
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }

    @Override
    public void realizarEmprestimo(EmprestimoRequest req, StreamObserver<OperacaoResponse> obs) {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);

            double multa = multaPendente(c, req.getUsuarioId());
            if (multa > 0) {
                c.rollback();
                obs.onNext(OperacaoResponse.newBuilder().setSucesso(false)
                        .setMensagem(String.format("Empréstimo bloqueado. Multa pendente: R$ %.2f", multa)).build());
                obs.onCompleted();
                return;
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT disponivel FROM livros WHERE id=?")) {
                ps.setInt(1, req.getLivroId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        fail(obs, "Livro não encontrado.");
                        return;
                    }
                    if (rs.getInt(1) == 0) {
                        c.rollback();
                        obs.onNext(OperacaoResponse.newBuilder().setSucesso(false)
                                .setMensagem("Livro já está emprestado.").build());
                        obs.onCompleted();
                        return;
                    }
                }
            }

            LocalDate hoje = LocalDate.now();
            LocalDate prazo = hoje.plusDays(7);

            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO emprestimos(usuario_id,livro_id,data_emprestimo,data_devolucao_prevista)
                    VALUES(?,?,?,?)
                """)) {
                ps.setInt(1, req.getUsuarioId());
                ps.setInt(2, req.getLivroId());
                ps.setString(3, hoje.toString());
                ps.setString(4, prazo.toString());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE livros SET disponivel=0 WHERE id=?")) {
                ps.setInt(1, req.getLivroId());
                ps.executeUpdate();
            }

            c.commit();

            obs.onNext(OperacaoResponse.newBuilder().setSucesso(true)
                    .setMensagem("Empréstimo realizado. Devolução prevista: " + prazo).build());
            obs.onCompleted();
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }

    @Override
    public void listarEmprestimosAtivos(Empty req, StreamObserver<ListaEmprestimosResponse> obs) {
        ListaEmprestimosResponse.Builder out = ListaEmprestimosResponse.newBuilder();
        String sql = """
            SELECT e.id,u.nome,l.titulo,e.data_emprestimo,e.data_devolucao_prevista
            FROM emprestimos e
            JOIN usuarios u ON u.id=e.usuario_id
            JOIN livros l ON l.id=e.livro_id
            WHERE e.data_devolucao IS NULL
            ORDER BY e.id DESC
        """;
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.addEmprestimos(EmprestimoResponse.newBuilder()
                        .setId(rs.getInt(1)).setUsuario(rs.getString(2))
                        .setLivro(rs.getString(3)).setDataEmprestimo(rs.getString(4))
                        .setDataDevolucaoPrevista(rs.getString(5)).build());
            }
            obs.onNext(out.build());
            obs.onCompleted();
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }

    @Override
    public void devolverLivro(DevolucaoRequest req, StreamObserver<DevolucaoResponse> obs) {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);

            int usuarioId, livroId;
            String prevista;

            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT usuario_id,livro_id,data_devolucao_prevista
                    FROM emprestimos WHERE id=? AND data_devolucao IS NULL
                """)) {
                ps.setInt(1, req.getEmprestimoId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        fail(obs, "Empréstimo ativo não encontrado.");
                        return;
                    }
                    usuarioId = rs.getInt(1);
                    livroId = rs.getInt(2);
                    prevista = rs.getString(3);
                }
            }

            LocalDate hoje = LocalDate.now();
            LocalDate dataPrevista = LocalDate.parse(prevista);
            long atrasoLong = ChronoUnit.DAYS.between(dataPrevista, hoje);
            int atraso = (int)Math.max(0, atrasoLong);
            double valor = atraso;

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE emprestimos SET data_devolucao=? WHERE id=?")) {
                ps.setString(1, hoje.toString());
                ps.setInt(2, req.getEmprestimoId());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE livros SET disponivel=1 WHERE id=?")) {
                ps.setInt(1, livroId);
                ps.executeUpdate();
            }

            if (atraso > 0) {
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO multas(usuario_id,emprestimo_id,valor,dias_atraso)
                        VALUES(?,?,?,?)
                    """)) {
                    ps.setInt(1, usuarioId);
                    ps.setInt(2, req.getEmprestimoId());
                    ps.setDouble(3, valor);
                    ps.setInt(4, atraso);
                    ps.executeUpdate();
                }
            }

            c.commit();

            String mensagem = atraso > 0
                    ? String.format("Devolução realizada. %d dia(s) de atraso. Multa: R$ %.2f", atraso, valor)
                    : "Devolução realizada dentro do prazo.";

            obs.onNext(DevolucaoResponse.newBuilder().setSucesso(true)
                    .setMensagem(mensagem).setDiasAtraso(atraso)
                    .setValorMulta(valor).build());
            obs.onCompleted();
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }

    @Override
    public void listarMultas(Empty req, StreamObserver<ListaMultasResponse> obs) {
        ListaMultasResponse.Builder out = ListaMultasResponse.newBuilder();
        String sql = """
            SELECT m.id,u.nome,m.valor,m.dias_atraso,m.paga
            FROM multas m JOIN usuarios u ON u.id=m.usuario_id
            ORDER BY m.paga,m.id DESC
        """;
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.addMultas(MultaResponse.newBuilder()
                        .setId(rs.getInt(1)).setUsuario(rs.getString(2))
                        .setValor(rs.getDouble(3)).setDiasAtraso(rs.getInt(4))
                        .setPaga(rs.getInt(5) == 1).build());
            }
            obs.onNext(out.build());
            obs.onCompleted();
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }

    @Override
    public void quitarMulta(QuitarMultaRequest req, StreamObserver<OperacaoResponse> obs) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE multas SET paga=1 WHERE id=? AND paga=0")) {
            ps.setInt(1, req.getMultaId());
            int alteradas = ps.executeUpdate();

            if (alteradas == 0) {
                obs.onNext(OperacaoResponse.newBuilder().setSucesso(false)
                        .setMensagem("Multa não encontrada ou já quitada.").build());
            } else {
                obs.onNext(OperacaoResponse.newBuilder().setSucesso(true)
                        .setMensagem("Multa quitada com sucesso.").build());
            }
            obs.onCompleted();
        } catch (SQLException e) { fail(obs, e.getMessage()); }
    }
}
