package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final String CT_HEADER_NAME = "Content-Type";
    protected static final String CT_HEADER_VALUE = "application/json";
    protected static final int STATUS_OK = 200;
    protected static final int STATUS_CREATED = 201;
    protected static final int STATUS_NO_CONTENT = 204;
    protected static final int STATUS_BAD_REQUEST = 400;
    protected static final int STATUS_NOT_FOUND = 404;
    protected static final int STATUS_UNSUPPORTED_MEDIA_TYPE = 415;
    protected static final int STATUS_UNPROCESSABLE_ENTITY = 422;
    protected static final int STATUS_METHOD_NOT_ALLOWED = 405;
    protected static final String METHOD_GET = "GET";
    protected static final String METHOD_POST = "POST";
    protected static final String METHOD_DELETE = "DELETE";
    protected static final String METHOD_PUT =  "PUT";
    private static final Gson GSON = new Gson();

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set(CT_HEADER_NAME, CT_JSON);
        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set(CT_HEADER_NAME, CT_JSON);
        ex.sendResponseHeaders(STATUS_NO_CONTENT, -1);
        ex.close();
    }

    protected void sendError(HttpExchange ex, int status, String message) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(message);
        sendJson(ex, status, GSON.toJson(errorResponse));
    }

    protected void sendValidationError(HttpExchange ex, List<String> details) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", details);
        sendJson(ex, STATUS_UNPROCESSABLE_ENTITY, GSON.toJson(errorResponse));
    }
}