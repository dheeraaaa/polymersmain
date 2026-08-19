package com.vynedam.stockai.model;
import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document;
@Document("materials") public class Material extends BaseDocument { @Id public String id; @Indexed(unique=true) public String sku; public String name; public String category; public String unit; public double reorderPoint; public int leadTimeDays; public boolean active=true; }
