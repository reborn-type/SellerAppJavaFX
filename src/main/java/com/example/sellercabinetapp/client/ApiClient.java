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

    private String jwtToken;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private HttpRequest.Builder getBuilder(String path){
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json");
        if (jwtToken != null && !jwtToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }
        return builder;
    }

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String baseUrl;

    public ApiClient(String baseUrl) {this.baseUrl = baseUrl;};

    public void setJwtToken(String token){
        this.jwtToken = token;
    }

    public String getJwtToken(){
        return this.jwtToken;
    }

    public <T> T get(String path, Class<T> responseType) throws Exception {
        return send(getBuilder(path).GET().build(), responseType);
    }

    public <T> T get(String path, TypeReference<T> typeReference)
    throws Exception
    {
        HttpResponse<String> response = http.send(
                getBuilder(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        return mapper.readValue(response.body(), typeReference);
    }

    public <T> T post(String path, Object body, Class<T> type)
    throws Exception
    {
        String json = mapper.writeValueAsString(body);
        return send(getBuilder(path).POST(HttpRequest.BodyPublishers.ofString(json)).build(), type);
    }

    public void put(String path, Object body)
    throws Exception
    {
        String json = mapper.writeValueAsString(body);
        http.send(getBuilder(path).PUT(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    public void delete(String path)
    throws Exception
    {
        http.send(getBuilder(path).DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private <T> T send(HttpRequest request, Class<T> responseType) throws Exception{
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        return mapper.readValue(response.body(), responseType);
    }

}
