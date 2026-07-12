package com.youssefhenna.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.youssefhenna.status.DbManagerStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DbManagerStatusReader {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .build();

    // map snake_case JSON to camelCase.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public static DbManagerStatus fetchStatus(String host, int port) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://" + host + ":" + port + "/status"))
            .timeout(TIMEOUT)
            .GET()
            .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected status code " + response.statusCode() + " from db manager status endpoint");
        }
        return OBJECT_MAPPER.readValue(response.body(), DbManagerStatus.class);
    }
}
