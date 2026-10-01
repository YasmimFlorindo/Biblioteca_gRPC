package br.edu.biblioteca;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class BibliotecaServer {
    public static void main(String[] args) throws Exception {
        Database.init();

        Server server = ServerBuilder.forPort(50051)
                .addService(new BibliotecaServiceImpl())
                .build()
                .start();

        System.out.println("======================================");
        System.out.println(" SERVIDOR gRPC DA BIBLIOTECA");
        System.out.println(" Porta: 50051");
        System.out.println(" Banco: biblioteca_server.db");
        System.out.println("======================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando servidor...");
            server.shutdown();
        }));

        server.awaitTermination();
    }
}
