package com.example.sellercabinetapp.dto;

public record ProductRequest(
        String name,
        Double price,
        Integer count,
        Integer countOfSales,
        Double averageEstimation
){}
