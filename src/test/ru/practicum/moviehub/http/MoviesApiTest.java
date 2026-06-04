package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.practicum.moviehub.http.BaseHttpHandler.CT_HEADER_NAME;
import static ru.practicum.moviehub.http.BaseHttpHandler.CT_JSON;

public class MoviesApiTest {
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static final String BASE = "http://localhost:8080";

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        //перед каждым тестом очищаем хранилище
        store.clearStorage();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    //getMovies должен вернуть пустое значение, если не добавлены фильмы
    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH);
        // создайте объект GET-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправьте запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Допишите проверку кода ответа
        assertEquals(BaseHttpHandler.STATUS_OK, resp.statusCode(), "GET /movies должен вернуть 200");

        // Допишите проверку заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // проверка, что был возвращён массив
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    //getMovies должен вернуть фильмы, если они есть
    @Test
    void getMovies_whenNotEmpty_returnsArray() throws Exception {
        // добавляем фильмы в хранилище
        store.addMovie("Матрица", 1999);
        store.addMovie("Интерстеллар", 2014);
        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH);
        // создайте объект GET-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправьте запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Допишите проверку кода ответа
        assertEquals(BaseHttpHandler.STATUS_OK, resp.statusCode(), "GET /movies должен вернуть 200");

        // Допишите проверку заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(2, movies.size(), "Ожидается 2 фильма");
        assertEquals("Матрица", movies.get(0).getTitle());
        assertEquals("Интерстеллар", movies.get(1).getTitle());
    }

    //getMovieById должен вернуть фильм по ID, если он есть
    @Test
    void getMovieById_whenExists_returnsMovieJson() throws Exception {
        // добавляем фильм в хранилище
        Movie movie = store.addMovie("Матрица", 1999);

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH + "/" + movie.getId());

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправить запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(BaseHttpHandler.STATUS_OK, resp.statusCode(), "GET /movies/{id} должен вернуть 200");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        Movie responseMovie = gson.fromJson(resp.body(), Movie.class);

        assertEquals(movie.getId(), responseMovie.getId());
        assertEquals(movie.getTitle(), responseMovie.getTitle());
        assertEquals(movie.getYear(), responseMovie.getYear());
    }

    //getMovieById должен вернуть 404, если такого ID нет
    @Test
    void getMovieById_whenNotExists_returnsError() throws Exception {

        int testId = 22;

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH + "/" + testId);

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправить запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(BaseHttpHandler.STATUS_NOT_FOUND, resp.statusCode(), "GET /movies/{id} должен вернуть 404");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        JsonObject error = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Фильм не найден", error.get("error").getAsString());
    }

    //getMovieById должен вернуть 400, если ID не цифра
    @Test
    void getMovieById_whenNotIntChar_returnsError() throws Exception {


        String testId = "fff";

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH + "/" + testId);

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправить запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(BaseHttpHandler.STATUS_BAD_REQUEST, resp.statusCode(), "GET /movies/{id} должен вернуть 400");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        JsonObject error = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Некорректный ID", error.get("error").getAsString());
    }

    //deleteMovieById должен вернуть 204, если фильм удален
    @Test
    void deleteMovieById_whenDeleted_returnsStatusNoContent() throws Exception {
        // добавляем фильм в хранилище
        Movie movie = store.addMovie("Матрица", 1999);
        int testId = movie.getId();

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH + "/" + testId);

        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправить запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(BaseHttpHandler.STATUS_NO_CONTENT, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");
    }

    //deleteMovieById должен вернуть 400, если id не найден
    @Test
    void deleteMovieById_whenNotExist_returnsStatusNotFound() throws Exception {

        int testId = 22;

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH + "/" + testId);

        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправить запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(BaseHttpHandler.STATUS_NOT_FOUND, resp.statusCode(), "DELETE /movies/{id} должен вернуть 404");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        JsonObject error = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Фильм не найден", error.get("error").getAsString());
    }

    //getMoviesByYear должен вернуть фильмы, если они есть
    @Test
    void getMoviesByYear_whenNotEmpty_returnsArray() throws Exception {
        int year = 1999;
        // добавляем фильмы в хранилище
        store.addMovie("Матрица", year);
        store.addMovie("Фильм", year);


        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH + "?year=" + year);

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправьте запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Допишите проверку кода ответа
        assertEquals(BaseHttpHandler.STATUS_OK, resp.statusCode(), "GET /movies?year= должен вернуть 200");

        // Допишите проверку заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(2, movies.size(), "Ожидается 2 фильма");
        assertEquals("Матрица", movies.get(0).getTitle());
        assertEquals("Фильм", movies.get(1).getTitle());
    }

    //getMoviesByYear должен вернуть 400 если запрос некорректный
    @Test
    void getMoviesByYear_whenNotIntChar_returnsBadRequest() throws Exception {


        String year = "fff";

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH + "?year=" + year);

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        // Отправить запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(BaseHttpHandler.STATUS_BAD_REQUEST, resp.statusCode(), "GET /movies?year= должен вернуть 400");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");

        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        JsonObject error = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Некорректный параметр запроса — 'year'", error.get("error").getAsString());
    }

    //postMovie должен вернуть 201 при успешном добавлении
    @Test
    void postMovie_whenValid_returnsCreatedMovieJson() throws Exception {
        String requestBody = "{\"title\":\"Матрица\",\"year\":1999}";

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.STATUS_CREATED, resp.statusCode(),
                "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        Movie movie = gson.fromJson(resp.body(), Movie.class);

        assertEquals(1, movie.getId());
        assertEquals("Матрица", movie.getTitle());
        assertEquals(1999, movie.getYear());
    }

    //postMovie должен возвращать ошибку, если данные невалидны
    @Test
    void postMovie_whenValidationError_returns422() throws Exception {
        int maxYear = Year.now().getValue() + 1;

        String requestBody = "{\"title\":\" \",\"year\":1887}";

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header(CT_HEADER_NAME, CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.STATUS_UNPROCESSABLE_ENTITY, resp.statusCode(),
                "POST /movies с ошибкой валидации должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        JsonObject error = gson.fromJson(resp.body(), JsonObject.class);

        assertEquals("Ошибка валидации", error.get("error").getAsString());

        JsonArray details = error.getAsJsonArray("details");
        assertEquals(2, details.size());
        assertTrue(details.toString().contains("название не должно быть пустым"));
        assertTrue(details.toString().contains("год должен быть между 1888 и " + maxYear));
    }

    //postMovie должен возвращать ошибку, неправильный заголовок
    @Test
    void postMovie_whenWrongContentType_returns415() throws Exception {
        String requestBody = "{\"title\":\"Матрица\",\"year\":1999}";
        String wrongCT = "text/plain";

        URI uri = URI.create(BASE + MoviesHandler.BASE_URL_PATH);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header(CT_HEADER_NAME, wrongCT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.STATUS_UNSUPPORTED_MEDIA_TYPE, resp.statusCode(),
                "POST /movies с неверным Content-Type должен вернуть 415");

        String contentTypeHeaderValue =
                resp.headers().firstValue(CT_HEADER_NAME).orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        JsonObject error = gson.fromJson(resp.body(), JsonObject.class);
        assertEquals("Неподдерживаемый тип данных", error.get("error").getAsString());
    }

}