package com.vynedam.stockai.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.vynedam.stockai.exception.ApiException;
import com.vynedam.stockai.model.Batch;
import com.vynedam.stockai.repository.BatchRepository;

@Service
public class BarcodeService {

    private final BatchRepository batches;

    public BarcodeService(BatchRepository b) {
        batches = b;
    }

    public Map<String, Object> label(String id) {
        var b = batches.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Batch not found"));
        if (b.barcode == null || b.barcode.isBlank()) {
            b.barcode = "BCH-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            batches.save(b);
        }
        return Map.of("format", "QR", "code", b.barcode, "payload", "stockai://batch/" + b.id, "batchId", b.id);
    }

    public Batch scan(String code) {
        return batches.findByBarcode(code).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Barcode not found"));
    }
}
