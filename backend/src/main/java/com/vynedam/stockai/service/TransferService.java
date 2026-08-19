package com.vynedam.stockai.service;

import com.vynedam.stockai.domain.*;
import com.vynedam.stockai.dto.InventoryDtos.TransferRequest;
import com.vynedam.stockai.exception.ApiException;
import com.vynedam.stockai.model.*;
import com.vynedam.stockai.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private final StockTransferRepository transfers;
    private final BatchRepository batches;
    private final StockMovementRepository movements;
    private final WarehouseService warehouses;

    public TransferService(StockTransferRepository transfers, BatchRepository batches, StockMovementRepository movements, WarehouseService warehouses) {
        this.transfers = transfers; this.batches = batches; this.movements = movements; this.warehouses = warehouses;
    }
    public List<StockTransfer> list() { return transfers.findAll(); }
    public StockTransfer create(TransferRequest request, String actor) {
        if (request.fromWarehouseId().equals(request.toWarehouseId())) throw new ApiException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Source and destination must differ");
        var batch = getBatch(request.batchId());
        if (!batch.warehouseId.equals(request.fromWarehouseId())) throw new ApiException(HttpStatus.CONFLICT,"WAREHOUSE_MISMATCH","Batch is not held by the requested source warehouse");
        if (batch.status != BatchStatus.AVAILABLE || batch.quantity < request.quantity()) throw new ApiException(HttpStatus.CONFLICT,"INSUFFICIENT_STOCK","Available quantity is insufficient for transfer");
        warehouses.assertOperationalAccess(request.fromWarehouseId(), actor); warehouses.get(request.toWarehouseId());
        var transfer = new StockTransfer(); transfer.transferNumber = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); transfer.batchId = batch.id;
        transfer.fromWarehouseId = request.fromWarehouseId(); transfer.toWarehouseId = request.toWarehouseId(); transfer.quantity = request.quantity(); transfer.status = TransferStatus.PENDING; transfer.requestedBy = actor;
        return transfers.save(transfer);
    }
    public StockTransfer approve(String id, String actor) { var transfer = pending(id); warehouses.assertOperationalAccess(transfer.fromWarehouseId, actor); transfer.status = TransferStatus.APPROVED; transfer.approvedBy = actor; return transfers.save(transfer); }
    public StockTransfer receive(String id, String actor) {
        var transfer = transfers.findByIdAndStatus(id, TransferStatus.APPROVED).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","Approved transfer not found"));
        warehouses.assertOperationalAccess(transfer.toWarehouseId, actor); var source = getBatch(transfer.batchId);
        if (!source.warehouseId.equals(transfer.fromWarehouseId) || source.status != BatchStatus.AVAILABLE || source.quantity < transfer.quantity) throw new ApiException(HttpStatus.CONFLICT,"INSUFFICIENT_STOCK","Source batch is unavailable or insufficient");
        Batch destination;
        if (Double.compare(source.quantity, transfer.quantity) == 0) { source.warehouseId = transfer.toWarehouseId; destination = batches.save(source); }
        else { source.quantity -= transfer.quantity; batches.save(source); destination = copyToDestination(source, transfer); destination = batches.save(destination); }
        transfer.destinationBatchId = destination.id; transfer.status = TransferStatus.RECEIVED; transfers.save(transfer);
        movement(source.id, MovementType.TRANSFER_OUT, transfer.quantity, transfer.fromWarehouseId, transfer.toWarehouseId, transfer.transferNumber, actor);
        movement(destination.id, MovementType.TRANSFER_IN, transfer.quantity, transfer.fromWarehouseId, transfer.toWarehouseId, transfer.transferNumber, actor);
        return transfer;
    }
    private Batch copyToDestination(Batch source, StockTransfer transfer) { var b = new Batch(); b.batchNumber = source.batchNumber + "-T" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(); b.materialId = source.materialId; b.supplierLot = source.supplierLot; b.warehouseId = transfer.toWarehouseId; b.unitId = source.unitId; b.quantity = transfer.quantity; b.uom = source.uom; b.receivedAt = Instant.now(); b.expiresAt = source.expiresAt; b.status = BatchStatus.AVAILABLE; return b; }
    private Batch getBatch(String id) { return batches.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","Batch not found")); }
    private StockTransfer pending(String id) { return transfers.findByIdAndStatus(id, TransferStatus.PENDING).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","Pending transfer not found")); }
    private void movement(String batchId, MovementType type, double quantity, String from, String to, String reference, String actor) { var movement = new StockMovement(); movement.batchId = batchId; movement.type = type; movement.quantity = quantity; movement.fromWarehouseId = from; movement.toWarehouseId = to; movement.reference = reference; movement.actorId = actor; movement.occurredAt = Instant.now(); movements.save(movement); }
}
