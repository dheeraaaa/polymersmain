package com.vynedam.stockai.model;
import com.vynedam.stockai.domain.MovementType; import java.time.Instant; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document;
@Document("stock_movements") public class StockMovement extends BaseDocument { @Id public String id; public String batchId; public MovementType type; public double quantity; public String fromWarehouseId; public String toWarehouseId; public String reference; public String actorId; public Instant occurredAt; }
