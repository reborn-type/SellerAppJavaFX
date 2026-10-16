package com.example.sellercabinetapp.dto;


public record SellerDto(
        Long sellerId,
        String firstName,
        String lastName,
        Integer age,
        String email
){}


