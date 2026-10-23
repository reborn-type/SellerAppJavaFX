package com.example.sellercabinetapp.dto;

public record UserDto(
        Long userId,
        String firstName,
        String lastName,
        int age,
        String email,
        String role,
        String status
){}
