package com.vynedam.stockai.dto;

import java.time.Instant;
import java.util.List;

import com.vynedam.stockai.domain.MovementType;
import com.vynedam.stockai.domain.QualityResult;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record MaterialRequest(@NotBlank
            @Size(max = 50) String sku, @NotBlank
            @Size(max = 160) String name, @NotBlank String category, @NotBlank
            @Size(max = 20) String unit, @PositiveOrZero Double reorderPoint, @PositiveOrZero Integer leadTimeDays) {

    }

    public record BatchRequest(@NotBlank String batchNumber, @NotBlank String materialId, String supplierLot, @NotBlank String warehouseId, @NotBlank String unitId, @Positive double quantity, @NotBlank String uom, Instant expiresAt, String barcode) {

    }

    public record MovementRequest(@NotBlank String batchId, @NotNull MovementType type, @Positive double quantity, String toWarehouseId, @Size(max = 100) String reference) {

    }

    public record TransferRequest(@NotBlank String batchId, @NotBlank String fromWarehouseId, @NotBlank String toWarehouseId, @Positive double quantity) {

    }

    public record QualityTest(@NotBlank String name, @NotBlank String value, String unit, boolean withinSpec) {

    }

    public record QualityRequest(@NotBlank String batchId, @NotBlank String stage, @NotNull QualityResult result, List<QualityTest> tests, String notes) {

    }
}
