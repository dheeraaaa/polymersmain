package com.vynedam.stockai.model;
import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document;
@Document("warehouses") public class Warehouse extends BaseDocument { @Id public String id; @Indexed(unique=true) public String code; public String name; public String unitId; public String type; public String address; public double capacity; public String capacityUnit; public boolean active=true; }
