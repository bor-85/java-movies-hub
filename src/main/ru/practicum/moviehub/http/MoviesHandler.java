package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;

public class MoviesHandler extends BaseHttpHandler {
    protected static final String BASE_URL_PATH = "/movies";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String QUERY_YEAR = "year=";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_YEAR = "year";
    private static final int EARLIEST_MOVIE_YEAR = 1888;
    private static final int MAX_LENGTH_TITLE = 100;
    private static final String ERROR_UNSUPPORTED_MEDIA_TYPE = "Неподдерживаемый тип данных";
    private static final String ERROR_INVALID_JSON = "неверный JSON";
    private static final String ERROR_TITLE_EMPTY = "название не должно быть пустым";
    private static final String ERROR_TITLE_TOO_LONG = "название не должно быть длиннее 100 символов";
    private static final String ERROR_MOVIE_NOT_FOUND = "Фильм не найден";
    private static final String ERROR_INVALID_ID = "Некорректный ID";
    private static final String ERROR_INVALID_YEAR_QUERY = "Некорректный параметр запроса — 'year'";
    private static final String ERROR_METHOD_NOT_ALLOWED = "Метод не поддерживается";
    private static final String ERROR_NO_SUCH_ENDPOINT = "Такого эндпоинта не существует";

    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getQuery();

        Endpoint endpoint = getEndpoint(path, method, query);

        //в зависимости от эндпоинта выбираем метод
        switch (endpoint) {
            case GET_MOVIES: {
                handleGetMovies(exchange);
                break;
            }
            case GET_MOVIES_BY_YEAR: {
                handleGetMoviesByYear(exchange);
                break;
            }
            case GET_MOVIE_BY_ID: {
                handleGetMovieById(exchange);
                break;
            }
            case POST_MOVIES: {
                handlePostMovie(exchange);
                break;
            }
            case DELETE_MOVIE_BY_ID: {
                handleDeleteMovieById(exchange);
                break;
            }
            default:
                if (path.equals(BASE_URL_PATH)
                        || path.matches(BASE_URL_PATH + "/[^/]+")) {
                    sendError(exchange, STATUS_METHOD_NOT_ALLOWED, ERROR_METHOD_NOT_ALLOWED);
                } else {
                    sendError(exchange, STATUS_NOT_FOUND, ERROR_NO_SUCH_ENDPOINT);
                }
                break;
        }
    }

    //получаем эндпоинт
    private Endpoint getEndpoint(String requestPath, String requestMethod, String requestParams) {

        if (METHOD_GET.equalsIgnoreCase(requestMethod) && BASE_URL_PATH.equals(requestPath) && requestParams == null) {
            return Endpoint.GET_MOVIES;
        } else if (METHOD_GET.equalsIgnoreCase(requestMethod) && BASE_URL_PATH.equals(requestPath) && requestParams != null && requestParams.startsWith(QUERY_YEAR)) {
            return Endpoint.GET_MOVIES_BY_YEAR;
        } else if (METHOD_GET.equalsIgnoreCase(requestMethod) && requestPath.matches(BASE_URL_PATH + "/[^/]+")) {
            return Endpoint.GET_MOVIE_BY_ID;
        } else if (METHOD_POST.equalsIgnoreCase(requestMethod) && BASE_URL_PATH.equals(requestPath)) {
            return Endpoint.POST_MOVIES;
        } else if (METHOD_DELETE.equalsIgnoreCase(requestMethod) && requestPath.matches(BASE_URL_PATH + "/[^/]+")) {
            return Endpoint.DELETE_MOVIE_BY_ID;
        }

        return Endpoint.UNKNOWN;
    }

    //вызов GET без параметров, возвращает все фильмы
    private void handleGetMovies(HttpExchange exchange) throws IOException {
        Collection<Movie> movies = moviesStore.getMovies();
        String response = new Gson().toJson(movies);

        sendJson(exchange, STATUS_OK, response);
    }

    //вызов GET по году
    private void handleGetMoviesByYear(HttpExchange exchange) throws IOException {
        try {
            String yearParam = exchange.getRequestURI().getQuery().substring(QUERY_YEAR.length());
            int year = Integer.parseInt(yearParam);

            Collection<Movie> movies = moviesStore.getMoviesByYear(year);

            String response = new Gson().toJson(movies);
            sendJson(exchange, STATUS_OK, response);
        } catch (NumberFormatException | NullPointerException e) {
            sendError(exchange, STATUS_BAD_REQUEST, ERROR_INVALID_YEAR_QUERY);
        }
    }

    //вызов GET по id
    private void handleGetMovieById(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String idParam = path.substring(BASE_URL_PATH.length() + 1);

        try {
            int id = Integer.parseInt(idParam);

            Movie movie = moviesStore.getMovieById(id);

            if (movie == null) {
                sendError(exchange, STATUS_NOT_FOUND, ERROR_MOVIE_NOT_FOUND);
                return;
            }

            String response = new Gson().toJson(movie);
            sendJson(exchange, STATUS_OK, response);

        } catch (NumberFormatException e) {
            sendError(exchange, STATUS_BAD_REQUEST, ERROR_INVALID_ID);
        }
    }

    //вызов POST
    private void handlePostMovie(HttpExchange exchange) throws IOException {
        try {
            String contentType = exchange.getRequestHeaders().getFirst(CT_HEADER_NAME);
            if (contentType == null || !contentType.startsWith(CT_HEADER_VALUE)) {
                sendError(exchange, STATUS_UNSUPPORTED_MEDIA_TYPE, ERROR_UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            //вынос в отдельный метод получение тела
            String body = readRequestBody(exchange);

            //вынос в отдельный метод получение Json
            JsonObject jsonObject = parseJsonObject(body);

            List<String> details = new ArrayList<>();

            //вынос в отдельный метод получение title
            String title = extractTitle(jsonObject);

            if (title.isBlank()) {
                details.add(ERROR_TITLE_EMPTY);
            } else if (title.length() > MAX_LENGTH_TITLE) {
                details.add(ERROR_TITLE_TOO_LONG);
            }

            int maxYear = Year.now().getValue() + 1;

            //вынос в отедльный метод получение года
            Integer year = extractYear(jsonObject);

            if (year == null || year < EARLIEST_MOVIE_YEAR || year > maxYear) {
                details.add("год должен быть между " + EARLIEST_MOVIE_YEAR + " и " + maxYear);
            }

            if (!details.isEmpty()) {
                sendValidationError(exchange, details);
                return;
            }

            Movie createdMovie = moviesStore.addMovie(title, year);
            sendJson(exchange, STATUS_CREATED, new Gson().toJson(createdMovie));

        } catch (IOException | IllegalStateException | NumberFormatException e) {
            sendValidationError(exchange, List.of(ERROR_INVALID_JSON));
        }
    }

    private void handleDeleteMovieById(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String idParam = path.substring(BASE_URL_PATH.length() + 1);

        try {
            int id = Integer.parseInt(idParam);

            boolean deleted = moviesStore.deleteMovieById(id);
            if (!deleted) {
                sendError(exchange, STATUS_NOT_FOUND, ERROR_MOVIE_NOT_FOUND);
                return;
            }

            sendNoContent(exchange);

        } catch (NumberFormatException e) {
            sendError(exchange, STATUS_BAD_REQUEST, ERROR_INVALID_ID);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), DEFAULT_CHARSET);
        }
    }

    private JsonObject parseJsonObject(String body) throws IOException {
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException(ERROR_INVALID_JSON, e);
        }
    }

    private String extractTitle(JsonObject jsonObject) {
        if (jsonObject.has(FIELD_TITLE) && !jsonObject.get(FIELD_TITLE).isJsonNull()) {
            return jsonObject.get(FIELD_TITLE).getAsString().trim();
        }
        return "";
    }

    private Integer extractYear(JsonObject jsonObject) {
        if (jsonObject.has(FIELD_YEAR) && !jsonObject.get(FIELD_YEAR).isJsonNull()) {
            try {
                return jsonObject.get(FIELD_YEAR).getAsInt();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    enum Endpoint {
        GET_MOVIES,
        POST_MOVIES,
        GET_MOVIE_BY_ID,
        DELETE_MOVIE_BY_ID,
        GET_MOVIES_BY_YEAR,
        UNKNOWN
    }
}
