package com.example.sellercabinetapp.client;

import com.example.sellercabinetapp.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public class SellerApi {

    private final ApiClient api;

    public SellerApi(ApiClient api) {
        this.api = api;
    }

    public TokenPairDto login(String email, String password) throws Exception {
        var req = new LoginRequestDto(email, password);
        return api.post("api/auth/login", req, TokenPairDto.class);
    }

    public List<UserDto> getAllUsers() throws Exception {
        return api.get("api/users", new TypeReference<List<UserDto>>(){});
    }

    public UserDto getUser(Long userId) throws Exception {
        return api.get("api/users/" + userId, UserDto.class);
    }

    public void deleteUser(Long userId) throws Exception {
        api.delete("api/users/" + userId);
    }

    public List<ProductDto> getAllProducts() throws Exception {
        return api.get("api/products", new TypeReference<List<ProductDto>>() {});
    }

    public List<ProductDto> getProductsBySeller(Long sellerId) throws Exception {
        return api.get("api/products/by-seller/" + sellerId, new TypeReference<List<ProductDto>>(){});
    }

    public ProductDto getProduct(Long productId) throws Exception {
        return api.get("api/products/" + productId, ProductDto.class);
    }

    public ProductDto createProduct(ProductRequest req) throws Exception {
        return api.post("api/products", req, ProductDto.class);
    }

    public void updateProduct(Long productId, ProductRequest req) throws Exception {
        api.put("api/products/" + productId, req);
    }

    public void deleteProduct(Long productId) throws Exception {
        api.delete("api/products/" + productId);
    }

    public List<SellerStatsDto> getAllSellerStats() throws Exception {
        return api.get("api/analytics/sellers", new TypeReference<List<SellerStatsDto>>() {});
    }

    public SellerStatsDto getSellerStats(Long sellerId) throws Exception {
        return api.get("api/analytics/sellers/" + sellerId, SellerStatsDto.class);
    }

    public void register(RegisterRequestDto req) throws Exception {
        api.post("api/auth/register", req, Object.class);
    }
}