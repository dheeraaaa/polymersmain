package com.vynedam.stockai.model;
import java.time.Instant; import org.springframework.data.annotation.*;
public abstract class BaseDocument { @CreatedDate public Instant createdAt; @LastModifiedDate public Instant updatedAt; @Version public Long version; }
