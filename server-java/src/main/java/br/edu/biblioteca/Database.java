package br.edu.biblioteca;

import java.sql.*;

public class Database {
    private static final String URL = "jdbc:sqlite:biblioteca_server.db";

    public static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    public static void init() throws SQLException {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    matricula TEXT UNIQUE NOT NULL
                )
            """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS generos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT UNIQUE NOT NULL
                )
            """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS livros (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    titulo TEXT NOT NULL,
                    autor TEXT NOT NULL,
                    genero_id INTEGER NOT NULL,
                    disponivel INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (genero_id) REFERENCES generos(id)
                )
            """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS emprestimos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER NOT NULL,
                    livro_id INTEGER NOT NULL,
                    data_emprestimo TEXT NOT NULL,
                    data_devolucao_prevista TEXT NOT NULL,
                    data_devolucao TEXT,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
                    FOREIGN KEY (livro_id) REFERENCES livros(id)
                )
            """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS multas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER NOT NULL,
                    emprestimo_id INTEGER NOT NULL,
                    valor REAL NOT NULL,
                    dias_atraso INTEGER NOT NULL,
                    paga INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
                    FOREIGN KEY (emprestimo_id) REFERENCES emprestimos(id)
                )
            """);
        }
    }
}
