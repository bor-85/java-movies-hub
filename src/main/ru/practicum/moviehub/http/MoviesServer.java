package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private static final int PORT = 8080;
    private static final int DEFAULT_DELAY = 0;
    private static final int DEFAULT_BACKLOG = 0;
    private final HttpServer server;

    public MoviesServer(MoviesStore moviesStore) {

        try {
            server = HttpServer.create(new InetSocketAddress(PORT), DEFAULT_BACKLOG);
            server.createContext(MoviesHandler.BASE_URL_PATH, new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        // запуск сервера
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        // остановка сервера
        server.stop(DEFAULT_DELAY);
        System.out.println("Сервер остановлен");
    }
}
