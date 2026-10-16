package com.example.sellercabinetapp.dto;

public record SellerRequest(
        String firstName,
        String lastName,
        Integer age,
        String email
){}
