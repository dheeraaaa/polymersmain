package com.vynedam.stockai.dto;
import jakarta.validation.constraints.*; import java.util.Set;
public final class WarehouseDtos { private WarehouseDtos(){} public record WarehouseRequest(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=50) String unitId,@NotBlank @Size(max=40) String type,@Size(max=400) String address,@PositiveOrZero Double capacity,@Size(max=20) String capacityUnit){} public record StaffAssignmentRequest(@NotNull Set<@NotBlank String> userIds){} }
