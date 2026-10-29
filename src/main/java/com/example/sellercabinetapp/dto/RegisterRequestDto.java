package com.example.sellercabinetapp.dto;

public record RegisterRequestDto(
        String firstName,
        String lastName,
        int age,
        String email,
        String password
) {}