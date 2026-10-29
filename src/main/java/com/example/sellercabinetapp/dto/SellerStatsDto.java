package com.example.sellercabinetapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerStatsDto(
        Long sellerId,
        Integer totalProducts,
        Long totalSales,
        BigDecimal totalStockValue,
        BigDecimal averageEstimation,
        LocalDateTime updatedAt
) {}