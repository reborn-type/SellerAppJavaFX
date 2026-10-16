package com.example.sellercabinetapp.client;
import com.fasterxml.jackson.core.type.TypeReference;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import java.time.Duration;

public class ApiClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String baseUrl;

    public ApiClient(String baseUrl) {this.baseUrl = baseUrl;};

    private void ensureSuccess(HttpResponse<?> res) throws IOException{
        if (res.statusCode() >= 300){
            throw new IOException("HTTP " + res.statusCode() + ": " + res.body());
        }
    }

    public <T> T get(String path, Class<T> type)
    throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(res);
        return mapper.readValue(res.body(), type);
    }

    public <T> T get(String path, TypeReference<T> type)
    throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(res);
        return mapper.readValue(res.body(), type);
    }

    public <T> T post(String path, Object body, Class<T> type)
    throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(res);
        return res.body().isBlank() ? null : mapper.readValue(res.body(), type);
    }

    public void put(String path, Object body)
    throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        ensureSuccess(http.send(req, HttpResponse.BodyHandlers.ofString()));
    }

    public void delete(String path)
    throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        ensureSuccess(http.send(req, HttpResponse.BodyHandlers.ofString()));
    }



}
