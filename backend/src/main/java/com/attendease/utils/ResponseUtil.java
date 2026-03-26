package com.attendease.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static void sendResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = GSON.toJson(data);
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        sendResponse(exchange, statusCode, error);
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
    }

    public static Gson getGson() {
        return GSON;
    }
}
