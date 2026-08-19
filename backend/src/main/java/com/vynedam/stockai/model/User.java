package com.vynedam.stockai.model;
import com.vynedam.stockai.domain.Role; import java.util.HashSet; import java.util.Set; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document;
@Document("users") public class User extends BaseDocument { @Id public String id; public String name; @Indexed(unique=true) public String email; public String passwordHash; public Role role; public String unitId; public Set<String> warehouseIds=new HashSet<>(); public boolean active=true; }
