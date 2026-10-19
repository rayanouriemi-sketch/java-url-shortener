package fr.rayan.shortener;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        ShortenerServer server = new ShortenerServer(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("Serveur démarré : http://localhost:" + server.port());
    }
}
