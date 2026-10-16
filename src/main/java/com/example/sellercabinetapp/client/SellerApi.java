package com.example.sellercabinetapp.client;
import com.example.sellercabinetapp.client.ApiClient;
import com.example.sellercabinetapp.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;


public class SellerApi {

    private final ApiClient api;

    public SellerApi(ApiClient api) {this.api = api;};

    public List<SellerDto> getAllSellers() throws Exception {
        return api.get("api/sellers", new TypeReference<List<SellerDto>>(){});
    }

    public SellerDto getSeller(Long sellerId) throws Exception {
        return api.get("api/sellers/" + sellerId, SellerDto.class);
    }

    public SellerDto createSeller(SellerRequest req) throws Exception {
        return api.post("api/sellers", req, SellerDto.class);
    }

    public void updateSeller(Long sellerId, SellerRequest req) throws Exception {
        api.put("api/sellers/" + sellerId, req);
    }

    public void deleteSeller(Long sellerId) throws Exception {
        api.delete("api/sellers/" + sellerId);
    }

    public List<ProductDto> getAllProducts(Long sellerId) throws Exception {
        return api.get("api/sellers/" + sellerId + "/products", new TypeReference<List<ProductDto>>() {});
    }

    public ProductDto getProduct(Long productId, Long sellerId) throws Exception {
        return api.get("api/sellers/" + sellerId + "/product/" + productId, ProductDto.class);
    }

    public ProductDto createProduct(Long sellerId, ProductRequest req) throws Exception {
        return api.post("api/sellers/" + sellerId + "/products", req, ProductDto.class);
    }

    public void updateProduct(Long sellerId, Long productId, ProductRequest req) throws Exception {
        api.put("api/sellers/" + sellerId + "/product/" + productId, req);
    }

    public void deleteProduct(Long sellerId, Long productId) throws Exception {
        api.delete("api/sellers/" + sellerId + "/product/" + productId);
    }

}
