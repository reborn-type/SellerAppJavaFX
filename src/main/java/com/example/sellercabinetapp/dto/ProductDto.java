package com.example.sellercabinetapp.dto;

public record ProductDto(
        Long productId,
        Long sellerId,
        String name,
        Double price,
        Integer count,
        Integer countOfSales,
        Double averageEstimation
){}

